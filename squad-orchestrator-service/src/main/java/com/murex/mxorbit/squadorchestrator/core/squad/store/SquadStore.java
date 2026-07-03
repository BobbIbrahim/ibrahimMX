package com.murex.mxorbit.squadorchestrator.core.squad.store;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;

import java.util.List;
import java.util.Optional;

public interface SquadStore {

	Squad save(CreateSquadStoreRequest request);

	List<Squad> findAll();

	Optional<Squad> findById(String squadId);

	Optional<Squad> update(String squadId, CreateSquadStoreRequest request);
}
