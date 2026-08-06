package com.murex.mxorbit.squadorchestrator.core.automation.facade;

import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationDetails;

import java.util.List;
import java.util.UUID;

public interface AutomationFacade {

	AutomationDetails createAutomation(CreateAutomationRequest request);

	List<AutomationDetails> getAutomations();

	AutomationDetails getAutomationById(UUID automationId);

	AutomationDetails updateAutomation(UUID automationId, CreateAutomationRequest request);

	AutomationDetails pauseAutomation(UUID automationId);

	AutomationDetails resumeAutomation(UUID automationId);

	boolean deleteAutomation(UUID automationId);
}
