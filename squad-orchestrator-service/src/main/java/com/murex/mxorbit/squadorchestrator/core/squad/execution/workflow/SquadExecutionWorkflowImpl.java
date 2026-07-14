package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadStepExecutionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.Promise;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WorkflowImpl(taskQueues = "squad-orchestration-task-queue")
public class SquadExecutionWorkflowImpl implements SquadExecutionWorkflow {

	private final GetSquadActivity getSquadActivity = Workflow.newActivityStub(GetSquadActivity.class,
			buildActivityOptions());

	private final RunAiAgentActivity runAiAgentActivity = Workflow.newActivityStub(RunAiAgentActivity.class,
			buildActivityOptions());

	private final SaveSquadStepExecutionActivity saveSquadStepExecutionActivity = Workflow
			.newActivityStub(SaveSquadStepExecutionActivity.class, buildActivityOptions());

	private final Map<String, SquadStep> stepsById = new LinkedHashMap<>();
	private final Map<String, SquadStepStatus> stepStatusesById = new LinkedHashMap<>();
	private final Set<String> startedStepIds = new LinkedHashSet<>();
	private final Set<String> finishedStepIds = new LinkedHashSet<>();
	private final Map<String, SquadStepExecutionResult> resultsByStepId = new LinkedHashMap<>();

	private String squadId;
	private String squadRunId;
	private WorkflowRunStatus overallStatus = WorkflowRunStatus.RUNNING;
	private boolean workflowFailed;
	private String workflowFailureMessage;
	private String workflowFailureType;

	@Override
	public SquadExecutionResult execute(SquadExecutionRequest request) {
		squadId = request.getSquadId();
		squadRunId = Workflow.getInfo().getWorkflowId();
		GetSquadResult getSquadResult = getSquadActivity
				.getSquad(GetSquadRequest.builder().squadId(request.getSquadId()).build());

		Squad squad = getSquadResult.getSquad();
		squadId = squad.getId();

		List<SquadStep> steps = squad.getSteps();
		for (SquadStep step : steps) {
			stepsById.put(step.getId(), step);
			stepStatusesById.put(step.getId(), SquadStepStatus.builder().stepId(step.getId()).stepName(step.getName())
					.status(SquadStepExecutionStatus.PENDING).build());
		}

		Map<String, Set<String>> dependenciesByStepId = buildDependenciesMap(squad.getEdges(), stepsById.keySet());

		while (finishedStepIds.size() < stepsById.size()) {
			List<SquadStep> readySteps = findReadySteps(steps, dependenciesByStepId, startedStepIds, finishedStepIds);

			if (readySteps.isEmpty() && startedStepIds.size() == finishedStepIds.size()) {
				throw ApplicationFailure.newNonRetryableFailure("No executable steps remain for squad " + squad.getId()
						+ ". The graph may contain a cycle or invalid dependencies.", "SQUAD_GRAPH_INVALID");
			}

			for (SquadStep readyStep : readySteps) {
				startedStepIds.add(readyStep.getId());
				startStepExecution(readyStep, dependenciesByStepId);
			}

			int finishedCountBeforeWait = finishedStepIds.size();
			Workflow.await(() -> finishedStepIds.size() > finishedCountBeforeWait || workflowFailed);

			if (workflowFailed) {
				overallStatus = WorkflowRunStatus.FAILED;
				throw ApplicationFailure.newNonRetryableFailure(workflowFailureMessage, workflowFailureType);
			}
		}

		overallStatus = WorkflowRunStatus.COMPLETED;
		return SquadExecutionResult.builder().squadId(squad.getId()).status("COMPLETED")
				.message("Executed " + resultsByStepId.size() + " step(s) for squad " + squad.getId()).build();
	}

	@Override
	public SquadExecutionStatus getExecutionStatus() {
		return SquadExecutionStatus.builder().squadId(squadId).overallStatus(overallStatus)
				.steps(buildStepStatusesSnapshot()).build();
	}

	private void startStepExecution(SquadStep step, Map<String, Set<String>> dependenciesByStepId) {
		Map<String, Object> stepInput = buildStepInput(step, dependenciesByStepId);
		markStepRunning(step);

		Promise<SquadStepExecutionResult> stepPromise = Async.function(() -> executeStep(squadId, step, stepInput));
		stepPromise.handle((stepExecutionResult, throwable) -> {
			if (throwable != null) {
				markStepFailed(step, stepInput, throwable);
				return null;
			}

			resultsByStepId.put(step.getId(), stepExecutionResult);
			persistStepExecution(step, stepInput, stepExecutionResult);
			markStepCompleted(step, stepExecutionResult);
			return stepExecutionResult;
		});
	}

	private Map<String, Set<String>> buildDependenciesMap(List<SquadEdge> edges, Set<String> stepIds) {
		Map<String, Set<String>> dependenciesByStepId = new HashMap<>();
		for (String stepId : stepIds) {
			dependenciesByStepId.put(stepId, new LinkedHashSet<>());
		}

		for (SquadEdge edge : edges) {
			if (!stepIds.contains(edge.getSourceStepId()) || !stepIds.contains(edge.getTargetStepId())) {
				throw ApplicationFailure
						.newNonRetryableFailure(
								"Invalid edge " + edge.getId() + " references unknown steps. source="
										+ edge.getSourceStepId() + ", target=" + edge.getTargetStepId(),
								"SQUAD_GRAPH_INVALID");
			}

			dependenciesByStepId.get(edge.getTargetStepId()).add(edge.getSourceStepId());
		}

		return dependenciesByStepId;
	}

	private List<SquadStep> findReadySteps(List<SquadStep> steps, Map<String, Set<String>> dependenciesByStepId,
			Set<String> startedStepIds, Set<String> finishedStepIds) {
		List<SquadStep> readySteps = new ArrayList<>();

		for (SquadStep step : steps) {
			if (startedStepIds.contains(step.getId())) {
				continue;
			}

			Set<String> dependencies = dependenciesByStepId.getOrDefault(step.getId(), new HashSet<>());
			if (finishedStepIds.containsAll(dependencies)) {
				readySteps.add(step);
			}
		}

		return readySteps;
	}

	private Map<String, Object> buildStepInput(SquadStep step, Map<String, Set<String>> dependenciesByStepId) {
		Map<String, Object> stepInput = new LinkedHashMap<>();
		Set<String> dependencies = dependenciesByStepId.getOrDefault(step.getId(), new LinkedHashSet<>());

		for (String dependencyStepId : dependencies) {
			SquadStepExecutionResult dependencyResult = resultsByStepId.get(dependencyStepId);
			if (dependencyResult == null) {
				throw ApplicationFailure.newNonRetryableFailure(
						"Missing execution result for dependency " + dependencyStepId + " of step " + step.getId(),
						"SQUAD_STEP_DEPENDENCY_RESULT_MISSING");
			}

			stepInput.put(dependencyStepId, dependencyResult.getOutput());
		}

		return stepInput;
	}

	private SquadStepExecutionResult executeStep(String squadId, SquadStep step, Map<String, Object> input) {
		switch (step.getType()) {
			case AI_AGENT :
				AiAgentStep aiAgentStep = (AiAgentStep) step;
				return runAiAgentActivity.runAiAgent(SquadStepExecutionRequest.builder().squadId(squadId)
						.stepId(aiAgentStep.getId()).stepName(aiAgentStep.getName()).agentKey(aiAgentStep.getAgentKey())
						.input(input).build());
			default :
				throw ApplicationFailure.newNonRetryableFailure(
						"Unsupported step type " + step.getType() + " for step " + step.getId(),
						"UNSUPPORTED_STEP_TYPE");
		}
	}

	private void persistStepExecution(SquadStep step, Map<String, Object> input, SquadStepExecutionResult result) {
		saveSquadStepExecutionActivity
				.saveSquadStepExecution(SaveSquadStepExecutionRequest.builder().squadRunId(squadRunId).squadId(squadId)
						.stepId(step.getId()).stepName(step.getName()).status(result.getStatus())
						.message(result.getMessage()).input(input).output(result.getOutput()).build());
	}

	private void markStepRunning(SquadStep step) {
		updateStepStatus(step, SquadStepExecutionStatus.RUNNING, "Running step \"" + step.getName() + "\".");
	}

	private void markStepCompleted(SquadStep step, SquadStepExecutionResult result) {
		updateStepStatus(step, SquadStepExecutionStatus.COMPLETED, result.getMessage());
		finishedStepIds.add(step.getId());
	}

	private void markStepFailed(SquadStep step, Map<String, Object> input, Throwable throwable) {
		String failureMessage = extractFailureMessage(throwable);
		String workflowMessage = "Step \"" + step.getName() + "\" failed: " + failureMessage;
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("error", failureMessage);

		SquadStepExecutionResult failedResult = SquadStepExecutionResult.builder().stepId(step.getId()).status("FAILED")
				.message(workflowMessage).output(output).build();
		resultsByStepId.put(step.getId(), failedResult);
		persistStepExecution(step, input, failedResult);
		updateStepStatus(step, SquadStepExecutionStatus.FAILED, workflowMessage);
		finishedStepIds.add(step.getId());
		workflowFailed = true;
		workflowFailureMessage = workflowMessage;
		workflowFailureType = "SQUAD_STEP_FAILED";
	}

	private String extractFailureMessage(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}

		String message = current.getMessage();
		if (message == null || message.isEmpty()) {
			message = current.getClass().getSimpleName();
		}
		return message;
	}

	private void updateStepStatus(SquadStep step, SquadStepExecutionStatus status, String message) {
		SquadStepStatus stepStatus = stepStatusesById.get(step.getId());
		if (stepStatus == null) {
			stepStatus = SquadStepStatus.builder().stepId(step.getId()).stepName(step.getName()).build();
			stepStatusesById.put(step.getId(), stepStatus);
		}

		stepStatus.setStatus(status);
		stepStatus.setMessage(message);
	}

	private List<SquadStepStatus> buildStepStatusesSnapshot() {
		List<SquadStepStatus> stepStatuses = new ArrayList<>();
		for (SquadStep step : stepsById.values()) {
			SquadStepStatus stepStatus = stepStatusesById.get(step.getId());
			if (stepStatus != null) {
				stepStatuses.add(stepStatus);
			}
		}
		return stepStatuses;
	}

	private ActivityOptions buildActivityOptions() {
		RetryOptions retryOptions = RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1))
				.setMaximumInterval(Duration.ofSeconds(20)).setBackoffCoefficient(2).setMaximumAttempts(3).build();

		return ActivityOptions.newBuilder().setRetryOptions(retryOptions).setStartToCloseTimeout(Duration.ofSeconds(30))
				.setScheduleToCloseTimeout(Duration.ofMinutes(5)).build();
	}
}
