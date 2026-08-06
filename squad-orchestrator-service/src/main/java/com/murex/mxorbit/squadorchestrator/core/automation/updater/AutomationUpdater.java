package com.murex.mxorbit.squadorchestrator.core.automation.updater;

import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.util.UUID;

public interface AutomationUpdater {

	Automation updateAutomation(UUID automationId, CreateAutomationRequest request);
}
