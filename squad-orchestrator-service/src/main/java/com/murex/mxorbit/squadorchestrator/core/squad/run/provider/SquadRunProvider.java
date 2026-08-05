package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;

import java.util.List;
import java.util.Optional;

public interface SquadRunProvider {

	List<SquadRunSummary> getSquadRuns();

	List<String> getRunningSquadRunIds(String squadId);

	Optional<SquadExecutionStatus> getSquadRunStatus(String squadRunId);
}
