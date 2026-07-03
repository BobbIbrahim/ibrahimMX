package com.murex.mxorbit.squadorchestrator.core.squad.store;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;

import java.util.List;

public interface SquadStore {

	Squad save(CreateSquadStoreRequest request);

	List<Squad> findAll();
}
