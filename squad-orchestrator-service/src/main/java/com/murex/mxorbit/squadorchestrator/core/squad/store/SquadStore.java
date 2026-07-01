package com.murex.mxorbit.squadorchestrator.core.squad.store;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;

public interface SquadStore {

	Squad save(CreateSquadStoreRequest request);
}
