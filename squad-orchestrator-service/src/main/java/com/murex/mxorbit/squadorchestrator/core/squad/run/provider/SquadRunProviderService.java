package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.SquadStepExecutionJpaStore;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowExecutionSummary;
import io.temporal.client.WorkflowNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunProviderService implements SquadRunProvider {

	private static final String SQUAD_EXECUTION_WORKFLOW_TYPE = SquadExecutionWorkflow.class.getSimpleName();

	private final TemporalClient temporalClient;
	private final SquadStepExecutionJpaStore squadStepExecutionJpaStore;

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
			log.debug("Execution status snapshot before API return. squadRunId: {}, steps: {}", squadRunId, status
					.getSteps().stream()
					.map(step -> step.getStepId() + "=" + step.getStatus() + "(startedAt=" + step.getStartedAt()
							+ ", completedAt=" + step.getCompletedAt() + ", durationMs=" + step.getDurationMs() + ")")
					.toList());
			return Optional.of(status);
		} catch (WorkflowNotFoundException e) {
			log.debug("Squad run not found. squadRunId: {}", squadRunId);
			return Optional.empty();
		}
	}

	private void enrichWithStepExecutionData(String squadRunId, SquadExecutionStatus status) {
		var stepExecutionDataMap = squadStepExecutionJpaStore.findBySquadRunId(squadRunId).stream()
				.collect(java.util.stream.Collectors.toMap(data -> data.getStepId(), data -> data));

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
		if (execution.getStartTime() != null && execution.getCloseTime() != null) {
			durationMs = Duration.between(execution.getStartTime(), execution.getCloseTime()).toMillis();
		}

		return SquadRunSummary.builder().squadId(memo.get(SquadRunMemoKeys.SQUAD_ID))
				.squadName(memo.get(SquadRunMemoKeys.SQUAD_NAME)).squadRunId(execution.getWorkflowId())
				.startedAt(execution.getStartTime()).overallStatus(execution.getStatus())
				.completedAt(execution.getCloseTime()).durationMs(durationMs).build();
	}
}
