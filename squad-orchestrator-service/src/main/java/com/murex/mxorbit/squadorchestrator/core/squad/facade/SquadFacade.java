package com.murex.mxorbit.squadorchestrator.core.squad.facade;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.run.model.SquadRunSummary;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SquadFacade {

	Squad createSquad(CreateSquadRequest request);

	List<Squad> getSquads();

	Optional<Squad> getSquadById(String squadId);

	Optional<Squad> updateSquad(String squadId, CreateSquadRequest request);

	boolean deleteSquad(String squadId);

	Optional<SquadRunStartResult> startSquadRun(String squadId, Map<String, Object> initialInput);

	List<SquadRunSummary> getSquadRuns();

	Optional<SquadExecutionStatus> getSquadRunStatus(String squadRunId);

	void cancelSquadRun(String squadRunId);

	boolean deleteSquadRun(String squadRunId);
}
