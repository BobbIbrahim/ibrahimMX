package com.murex.mxorbit.squadorchestrator.core.squad.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;

import java.util.List;
import java.util.Optional;

public interface SquadProvider {

	List<Squad> getSquads();

	Optional<Squad> getSquadById(String squadId);
}
