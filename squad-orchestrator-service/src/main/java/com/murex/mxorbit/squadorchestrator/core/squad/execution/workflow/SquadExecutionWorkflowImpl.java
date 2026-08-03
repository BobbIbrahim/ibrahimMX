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
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConditionEvaluator;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionException;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionService;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadRoutingDecisionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadRoutingDecisionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WorkflowImpl(taskQueues = "squad-orchestration-task-queue")
public class SquadExecutionWorkflowImpl implements SquadExecutionWorkflow {

	private final GetSquadActivity getSquadActivity = Workflow.newActivityStub(GetSquadActivity.class,
			buildActivityOptions());

	private final RunAiAgentActivity runAiAgentActivity = Workflow.newActivityStub(RunAiAgentActivity.class,
			buildActivityOptions());

	private final SaveSquadStepExecutionActivity saveSquadStepExecutionActivity = Workflow
			.newActivityStub(SaveSquadStepExecutionActivity.class, buildActivityOptions());

	private final SaveSquadRoutingDecisionActivity saveSquadRoutingDecisionActivity = Workflow
			.newActivityStub(SaveSquadRoutingDecisionActivity.class, buildActivityOptions());

	/**
	 * Temporal workflows are instantiated by the Temporal worker runtime, not
	 * Spring, so routing must be created locally in a deterministic way.
	 */
	private final SquadRoutingDecisionService routingDecisionService = new SquadRoutingDecisionService(
			new SquadRoutingConditionEvaluator());

	private final Map<String, SquadStep> stepsById = new LinkedHashMap<>();
	private final Map<String, SquadStepStatus> stepStatusesById = new LinkedHashMap<>();
	private final Map<String, SquadStepExecutionResult> resultsByStepId = new LinkedHashMap<>();
	private final Map<String, List<SquadEdge>> outgoingEdgesBySourceStepId = new LinkedHashMap<>();
	private final List<SquadRoutingDecision> routingDecisions = new ArrayList<>();

	private String squadId;
	private String squadRunId;
	private Map<String, Object> initialInput = new LinkedHashMap<>();
	private WorkflowRunStatus overallStatus = WorkflowRunStatus.RUNNING;

	@Override
	public SquadExecutionResult execute(SquadExecutionRequest request) {
		squadId = request.getSquadId();
		squadRunId = Workflow.getInfo().getWorkflowId();
		initialInput = request.getInitialInput() == null ? Map.of() : request.getInitialInput();
		GetSquadResult getSquadResult = getSquadActivity
				.getSquad(GetSquadRequest.builder().squadId(request.getSquadId()).build());

		Squad squad = getSquadResult.getSquad();
		squadId = squad.getId();

		List<SquadStep> steps = squad.getSteps();
		if (steps == null || steps.isEmpty()) {
			throw ApplicationFailure.newNonRetryableFailure("Squad " + squad.getId() + " has no steps.",
					"SQUAD_GRAPH_INVALID");
		}

		for (SquadStep step : steps) {
			stepsById.put(step.getId(), step);
			stepStatusesById.put(step.getId(), SquadStepStatus.builder().stepId(step.getId()).stepName(step.getName())
					.status(SquadStepExecutionStatus.PENDING).build());
		}

		buildOutgoingEdgesBySource(squad.getEdges() == null ? List.of() : squad.getEdges(), stepsById.keySet());
		String currentStepId = findSingleRootStepId(steps, squad.getEdges() == null ? List.of() : squad.getEdges());
		Set<String> visitedStepIds = new LinkedHashSet<>();

		try {
			while (true) {
				if (!visitedStepIds.add(currentStepId)) {
					throw ApplicationFailure.newNonRetryableFailure(
							"Detected a cycle while traversing selected route at step '" + currentStepId + "'.",
							"SQUAD_GRAPH_INVALID");
				}

				SquadStep currentStep = stepsById.get(currentStepId);
				SquadStepExecutionResult currentResult = executeSelectedStep(currentStep);

				List<SquadEdge> outgoingEdges = outgoingEdgesBySourceStepId.getOrDefault(currentStepId, List.of());
				if (outgoingEdges.isEmpty()) {
					markPendingStepsSkipped("Step \"" + currentStep.getName() + "\" was not followed by any route.");
					break;
				}

				SquadRoutingDecision routingDecision = decideOutgoingEdgeOrFail(currentStepId,
						currentResult.getOutput(), outgoingEdges);

				SquadEdge selectedEdge = routingDecision.getSelectedEdge();
				markUnreachablePendingStepsAfterSelection(selectedEdge.getTargetStepId());
				currentStepId = selectedEdge.getTargetStepId();
			}
		} catch (ApplicationFailure failure) {
			overallStatus = WorkflowRunStatus.FAILED;
			throw failure;
		}

		overallStatus = WorkflowRunStatus.COMPLETED;
		return SquadExecutionResult.builder().squadId(squad.getId()).status("COMPLETED")
				.message("Executed " + resultsByStepId.size() + " step(s) for squad " + squad.getId()).build();
	}

	@Override
	public SquadExecutionStatus getExecutionStatus() {
		return SquadExecutionStatus.builder().squadId(squadId).overallStatus(overallStatus)
				.steps(buildStepStatusesSnapshot()).routingDecisions(new ArrayList<>(routingDecisions)).build();
	}

	private SquadStepExecutionResult executeSelectedStep(SquadStep step) {
		Map<String, Map<String, Object>> stepOutputsByStepId = buildStepOutputsByStepId();
		Map<String, Object> seedInput = isRootStep(step.getId()) ? initialInput : Map.of();
		Map<String, Object> stepInput = resolveStepInput(step, stepOutputsByStepId, seedInput);
		markStepRunning(step, stepInput);

		try {
			SquadStepExecutionResult stepExecutionResult = executeStep(squadId, step, stepOutputsByStepId, seedInput);
			resultsByStepId.put(step.getId(), stepExecutionResult);
			markStepCompleted(step, stepExecutionResult.getInput(), stepExecutionResult);
			persistStepExecution(step, stepExecutionResult.getInput(), stepExecutionResult);
			return stepExecutionResult;
		} catch (Throwable throwable) {
			markStepFailed(step, stepInput, throwable);
			throw ApplicationFailure.newNonRetryableFailure(
					"Step \"" + step.getName() + "\" failed: " + extractFailureMessage(throwable), "SQUAD_STEP_FAILED");
		}
	}

	private SquadRoutingDecision decideOutgoingEdgeOrFail(String sourceStepId, Map<String, Object> sourceOutput,
			List<SquadEdge> outgoingEdges) {
		try {
			SquadRoutingDecision decision = routingDecisionService.decide(sourceStepId, sourceOutput, outgoingEdges);

			recordRoutingDecision(decision);
			return decision;
		} catch (SquadRoutingDecisionException exception) {
			if (exception.getDecision() != null) {
				recordRoutingDecision(exception.getDecision());
			}

			throw ApplicationFailure.newNonRetryableFailure(exception.getMessage(), "SQUAD_ROUTING_DECISION_FAILED");
		}
	}

	private void recordRoutingDecision(SquadRoutingDecision decision) {
		int decisionSequence = routingDecisions.size();
		routingDecisions.add(decision);

		saveSquadRoutingDecisionActivity.saveSquadRoutingDecision(SaveSquadRoutingDecisionRequest.builder()
				.squadRunId(squadRunId).squadId(squadId).decisionSequence(decisionSequence).decision(decision).build());
	}

	private void buildOutgoingEdgesBySource(List<SquadEdge> edges, Set<String> stepIds) {
		for (String stepId : stepIds) {
			outgoingEdgesBySourceStepId.put(stepId, new ArrayList<>());
		}

		for (SquadEdge edge : edges) {
			if (!stepIds.contains(edge.getSourceStepId()) || !stepIds.contains(edge.getTargetStepId())) {
				throw ApplicationFailure
						.newNonRetryableFailure(
								"Invalid edge " + edge.getId() + " references unknown steps. source="
										+ edge.getSourceStepId() + ", target=" + edge.getTargetStepId(),
								"SQUAD_GRAPH_INVALID");
			}

			outgoingEdgesBySourceStepId.get(edge.getSourceStepId()).add(edge);
		}
	}

	private String findSingleRootStepId(List<SquadStep> steps, List<SquadEdge> edges) {
		Set<String> targetStepIds = new LinkedHashSet<>();
		for (SquadEdge edge : edges) {
			targetStepIds.add(edge.getTargetStepId());
		}

		List<String> rootStepIds = new ArrayList<>();
		for (SquadStep step : steps) {
			if (!targetStepIds.contains(step.getId())) {
				rootStepIds.add(step.getId());
			}
		}

		if (rootStepIds.size() != 1) {
			throw ApplicationFailure.newNonRetryableFailure(
					"Squad " + squadId + " must have exactly one root step but found " + rootStepIds.size() + ".",
					"SQUAD_GRAPH_INVALID");
		}

		return rootStepIds.get(0);
	}

	private boolean isRootStep(String stepId) {
		for (List<SquadEdge> outgoingEdges : outgoingEdgesBySourceStepId.values()) {
			for (SquadEdge edge : outgoingEdges) {
				if (stepId.equals(edge.getTargetStepId())) {
					return false;
				}
			}
		}
		return true;
	}

	private void markUnreachablePendingStepsAfterSelection(String selectedTargetStepId) {
		Set<String> reachableStepIds = computeReachableStepIds(selectedTargetStepId);
		for (SquadStep step : stepsById.values()) {
			SquadStepStatus stepStatus = stepStatusesById.get(step.getId());
			if (stepStatus != null && stepStatus.getStatus() == SquadStepExecutionStatus.PENDING
					&& !reachableStepIds.contains(step.getId())) {
				markStepSkipped(step, "Step \"" + step.getName() + "\" is not on the selected route.");
			}
		}
	}

	private Set<String> computeReachableStepIds(String startStepId) {
		Set<String> reachableStepIds = new LinkedHashSet<>();
		ArrayDeque<String> queue = new ArrayDeque<>();
		queue.add(startStepId);

		while (!queue.isEmpty()) {
			String stepId = queue.removeFirst();
			if (!reachableStepIds.add(stepId)) {
				continue;
			}

			List<SquadEdge> outgoingEdges = outgoingEdgesBySourceStepId.getOrDefault(stepId, List.of());
			for (SquadEdge edge : outgoingEdges) {
				queue.addLast(edge.getTargetStepId());
			}
		}

		return reachableStepIds;
	}

	private void markPendingStepsSkipped(String reason) {
		for (SquadStep step : stepsById.values()) {
			SquadStepStatus stepStatus = stepStatusesById.get(step.getId());
			if (stepStatus != null && stepStatus.getStatus() == SquadStepExecutionStatus.PENDING) {
				markStepSkipped(step, reason);
			}
		}
	}

	private void markStepSkipped(SquadStep step, String message) {
		updateStepStatus(step, SquadStepExecutionStatus.SKIPPED, message, null, null);
	}

	private Map<String, Map<String, Object>> buildStepOutputsByStepId() {
		Map<String, Map<String, Object>> stepOutputsByStepId = new LinkedHashMap<>();
		for (Map.Entry<String, SquadStepExecutionResult> entry : resultsByStepId.entrySet()) {
			Map<String, Object> stepOutput = entry.getValue().getOutput();
			stepOutputsByStepId.put(entry.getKey(), new LinkedHashMap<>(stepOutput));
		}
		return stepOutputsByStepId;
	}

	/**
	 * Best-effort resolution of a step's input, mirroring the mapping performed by
	 * {@code RunAiAgentActivityImpl}. Used so the RUNNING/FAILED status reflects
	 * the actual resolved input attempted for the step, rather than the raw
	 * upstream step outputs. The activity remains the authoritative resolver
	 * (including validation) for a successful run.
	 */
	private Map<String, Object> resolveStepInput(SquadStep step, Map<String, Map<String, Object>> stepOutputsByStepId,
			Map<String, Object> seedInput) {
		List<StepInputRef> inputRefs = step.getInputRefs();
		if (inputRefs == null || inputRefs.isEmpty()) {
			return new LinkedHashMap<>(seedInput);
		}

		Map<String, Object> input = new LinkedHashMap<>();
		for (StepInputRef inputRef : inputRefs) {
			Map<String, Object> fromStepOutput = stepOutputsByStepId.get(inputRef.getFromStepId());
			Object value = fromStepOutput == null ? null : fromStepOutput.get(inputRef.getKey());
			input.put(inputRef.getTargetInput(), value);
		}
		return input;
	}

	private SquadStepExecutionResult executeStep(String squadId, SquadStep step,
			Map<String, Map<String, Object>> stepOutputsByStepId, Map<String, Object> seedInput) {
		switch (step.getType()) {
			case AI_AGENT :
				AiAgentStep aiAgentStep = (AiAgentStep) step;
				return runAiAgentActivity.runAiAgent(SquadStepExecutionRequest.builder().squadId(squadId)
						.stepId(aiAgentStep.getId()).stepName(aiAgentStep.getName()).agentKey(aiAgentStep.getAgentKey())
						.inputRefs(aiAgentStep.getInputRefs() == null
								? List.of()
								: new ArrayList<>(aiAgentStep.getInputRefs()))
						.stepOutputsByStepId(stepOutputsByStepId).seedInput(seedInput).build());
			default :
				throw ApplicationFailure.newNonRetryableFailure(
						"Unsupported step type " + step.getType() + " for step " + step.getId(),
						"UNSUPPORTED_STEP_TYPE");
		}
	}

	private void persistStepExecution(SquadStep step, Map<String, Object> input, SquadStepExecutionResult result) {
		SquadStepStatus stepStatus = stepStatusesById.get(step.getId());

		saveSquadStepExecutionActivity
				.saveSquadStepExecution(SaveSquadStepExecutionRequest.builder().squadRunId(squadRunId).squadId(squadId)
						.stepId(step.getId()).stepName(step.getName()).status(result.getStatus())
						.message(result.getMessage()).startedAt(stepStatus == null ? null : stepStatus.getStartedAt())
						.completedAt(stepStatus == null ? null : stepStatus.getCompletedAt())
						.durationMs(stepStatus == null ? null : stepStatus.getDurationMs()).input(input)
						.output(result.getOutput()).build());
	}

	private void markStepRunning(SquadStep step, Map<String, Object> input) {
		updateStepStatus(step, SquadStepExecutionStatus.RUNNING, "Running step \"" + step.getName() + "\".", input,
				null);
	}

	private void markStepCompleted(SquadStep step, Map<String, Object> input, SquadStepExecutionResult result) {
		updateStepStatus(step, SquadStepExecutionStatus.COMPLETED, result.getMessage(), input, result.getOutput());
	}

	private void markStepFailed(SquadStep step, Map<String, Object> input, Throwable throwable) {
		String failureMessage = extractFailureMessage(throwable);
		String workflowMessage = "Step \"" + step.getName() + "\" failed: " + failureMessage;
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("error", failureMessage);

		SquadStepExecutionResult failedResult = SquadStepExecutionResult.builder().stepId(step.getId()).status("FAILED")
				.message(workflowMessage).output(output).build();
		resultsByStepId.put(step.getId(), failedResult);
		updateStepStatus(step, SquadStepExecutionStatus.FAILED, workflowMessage, input, output);
		persistStepExecution(step, input, failedResult);
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

	private void updateStepStatus(SquadStep step, SquadStepExecutionStatus status, String message,
			Map<String, Object> input, Map<String, Object> output) {
		SquadStepStatus stepStatus = stepStatusesById.get(step.getId());

		if (stepStatus == null) {
			stepStatus = SquadStepStatus.builder().stepId(step.getId()).stepName(step.getName()).build();

			stepStatusesById.put(step.getId(), stepStatus);
		}

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, status,
				Instant.ofEpochMilli(Workflow.currentTimeMillis()));

		stepStatus.setStatus(status);
		stepStatus.setMessage(message);
		if (input != null) {
			stepStatus.setInput(copy(input));
		}
		if (output != null) {
			stepStatus.setOutput(copy(output));
		}
		log.debug(
				"Step status updated. squadRunId: {}, stepId: {}, status: {}, startedAt: {}, completedAt: {}, durationMs: {}",
				squadRunId, step.getId(), status, stepStatus.getStartedAt(), stepStatus.getCompletedAt(),
				stepStatus.getDurationMs());
	}

	private Map<String, Object> copy(Map<String, Object> source) {
		return new LinkedHashMap<>(source);
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
