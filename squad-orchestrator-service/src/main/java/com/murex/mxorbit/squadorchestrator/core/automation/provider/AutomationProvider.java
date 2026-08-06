package com.murex.mxorbit.squadorchestrator.core.automation.provider;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.util.List;
import java.util.UUID;

public interface AutomationProvider {

	List<Automation> getAutomations();

	Automation getAutomationById(UUID automationId);
}
