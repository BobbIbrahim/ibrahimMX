package com.murex.mxorbit.squadorchestrator.core.squad.facade;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;

import java.util.List;
import java.util.Optional;

public interface SquadFacade {

	Squad createSquad(CreateSquadRequest request);

	List<Squad> getSquads();

	Optional<Squad> getSquadById(String squadId);

	Optional<Squad> updateSquad(String squadId, CreateSquadRequest request);

	Optional<SquadRunStartResult> startSquadRun(String squadId);
}
