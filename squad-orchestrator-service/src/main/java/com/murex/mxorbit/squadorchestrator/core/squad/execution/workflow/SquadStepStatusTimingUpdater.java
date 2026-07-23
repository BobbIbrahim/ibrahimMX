package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;

import java.time.Duration;
import java.time.Instant;

final class SquadStepStatusTimingUpdater {

	private SquadStepStatusTimingUpdater() {
	}

	static void updateTiming(SquadStepStatus stepStatus, SquadStepExecutionStatus status, Instant now) {
		if (status == SquadStepExecutionStatus.RUNNING && stepStatus.getStartedAt() == null) {
			stepStatus.setStartedAt(now);
		}

		if (status == SquadStepExecutionStatus.COMPLETED || status == SquadStepExecutionStatus.FAILED) {
			stepStatus.setCompletedAt(now);

			if (stepStatus.getStartedAt() != null) {
				stepStatus.setDurationMs(Duration.between(stepStatus.getStartedAt(), now).toMillis());
			}
		}

	}
}
