package com.murex.mxorbit.squadorchestrator.core.automation.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/**
 * Read-time view of a stored {@link Automation} enriched with runtime state
 * resolved from the assignee and the Temporal schedule. None of the
 * enrichment fields are persisted; they are recomputed on every read.
 */
@Value
@Builder
public class AutomationDetails {

	Automation automation;

	String assigneeName;

	AutomationStatus status;

	Instant nextRunAt;

	String lastRunId;
}
