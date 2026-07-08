package com.murex.mxorbit.squadorchestrator.core.squad.run.creator;

import com.murex.mxorbit.squadorchestrator.core.squad.run.store.SquadRunStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunCreatorService implements SquadRunCreator {

	private final SquadRunStore squadRunStore;

	@Override
	public void createSquadRun(String squadId, String workflowId, String runId) {
		log.debug("Creating squad run record for squadId: {}, workflowId: {}, runId: {}", squadId, workflowId, runId);
		squadRunStore.save(squadId, workflowId, runId);
		log.info("Squad run record created for squadId: {}, workflowId: {}, runId: {}", squadId, workflowId, runId);
	}
}
