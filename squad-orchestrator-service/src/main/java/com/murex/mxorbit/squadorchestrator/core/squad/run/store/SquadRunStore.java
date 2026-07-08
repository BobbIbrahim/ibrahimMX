package com.murex.mxorbit.squadorchestrator.core.squad.run.store;

import com.murex.mxorbit.squadorchestrator.core.squad.run.model.StoredSquadRun;
import java.util.List;

public interface SquadRunStore {

	void save(String squadId, String workflowId, String runId);

	List<StoredSquadRun> findAllWithSquadNames();
}
