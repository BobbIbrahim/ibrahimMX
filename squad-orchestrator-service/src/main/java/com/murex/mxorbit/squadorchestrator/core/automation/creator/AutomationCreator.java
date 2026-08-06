package com.murex.mxorbit.squadorchestrator.core.automation.creator;

import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

public interface AutomationCreator {

	Automation createAutomation(CreateAutomationRequest request);
}
