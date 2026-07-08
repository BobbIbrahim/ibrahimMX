package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.squad.run.store.SquadRunStore;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunProviderService implements SquadRunProvider {

	private static final String SQUAD_EXECUTION_WORKFLOW_TYPE = SquadExecutionWorkflow.class.getSimpleName();

	private final SquadRunStore squadRunStore;
	private final TemporalClient temporalClient;

	@Override
	public List<SquadRunSummary> getSquadRuns() {
		log.debug("Getting all squad runs with status");
		return squadRunStore
				.findAllWithSquadNames().stream().map(run -> SquadRunSummary.builder().squadId(run.getSquadId())
						.squadName(run.getSquadName()).workflowId(run.getWorkflowId()).runId(run.getRunId())
						.startedAt(run.getStartedAt()).status(temporalClient
								.getWorkflowStatus(run.getWorkflowId(), SQUAD_EXECUTION_WORKFLOW_TYPE).name())
						.build())
				.toList();
	}
}
