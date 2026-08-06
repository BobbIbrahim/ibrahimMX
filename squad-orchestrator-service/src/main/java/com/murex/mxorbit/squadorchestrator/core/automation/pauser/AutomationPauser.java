package com.murex.mxorbit.squadorchestrator.core.automation.pauser;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.util.UUID;

public interface AutomationPauser {

	Automation pauseAutomation(UUID automationId);
}
