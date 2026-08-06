package com.murex.mxorbit.squadorchestrator.core.automation.resumer;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.util.UUID;

public interface AutomationResumer {

	Automation resumeAutomation(UUID automationId);
}
