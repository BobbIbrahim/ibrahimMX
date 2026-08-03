package com.murex.mxorbit.squadorchestrator.core.squad.run.provider;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecisionOutcome;
import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
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

	@Test
	void shouldIdentifySelectedTerminalStepFromLastRoutingDecision() {
		SquadRoutingDecision firstDecision = SquadRoutingDecision.builder().sourceStepId("step-1")
				.selectedEdgeId("edge-1").selectedTargetStepId("step-2")
				.outcome(SquadRoutingDecisionOutcome.CONDITIONAL_MATCH).reason("Matched").build();

		SquadRoutingDecision secondDecision = SquadRoutingDecision.builder().sourceStepId("step-2")
				.selectedEdgeId("edge-2").selectedTargetStepId("step-final")
				.outcome(SquadRoutingDecisionOutcome.LEGACY_ALWAYS).reason("Selected").build();

		SquadExecutionStatus status = SquadExecutionStatus.builder().squadId("squad-1")
				.overallStatus(WorkflowRunStatus.COMPLETED).steps(List.of())
				.routingDecisions(List.of(firstDecision, secondDecision)).build();

		assertEquals(Optional.of("step-final"), SquadRunProviderService.findSelectedTerminalStepId(status));
	}

	@Test
	void shouldFallBackToLastCompletedStepWhenRoutingDecisionsAreEmpty() {
		SquadStepStatus firstStep = SquadStepStatus.builder().stepId("step-1").stepName("Step 1")
				.status(SquadStepExecutionStatus.COMPLETED).build();

		SquadStepStatus terminalStep = SquadStepStatus.builder().stepId("step-selected-terminal")
				.stepName("Selected terminal").status(SquadStepExecutionStatus.COMPLETED).build();

		SquadStepStatus skippedStep = SquadStepStatus.builder().stepId("step-skipped").stepName("Skipped")
				.status(SquadStepExecutionStatus.SKIPPED).build();

		SquadExecutionStatus status = SquadExecutionStatus.builder().squadId("squad-1")
				.overallStatus(WorkflowRunStatus.COMPLETED).steps(List.of(firstStep, terminalStep, skippedStep))
				.routingDecisions(List.of()).build();

		assertEquals(Optional.of("step-selected-terminal"), SquadRunProviderService.findSelectedTerminalStepId(status));
	}

}
