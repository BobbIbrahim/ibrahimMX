package com.murex.mxorbit.squadorchestrator.core.squad.deleter;

import com.murex.mxorbit.squadorchestrator.core.automation.deleter.AutomationDeleter;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.squad.run.deleter.SquadRunDeleter;
import com.murex.mxorbit.squadorchestrator.core.squad.run.provider.SquadRunProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadDeleterService implements SquadDeleter {

	private final SquadStore squadStore;
	private final SquadRunProvider squadRunProvider;
	private final SquadRunDeleter squadRunDeleter;
	private final AutomationDeleter automationDeleter;

	@Override
	public boolean deleteSquad(String squadId) {
		log.debug("Deleting squad with id: {}", squadId);

		if (squadStore.findById(squadId).isEmpty()) {
			return false;
		}

		rejectIfRunning(squadId);

		automationDeleter.deleteByAssignee(AssigneeType.SQUAD, squadId);
		squadRunDeleter.deleteSquadRuns(squadId);

		boolean deleted = squadStore.deleteById(squadId);
		log.info("Squad deleted. squadId: {}", squadId);
		return deleted;
	}

	private void rejectIfRunning(String squadId) {
		List<String> runningRunIds = squadRunProvider.getRunningSquadRunIds(squadId);

		if (runningRunIds.isEmpty()) {
			return;
		}

		log.info("Refusing to delete squad with running runs. squadId: {}, runningRunIds: {}", squadId, runningRunIds);
		throw new ResponseStatusException(HttpStatus.CONFLICT,
				"Squad has " + runningRunIds.size() + " run(s) still in progress. Cancel them before deleting.");
	}
}
