package com.murex.mxorbit.squadorchestrator.core.squad.routing.store;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request.SquadRoutingDecisionStoreRequest;

import java.util.List;

public interface SquadRoutingDecisionStore {

	void save(SquadRoutingDecisionStoreRequest request);

	List<SquadRoutingDecision> findBySquadRunId(String squadRunId);
}
