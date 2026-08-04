package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

/**
 * Activity budgets are grouped by workload shape: an agent invocation is a slow
 * remote LLM call, while lookups and persistence are fast local calls that must
 * fail quickly instead of holding workflow progress.
 */
final class SquadActivityOptions {

	private SquadActivityOptions() {
	}

	static ActivityOptions aiAgent() {
		return ActivityOptions.newBuilder()
				.setRetryOptions(retryOptions(Duration.ofSeconds(2), Duration.ofSeconds(30), 3))
				.setStartToCloseTimeout(Duration.ofMinutes(10)).setScheduleToCloseTimeout(Duration.ofMinutes(35))
				.setHeartbeatTimeout(Duration.ofMinutes(1)).build();
	}

	static ActivityOptions lookup() {
		return ActivityOptions.newBuilder()
				.setRetryOptions(retryOptions(Duration.ofSeconds(1), Duration.ofSeconds(10), 3))
				.setStartToCloseTimeout(Duration.ofSeconds(15)).setScheduleToCloseTimeout(Duration.ofMinutes(2))
				.build();
	}

	static ActivityOptions persistence() {
		return ActivityOptions.newBuilder()
				.setRetryOptions(retryOptions(Duration.ofSeconds(1), Duration.ofSeconds(10), 5))
				.setStartToCloseTimeout(Duration.ofSeconds(15)).setScheduleToCloseTimeout(Duration.ofMinutes(2))
				.build();
	}

	private static RetryOptions retryOptions(Duration initialInterval, Duration maximumInterval, int maximumAttempts) {
		return RetryOptions.newBuilder().setInitialInterval(initialInterval).setMaximumInterval(maximumInterval)
				.setBackoffCoefficient(2).setMaximumAttempts(maximumAttempts).build();
	}
}
