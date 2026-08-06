package com.murex.mxorbit.squadorchestrator.core.automation.store;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomationStore {

	Automation save(Automation automation);

	List<Automation> findAll();

	Optional<Automation> findById(UUID id);

	Optional<Automation> update(UUID id, Automation automation);

	boolean deleteById(UUID id);
}
