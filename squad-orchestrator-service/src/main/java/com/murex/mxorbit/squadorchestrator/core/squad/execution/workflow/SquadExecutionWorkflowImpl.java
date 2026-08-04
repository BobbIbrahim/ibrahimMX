package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadStepExecutionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.input.StepInputResolver;
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
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConditionEvaluator;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConfigurationGuard;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionException;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionOutcome;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionService;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.SaveSquadRoutingDecisionActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingEdgeEvaluation;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request.SquadRoutingDecisionStoreRequest;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.CanceledFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WorkflowImpl(taskQueues = "squad-orchestration-task-queue")
public class SquadExecutionWorkflowImpl implements SquadExecutionWorkflow {

	private final GetSquadActivity getSquadActivity = Workflow.newActivityStub(GetSquadActivity.class,
			SquadActivityOptions.lookup());

	private final RunAiAgentActivity runAiAgentActivity = Workflow.newActivityStub(RunAiAgentActivity.class,
			SquadActivityOptions.aiAgent());

	private final SaveSquadStepExecutionActivity saveSquadStepExecutionActivity = Workflow
			.newActivityStub(SaveSquadStepExecutionActivity.class, SquadActivityOptions.persistence());

	private final SaveSquadRoutingDecisionActivity saveSquadRoutingDecisionActivity = Workflow
			.newActivityStub(SaveSquadRoutingDecisionActivity.class, SquadActivityOptions.persistence());

	/**
	 * Temporal workflows are instantiated by the Temporal worker runtime, not
	 * Spring, so routing must be created locally in a deterministic way.
	 */
	private final SquadRoutingDecisionService routingDecisionService = new SquadRoutingDecisionService(
			new SquadRoutingConditionEvaluator(), new SquadRoutingConfigurationGuard());

	private final Map<String, SquadStep> stepsById = new LinkedHashMap<>();
	private final Map<String, SquadStepStatus> stepStatusesById = new LinkedHashMap<>();
	private final Map<String, SquadStepExecutionResult> resultsByStepId = new LinkedHashMap<>();
	private final List<SquadRoutingDecision> routingDecisions = new ArrayList<>();

	private SquadGraph squadGraph;
	private String squadId;
	private String squadRunId;
	private Map<String, Object> initialInput = new LinkedHashMap<>();
	private WorkflowRunStatus overallStatus = WorkflowRunStatus.RUNNING;

	@Override
	public SquadExecutionResult execute(SquadExecutionRequest request) {
		try {
			return executeSquad(request);
		} catch (CanceledFailure failure) {
			overallStatus = WorkflowRunStatus.CANCELLED;
			throw failure;
		} catch (ActivityFailure | ChildWorkflowFailure | ApplicationFailure failure) {
			overallStatus = isCancellation(failure) ? WorkflowRunStatus.CANCELLED : WorkflowRunStatus.FAILED;
			throw failure;
		}
	}

	/**
	 * A cancelled run reaches the workflow as a CanceledFailure nested inside the
	 * failure of whichever activity was in flight.
	 */
	private static boolean isCancellation(Throwable failure) {
		Throwable current = failure;

		while (current != null) {
			if (current instanceof CanceledFailure) {
				return true;
			}

			Throwable cause = current.getCause();
			current = cause == current ? null : cause;
		}

		return false;
	}

	private SquadExecutionResult executeSquad(SquadExecutionRequest request) {
		squadRunId = Workflow.getInfo().getWorkflowId();
		// Published before the first blocking call so an early status query can answer.
		squadId = request.getSquadId();
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

		squadGraph = buildGraphOrFail(steps, squad.getEdges() == null ? List.of() : squad.getEdges());
		SquadExecutionPlan plan = new SquadExecutionPlan(squadGraph);

		while (true) {
			skipUnreachableSteps(plan);

			List<SquadStep> releasedSteps = plan.releasedStepIds().stream().map(stepsById::get).toList();
			if (releasedSteps.isEmpty()) {
				break;
			}

			runConcurrently(releasedSteps);

			for (SquadStep step : releasedSteps) {
				plan.markExecuted(step.getId());
				resolveOutgoingEdges(step, plan);
			}
		}

		failOnStalledSteps(plan);

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
		} catch (ActivityFailure | ChildWorkflowFailure | ApplicationFailure failure) {
			// Rethrown unwrapped so the cancellation cause survives for execute().
			if (isCancellation(failure)) {
				throw failure;
			}

			markStepFailed(step, stepInput, failure);
			throw ApplicationFailure.newNonRetryableFailure(
					"Step \"" + step.getName() + "\" failed: " + extractFailureMessage(failure), "SQUAD_STEP_FAILED");
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

		saveSquadRoutingDecisionActivity.saveSquadRoutingDecision(SquadRoutingDecisionStoreRequest.builder()
				.squadRunId(squadRunId).squadId(squadId).decisionSequence(decisionSequence).decision(decision).build());
	}

	private SquadGraph buildGraphOrFail(List<SquadStep> steps, List<SquadEdge> edges) {
		try {
			return SquadGraph.build(steps, edges);
		} catch (SquadGraphException exception) {
			throw ApplicationFailure.newNonRetryableFailure(exception.getMessage(), "SQUAD_GRAPH_INVALID");
		}
	}

	private boolean isRootStep(String stepId) {
		return stepId.equals(squadGraph.getRootStepId());
	}

	private void runConcurrently(List<SquadStep> steps) {
		if (steps.size() == 1) {
			executeSelectedStep(steps.get(0));
			return;
		}

		List<Promise<SquadStepExecutionResult>> stepPromises = steps.stream()
				.map(step -> Async.function(this::executeSelectedStep, step)).toList();

		Promise.allOf(stepPromises).get();
	}

	private void resolveOutgoingEdges(SquadStep step, SquadExecutionPlan plan) {
		List<SquadEdge> outgoingEdges = squadGraph.outgoingEdges(step.getId());
		if (outgoingEdges.isEmpty()) {
			return;
		}

		if (isParallelFanOut(outgoingEdges)) {
			outgoingEdges.forEach(edge -> recordRoutingDecision(fanOutDecision(step.getId(), edge)));
			plan.traverseAll(step.getId());
			return;
		}

		SquadStepExecutionResult result = resultsByStepId.get(step.getId());
		SquadRoutingDecision decision = decideOutgoingEdgeOrFail(step.getId(),
				result == null ? Map.of() : result.getOutput(), outgoingEdges);

		plan.traverseOnly(step.getId(), decision.getSelectedEdgeId());
	}

	/**
	 * Skipping cascades: a skipped step withdraws the routes its successors waited
	 * on.
	 */
	private void skipUnreachableSteps(SquadExecutionPlan plan) {
		List<String> unreachableStepIds = plan.unreachableStepIds();

		while (!unreachableStepIds.isEmpty()) {
			for (String stepId : unreachableStepIds) {
				SquadStep step = stepsById.get(stepId);
				markStepSkipped(step, "Step \"" + step.getName() + "\" is not on the selected route.");
				plan.markSkipped(stepId);
			}

			unreachableStepIds = plan.unreachableStepIds();
		}
	}

	/**
	 * Nothing is runnable yet steps remain pending, so the definition has a cycle.
	 */
	private void failOnStalledSteps(SquadExecutionPlan plan) {
		List<String> pendingStepIds = plan.pendingStepIds();

		if (!pendingStepIds.isEmpty()) {
			throw ApplicationFailure.newNonRetryableFailure(
					"Steps " + pendingStepIds + " were never reached; the squad graph contains a cycle.",
					"SQUAD_GRAPH_INVALID");
		}
	}

	private static boolean isParallelFanOut(List<SquadEdge> outgoingEdges) {
		return outgoingEdges.size() > 1 && outgoingEdges.stream().allMatch(SquadExecutionWorkflowImpl::isParallelEdge);
	}

	private static boolean isParallelEdge(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && !Boolean.TRUE.equals(edge.getIsDefault());
	}

	private static SquadRoutingDecision fanOutDecision(String sourceStepId, SquadEdge edge) {
		String reason = "Unconditional edge taken in parallel with its siblings.";

		return SquadRoutingDecision.builder().sourceStepId(sourceStepId).selectedEdgeId(edge.getId())
				.selectedTargetStepId(edge.getTargetStepId()).outcome(SquadRoutingDecisionOutcome.LEGACY_ALWAYS)
				.reason(reason)
				.checkedEdges(List.of(
						SquadRoutingEdgeEvaluation.builder().edgeId(edge.getId()).targetStepId(edge.getTargetStepId())
								.routingType(edge.getRoutingType()).condition(edge.getCondition())
								.priority(edge.getPriority()).isDefault(false).matched(true).reason(reason).build()))
				.build();
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
	 * Preview of the input the agent activity will build, so RUNNING and FAILED
	 * statuses report the resolved input rather than raw upstream outputs. The
	 * activity remains the authoritative resolver.
	 */
	private Map<String, Object> resolveStepInput(SquadStep step, Map<String, Map<String, Object>> stepOutputsByStepId,
			Map<String, Object> seedInput) {
		return StepInputResolver.resolveLenient(step.getId(), step.getInputRefs(), stepOutputsByStepId, seedInput);
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
}
