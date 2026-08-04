package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.store.SquadStepExecutionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.SquadRoutingDecisionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowExecutionSummary;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import io.temporal.client.WorkflowNotFoundException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunProviderService implements SquadRunProvider {

	private static final String SQUAD_EXECUTION_WORKFLOW_TYPE = SquadExecutionWorkflow.class.getSimpleName();

	private final TemporalClient temporalClient;

	private final SquadStepExecutionStore squadStepExecutionStore;

	private final SquadRoutingDecisionStore squadRoutingDecisionStore;

	private final SquadProvider squadProvider;

	@Override
	public List<SquadRunSummary> getSquadRuns() {
		log.debug("Listing squad runs from Temporal");
		return temporalClient.listWorkflowExecutions(SQUAD_EXECUTION_WORKFLOW_TYPE).stream()
				.map(this::toSquadRunSummary).toList();
	}

	@Override
	public Optional<SquadExecutionStatus> getSquadRunStatus(String squadRunId) {
		log.debug("Getting squad run execution status. squadRunId: {}", squadRunId);

		SquadExecutionWorkflow workflow = temporalClient.getWorkflowExecutionStub(SquadExecutionWorkflow.class,
				squadRunId);

		try {
			SquadExecutionStatus status = workflow.getExecutionStatus();
			enrichWithStepExecutionData(squadRunId, status);
			enrichWithRoutingDecisions(squadRunId, status);
			populateFinalResult(status);

			log.debug("Execution status snapshot before API return. squadRunId: {}, steps: {}, routingDecisions: {}",
					squadRunId,
					status.getSteps().stream()
							.map(step -> step.getStepId() + "=" + step.getStatus() + "(startedAt=" + step.getStartedAt()
									+ ", completedAt=" + step.getCompletedAt() + ", durationMs=" + step.getDurationMs()
									+ ")")
							.toList(),
					status.getRoutingDecisions().size());

			return Optional.of(status);
		} catch (WorkflowNotFoundException exception) {
			log.debug("Squad run not found. squadRunId: {}", squadRunId);
			return Optional.empty();
		}
	}

	private void enrichWithRoutingDecisions(String squadRunId, SquadExecutionStatus status) {
		List<SquadRoutingDecision> persistedDecisions = squadRoutingDecisionStore.findBySquadRunId(squadRunId);

		if (!persistedDecisions.isEmpty()) {
			status.setRoutingDecisions(new ArrayList<>(persistedDecisions));
			return;
		}

		if (status.getRoutingDecisions() == null) {
			status.setRoutingDecisions(new ArrayList<>());
		}
	}

	private void populateFinalResult(SquadExecutionStatus status) {
		if (status.getOverallStatus() != WorkflowRunStatus.COMPLETED) {
			return;
		}

		Optional<String> terminalStepId = findSelectedTerminalStepId(status);

		if (terminalStepId.isEmpty()) {
			terminalStepId = squadProvider.getSquadById(status.getSquadId())
					.flatMap(SquadRunProviderService::findTerminalStepId);
		}

		terminalStepId
				.flatMap(stepId -> status.getSteps().stream().filter(step -> step.getStepId().equals(stepId))
						.filter(step -> step.getStatus() == SquadStepExecutionStatus.COMPLETED).findFirst())
				.ifPresent(terminalStep -> status.setFinalResult(terminalStep.getOutput()));
	}

	static Optional<String> findSelectedTerminalStepId(SquadExecutionStatus status) {
		List<SquadRoutingDecision> routingDecisions = status.getRoutingDecisions();

		if (routingDecisions != null && !routingDecisions.isEmpty()) {
			for (int index = routingDecisions.size() - 1; index >= 0; index--) {
				String selectedTargetStepId = routingDecisions.get(index).getSelectedTargetStepId();
				if (selectedTargetStepId != null && !selectedTargetStepId.isBlank()) {
					return Optional.of(selectedTargetStepId);
				}
			}
		}

		return status.getSteps().stream().filter(step -> step.getStatus() == SquadStepExecutionStatus.COMPLETED)
				.reduce((first, second) -> second).map(SquadStepStatus::getStepId);
	}

	static Optional<String> findTerminalStepId(Squad squad) {
		Set<String> sourceStepIds = squad.getEdges().stream().map(SquadEdge::getSourceStepId)
				.collect(Collectors.toSet());

		return squad.getSteps().stream().map(SquadStep::getId).filter(stepId -> !sourceStepIds.contains(stepId))
				.findFirst();
	}

	private void enrichWithStepExecutionData(String squadRunId, SquadExecutionStatus status) {
		var stepExecutionDataMap = squadStepExecutionStore.findBySquadRunId(squadRunId).stream()
				.collect(Collectors.toMap(SquadStepExecutionData::getStepId, Function.identity()));

		status.getSteps().forEach(step -> {
			var executionData = stepExecutionDataMap.get(step.getStepId());
			if (executionData != null) {
				applyExecutionData(step, executionData);
			}
		});
	}

	static void applyExecutionData(SquadStepStatus step, SquadStepExecutionData executionData) {
		if (step.getStartedAt() == null) {
			step.setStartedAt(executionData.getStartedAt());
		}
		if (step.getCompletedAt() == null) {
			step.setCompletedAt(executionData.getCompletedAt());
		}
		if (step.getDurationMs() == null) {
			step.setDurationMs(executionData.getDurationMs());
		}
		step.setInput(executionData.getInput());
		step.setOutput(executionData.getOutput());
	}

	private SquadRunSummary toSquadRunSummary(WorkflowExecutionSummary execution) {
		Map<String, String> memo = execution.getMemo();
		Long durationMs = null;

		if (execution.getCloseTime() != null) {
			durationMs = Duration.between(execution.getStartTime(), execution.getCloseTime()).toMillis();
		}

		return SquadRunSummary.builder().squadId(memo.get(SquadRunMemoKeys.SQUAD_ID))
				.squadName(memo.get(SquadRunMemoKeys.SQUAD_NAME)).squadRunId(execution.getWorkflowId())
				.startedAt(execution.getStartTime()).overallStatus(execution.getStatus())
				.completedAt(execution.getCloseTime()).durationMs(durationMs).build();
	}
}
