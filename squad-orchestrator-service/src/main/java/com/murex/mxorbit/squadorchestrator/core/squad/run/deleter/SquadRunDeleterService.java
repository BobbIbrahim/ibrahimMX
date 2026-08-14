package com.murex.mxorbit.squadorchestrator.core.squad.run.deleter;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.store.SquadStepExecutionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.SquadRoutingDecisionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.run.provider.SquadRunProvider;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunDeleterService implements SquadRunDeleter {

	private final SquadRunProvider squadRunProvider;
	private final SquadStepExecutionStore squadStepExecutionStore;
	private final SquadRoutingDecisionStore squadRoutingDecisionStore;
	private final TemporalClient temporalClient;

	@Override
	public void deleteSquadRuns(String squadId) {
		log.debug("Deleting runs for squad. squadId: {}", squadId);

		List<String> squadRunIds = squadRunProvider.getAllSquadRunIds(squadId);

		for (String squadRunId : squadRunIds) {
			temporalClient.deleteWorkflowExecution(squadRunId);
		}

		squadStepExecutionStore.deleteBySquadId(squadId);
		squadRoutingDecisionStore.deleteBySquadId(squadId);

		log.info("Squad runs deleted. squadId: {}, runCount: {}", squadId, squadRunIds.size());
	}

	@Override
	public boolean deleteSquadRun(String squadRunId) {
		log.debug("Deleting squad run. squadRunId: {}", squadRunId);

		Optional<SquadExecutionStatus> squadRunStatus = squadRunProvider.getSquadRunStatus(squadRunId);

		if (squadRunStatus.isEmpty()) {
			return false;
		}

		if (squadRunStatus.get().getOverallStatus() == WorkflowRunStatus.RUNNING) {
			log.info("Refusing to delete squad run still in progress. squadRunId: {}", squadRunId);
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Run is still in progress. Cancel it before deleting.");
		}

		temporalClient.deleteWorkflowExecution(squadRunId);
		squadStepExecutionStore.deleteBySquadRunId(squadRunId);
		squadRoutingDecisionStore.deleteBySquadRunId(squadRunId);

		log.info("Squad run deleted. squadRunId: {}", squadRunId);
		return true;
	}
}
