package com.murex.mxorbit.squadorchestrator.core.squad.updater;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;

import java.util.Optional;

public interface SquadUpdater {

	Optional<Squad> updateSquad(String squadId, CreateSquadRequest request);
}
