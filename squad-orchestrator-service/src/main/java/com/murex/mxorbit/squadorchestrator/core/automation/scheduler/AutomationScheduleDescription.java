package com.murex.mxorbit.squadorchestrator.core.automation.scheduler;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

/**
 * Minimal, Temporal-agnostic view of a schedule's current state. Keeps
 * {@link io.temporal.client.schedules.ScheduleDescription} internals out of
 * layers above the scheduler.
 */
@Value
@Builder
public class AutomationScheduleDescription {

	boolean paused;

	Instant nextRunAt;

	String lastRunId;
}
