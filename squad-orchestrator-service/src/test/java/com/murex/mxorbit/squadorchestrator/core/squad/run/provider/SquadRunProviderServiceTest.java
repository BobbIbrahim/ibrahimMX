package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void shouldIdentifyTerminalStepAsTheStepThatIsNotAnEdgeSource() {
		Squad squad = squad(List.of(step("step-1"), step("step-2"), step("step-3")),
				List.of(edge("step-1", "step-2"), edge("step-2", "step-3")));

		Optional<String> terminalStepId = SquadRunProviderService.findTerminalStepId(squad);

		assertTrue(terminalStepId.isPresent());
		assertEquals("step-3", terminalStepId.get());
	}

	@Test
	void shouldIdentifySingleStepAsTerminalWhenThereAreNoEdges() {
		Squad squad = squad(List.of(step("step-1")), List.of());

		assertEquals(Optional.of("step-1"), SquadRunProviderService.findTerminalStepId(squad));
	}

	private static Squad squad(List<SquadStep> steps, List<SquadEdge> edges) {
		return Squad.builder().id("squad-1").name("Squad").steps(steps).edges(edges)
				.createdAt(Instant.parse("2026-07-23T13:11:50Z")).updatedAt(Instant.parse("2026-07-23T13:11:50Z"))
				.build();
	}

	private static SquadStep step(String id) {
		return AiAgentStep.builder().id(id).name(id).agentKey("agent-key").build();
	}

	private static SquadEdge edge(String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(sourceStepId + "->" + targetStepId).sourceStepId(sourceStepId)
				.targetStepId(targetStepId).build();
	}
}
