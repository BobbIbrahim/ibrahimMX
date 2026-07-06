package com.murex.mxorbit.squadorchestrator.core.squad.execution.starter;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;

import java.util.Optional;

public interface SquadExecutionStarter {

	Optional<SquadRunStartResult> startSquadRun(String squadId);
}
