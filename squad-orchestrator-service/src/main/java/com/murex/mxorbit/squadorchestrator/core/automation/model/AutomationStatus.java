package com.murex.mxorbit.squadorchestrator.core.automation.model;

/**
 * Runtime-only status of an automation's Temporal schedule. Never persisted;
 * always derived from {@link com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService#describeSchedule(String)}.
 */
public enum AutomationStatus {
	ACTIVE, PAUSED
}
