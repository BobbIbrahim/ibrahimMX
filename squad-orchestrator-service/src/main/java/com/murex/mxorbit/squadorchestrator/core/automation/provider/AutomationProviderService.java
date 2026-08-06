package com.murex.mxorbit.squadorchestrator.core.automation.provider;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.store.AutomationStore;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationProviderService implements AutomationProvider {

	private final AutomationStore automationStore;

	@Override
	public List<Automation> getAutomations() {
		log.debug("Getting all automations");
		return automationStore.findAll();
	}

	@Override
	public Automation getAutomationById(UUID automationId) {
		log.debug("Getting automation by id: {}", automationId);
		return automationStore.findById(automationId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation not found: " + automationId));
	}
}
