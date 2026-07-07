package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.Workflow;

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

	@Override
	public SquadExecutionResult execute(SquadExecutionRequest request) {
		GetSquadResult getSquadResult = getSquadActivity
				.getSquad(GetSquadRequest.builder().squadId(request.getSquadId()).build());
		Squad squad = getSquadResult.getSquad();

		List<SquadStep> steps = squad.getSteps();
		Map<String, SquadStep> stepsById = new LinkedHashMap<>();
		for (SquadStep step : steps) {
			stepsById.put(step.getId(), step);
		}

		Map<String, Set<String>> dependenciesByStepId = buildDependenciesMap(squad.getEdges(), stepsById.keySet());

		Set<String> startedStepIds = new LinkedHashSet<>();
		Set<String> finishedStepIds = new LinkedHashSet<>();
		Map<String, SquadStepExecutionResult> resultsByStepId = new LinkedHashMap<>();

		while (finishedStepIds.size() < stepsById.size()) {
			List<SquadStep> readySteps = findReadySteps(steps, dependenciesByStepId, startedStepIds, finishedStepIds);

			if (readySteps.isEmpty() && startedStepIds.size() == finishedStepIds.size()) {
				throw ApplicationFailure.newNonRetryableFailure("No executable steps remain for squad " + squad.getId()
						+ ". The graph may contain a cycle or invalid dependencies.", "SQUAD_GRAPH_INVALID");
			}

			for (SquadStep readyStep : readySteps) {
				startedStepIds.add(readyStep.getId());
				startStepExecution(squad.getId(), readyStep, finishedStepIds, resultsByStepId);
			}

			int finishedCountBeforeWait = finishedStepIds.size();
			Workflow.await(() -> finishedStepIds.size() > finishedCountBeforeWait);
		}

		return SquadExecutionResult.builder().squadId(squad.getId()).status("COMPLETED")
				.message("Executed " + resultsByStepId.size() + " step(s) for squad " + squad.getId()).build();
	}

	private void startStepExecution(String squadId, SquadStep step, Set<String> finishedStepIds,
			Map<String, SquadStepExecutionResult> resultsByStepId) {
		Async.function(() -> executeStep(squadId, step)).thenApply(stepExecutionResult -> {
			resultsByStepId.put(step.getId(), stepExecutionResult);
			finishedStepIds.add(step.getId());
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

	private SquadStepExecutionResult executeStep(String squadId, SquadStep step) {
		switch (step.getType()) {
			case AI_AGENT :
				AiAgentStep aiAgentStep = (AiAgentStep) step;
				return runAiAgentActivity
						.runAiAgent(SquadStepExecutionRequest.builder().squadId(squadId).stepId(aiAgentStep.getId())
								.stepName(aiAgentStep.getName()).agentKey(aiAgentStep.getAgentKey()).build());
			default :
				throw ApplicationFailure.newNonRetryableFailure(
						"Unsupported step type " + step.getType() + " for step " + step.getId(),
						"UNSUPPORTED_STEP_TYPE");
		}
	}

	private ActivityOptions buildActivityOptions() {
		RetryOptions retryOptions = RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1))
				.setMaximumInterval(Duration.ofSeconds(20)).setBackoffCoefficient(2).setMaximumAttempts(3).build();

		return ActivityOptions.newBuilder().setRetryOptions(retryOptions).setStartToCloseTimeout(Duration.ofSeconds(30))
				.setScheduleToCloseTimeout(Duration.ofMinutes(5)).build();
	}
}
