package com.murex.mxorbit.squadorchestrator.core.automation.facade;

import com.murex.mxorbit.squadorchestrator.core.automation.creator.AutomationCreator;
import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.deleter.AutomationDeleter;
import com.murex.mxorbit.squadorchestrator.core.automation.enrichment.AutomationEnricherService;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationDetails;
import com.murex.mxorbit.squadorchestrator.core.automation.pauser.AutomationPauser;
import com.murex.mxorbit.squadorchestrator.core.automation.provider.AutomationProvider;
import com.murex.mxorbit.squadorchestrator.core.automation.resumer.AutomationResumer;
import com.murex.mxorbit.squadorchestrator.core.automation.updater.AutomationUpdater;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Coordinates the existing automation operation services and the shared
 * {@link AutomationEnricherService} for the future REST layer. Every
 * Temporal schedule mutation happens exactly once, inside the delegated
 * operation service; this Facade only enriches the already-mutated result
 * for reads.
 */
@Service
@RequiredArgsConstructor
public class AutomationFacadeService implements AutomationFacade {

	private final AutomationCreator automationCreator;
	private final AutomationProvider automationProvider;
	private final AutomationUpdater automationUpdater;
	private final AutomationDeleter automationDeleter;
	private final AutomationPauser automationPauser;
	private final AutomationResumer automationResumer;
	private final AutomationEnricherService automationEnricherService;

	@Override
	public AutomationDetails createAutomation(CreateAutomationRequest request) {
		Automation automation = automationCreator.createAutomation(request);
		return automationEnricherService.enrich(automation);
	}

	@Override
	public List<AutomationDetails> getAutomations() {
		return automationEnricherService.enrichAll(automationProvider.getAutomations());
	}

	@Override
	public AutomationDetails getAutomationById(UUID automationId) {
		return automationEnricherService.enrich(automationProvider.getAutomationById(automationId));
	}

	@Override
	public AutomationDetails updateAutomation(UUID automationId, CreateAutomationRequest request) {
		Automation automation = automationUpdater.updateAutomation(automationId, request);
		return automationEnricherService.enrich(automation);
	}

	@Override
	public AutomationDetails pauseAutomation(UUID automationId) {
		Automation automation = automationPauser.pauseAutomation(automationId);
		return automationEnricherService.enrich(automation);
	}

	@Override
	public AutomationDetails resumeAutomation(UUID automationId) {
		Automation automation = automationResumer.resumeAutomation(automationId);
		return automationEnricherService.enrich(automation);
	}

	@Override
	public boolean deleteAutomation(UUID automationId) {
		return automationDeleter.deleteAutomation(automationId);
	}
}
