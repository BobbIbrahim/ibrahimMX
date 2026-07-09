package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowExecutionSummary;
import io.temporal.client.WorkflowNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunProviderService implements SquadRunProvider {

	private static final String SQUAD_EXECUTION_WORKFLOW_TYPE = SquadExecutionWorkflow.class.getSimpleName();

	private final TemporalClient temporalClient;

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
			return Optional.of(workflow.getExecutionStatus());
		} catch (WorkflowNotFoundException e) {
			log.debug("Squad run not found. squadRunId: {}", squadRunId);
			return Optional.empty();
		}
	}

	private SquadRunSummary toSquadRunSummary(WorkflowExecutionSummary execution) {
		Map<String, String> memo = execution.getMemo();

		return SquadRunSummary.builder().squadId(memo.get(SquadRunMemoKeys.SQUAD_ID))
				.squadName(memo.get(SquadRunMemoKeys.SQUAD_NAME)).squadRunId(execution.getWorkflowId())
				.startedAt(execution.getStartTime()).build();
	}
}
