package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SquadRunProviderServiceTest {

	@Test
	void shouldBackfillTimingFieldsFromExecutionDataWhenMissing() {
		Instant startedAt = Instant.parse("2026-07-23T13:11:50Z");
		Instant completedAt = Instant.parse("2026-07-23T13:11:53Z");

		SquadStepStatus step = SquadStepStatus.builder().stepId("step-1").stepName("Step 1")
				.status(SquadStepExecutionStatus.COMPLETED).build();

		SquadStepExecutionData executionData = SquadStepExecutionData.builder().stepId("step-1").stepName("Step 1")
				.startedAt(startedAt).completedAt(completedAt).durationMs(3000L).input(Map.of("requirements", "v"))
				.output(Map.of("result", "ok")).build();

		SquadRunProviderService.applyExecutionData(step, executionData);

		assertEquals(startedAt, step.getStartedAt());
		assertEquals(completedAt, step.getCompletedAt());
		assertEquals(3000L, step.getDurationMs());
		assertEquals(Map.of("requirements", "v"), step.getInput());
		assertEquals(Map.of("result", "ok"), step.getOutput());
	}

	@Test
	void shouldReturnFinalStepInputAndOutputFromPersistedExecutionData() {
		SquadStepStatus finalStep = SquadStepStatus.builder().stepId("step-3").stepName("New Step 3")
				.status(SquadStepExecutionStatus.COMPLETED).build();

		SquadStepExecutionData persistedFinalStepExecution = SquadStepExecutionData.builder().stepId("step-3")
				.stepName("New Step 3").input(Map.of("requirements", "from-step-2"))
				.output(Map.of("message", "final-result")).build();

		SquadRunProviderService.applyExecutionData(finalStep, persistedFinalStepExecution);

		assertEquals(Map.of("requirements", "from-step-2"), finalStep.getInput());
		assertEquals(Map.of("message", "final-result"), finalStep.getOutput());
	}
}
