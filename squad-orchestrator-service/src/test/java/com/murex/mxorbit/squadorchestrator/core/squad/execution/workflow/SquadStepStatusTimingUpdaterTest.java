package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SquadStepStatusTimingUpdaterTest {

	@Test
	void shouldPopulateStartedAtWhenStepBecomesRunning() {
		Instant now = Instant.parse("2026-07-23T10:00:00Z");
		SquadStepStatus stepStatus = baseStepStatus();

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, SquadStepExecutionStatus.RUNNING, now);

		assertEquals(now, stepStatus.getStartedAt());
	}

	@Test
	void shouldNotOverwriteStartedAtWhenAlreadyPresent() {
		Instant originalStartedAt = Instant.parse("2026-07-23T09:59:00Z");
		Instant newNow = Instant.parse("2026-07-23T10:00:00Z");
		SquadStepStatus stepStatus = baseStepStatus();
		stepStatus.setStartedAt(originalStartedAt);

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, SquadStepExecutionStatus.RUNNING, newNow);

		assertEquals(originalStartedAt, stepStatus.getStartedAt());
	}

	@Test
	void shouldPopulateCompletedAtAndDurationWhenStepCompletes() {
		Instant startedAt = Instant.parse("2026-07-23T10:00:00Z");
		Instant completedAt = Instant.parse("2026-07-23T10:00:05Z");
		SquadStepStatus stepStatus = baseStepStatus();
		stepStatus.setStartedAt(startedAt);

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, SquadStepExecutionStatus.COMPLETED, completedAt);

		assertEquals(completedAt, stepStatus.getCompletedAt());
		assertNotNull(stepStatus.getDurationMs());
		assertEquals(5000L, stepStatus.getDurationMs());
	}

	@Test
	void shouldPopulateCompletedAtAndDurationWhenStepFails() {
		Instant startedAt = Instant.parse("2026-07-23T10:00:00Z");
		Instant failedAt = Instant.parse("2026-07-23T10:00:03Z");
		SquadStepStatus stepStatus = baseStepStatus();
		stepStatus.setStartedAt(startedAt);

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, SquadStepExecutionStatus.FAILED, failedAt);

		assertEquals(failedAt, stepStatus.getCompletedAt());
		assertNotNull(stepStatus.getDurationMs());
		assertEquals(3000L, stepStatus.getDurationMs());
	}

	@Test
	void shouldPopulateCompletedAtWhenStepIsSkipped() {
		Instant skippedAt = Instant.parse("2026-07-23T10:00:03Z");
		SquadStepStatus stepStatus = baseStepStatus();

		SquadStepStatusTimingUpdater.updateTiming(stepStatus, SquadStepExecutionStatus.SKIPPED, skippedAt);

		assertEquals(skippedAt, stepStatus.getCompletedAt());
	}

	private SquadStepStatus baseStepStatus() {
		return SquadStepStatus.builder().stepId("step-1").stepName("Step 1").status(SquadStepExecutionStatus.PENDING)
				.build();
	}
}
