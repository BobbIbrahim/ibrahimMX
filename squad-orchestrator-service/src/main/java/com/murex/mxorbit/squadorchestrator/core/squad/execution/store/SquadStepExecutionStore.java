package com.murex.mxorbit.squadorchestrator.core.squad.execution.store;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;

import java.util.List;

public interface SquadStepExecutionStore {

	void save(SaveSquadStepExecutionRequest request);

	List<SquadStepExecutionData> findBySquadRunId(String squadRunId);

	void deleteBySquadId(String squadId);

	void deleteBySquadRunId(String squadRunId);
}
