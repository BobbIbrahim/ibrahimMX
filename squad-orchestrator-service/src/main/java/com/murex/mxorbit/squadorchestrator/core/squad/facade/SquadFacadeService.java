package com.murex.mxorbit.squadorchestrator.core.squad.facade;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.SquadCreator;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.starter.SquadExecutionStarter;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.squad.run.provider.SquadRunProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.updater.SquadUpdater;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadFacadeService implements SquadFacade {

	private final SquadCreator squadCreator;
	private final SquadProvider squadProvider;
	private final SquadUpdater squadUpdater;
	private final SquadExecutionStarter squadExecutionStarter;
	private final SquadRunProvider squadRunProvider;

	@Override
	public Squad createSquad(CreateSquadRequest request) {
		return squadCreator.createSquad(request);
	}

	@Override
	public List<Squad> getSquads() {
		return squadProvider.getSquads();
	}

	@Override
	public Optional<Squad> getSquadById(String squadId) {
		return squadProvider.getSquadById(squadId);
	}

	@Override
	public Optional<Squad> updateSquad(String squadId, CreateSquadRequest request) {
		return squadUpdater.updateSquad(squadId, request);
	}

	@Override
	public Optional<SquadRunStartResult> startSquadRun(String squadId) {
		return squadExecutionStarter.startSquadRun(squadId);
	}

	@Override
	public List<SquadRunSummary> getSquadRuns() {
		return squadRunProvider.getSquadRuns();
	}

	@Override
	public Optional<SquadExecutionStatus> getSquadRunStatus(String squadRunId) {
		return squadRunProvider.getSquadRunStatus(squadRunId);
	}
}
