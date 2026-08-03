package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SquadRoutingDecisionServiceTest {

	private final SquadRoutingDecisionService routingDecisionService = new SquadRoutingDecisionService(
			new SquadRoutingConditionEvaluator());

	@Test
	void shouldReturnDetailedLegacyAlwaysDecision() {
		SquadEdge edge = alwaysEdge("edge-1", "step-1", "step-2");

		SquadRoutingDecision decision = routingDecisionService.decide("step-1", Map.of(), List.of(edge));

		assertSame(edge, decision.getSelectedEdge());
		assertEquals("edge-1", decision.getSelectedEdgeId());
		assertEquals("step-2", decision.getSelectedTargetStepId());
		assertEquals(SquadRoutingDecisionOutcome.LEGACY_ALWAYS, decision.getOutcome());
		assertEquals(1, decision.getCheckedEdges().size());
		assertTrue(decision.getCheckedEdges().get(0).getMatched());
	}

	@Test
	void shouldRecordFailedChecksBeforeMatchingCondition() {
		SquadEdge firstEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge secondEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals ENHANCEMENT", 20);

		SquadRoutingDecision decision = routingDecisionService.decide("step-1", output("changeType", "ENHANCEMENT"),
				List.of(secondEdge, firstEdge));

		assertEquals(SquadRoutingDecisionOutcome.CONDITIONAL_MATCH, decision.getOutcome());
		assertEquals("edge-2", decision.getSelectedEdgeId());
		assertEquals(2, decision.getCheckedEdges().size());
		assertEquals("edge-1", decision.getCheckedEdges().get(0).getEdgeId());
		assertFalse(decision.getCheckedEdges().get(0).getMatched());
		assertEquals("edge-2", decision.getCheckedEdges().get(1).getEdgeId());
		assertTrue(decision.getCheckedEdges().get(1).getMatched());
	}

	@Test
	void shouldNotRecordEdgesAfterFirstMatchAsChecked() {
		SquadEdge firstEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge secondEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals BUG_FIX", 20);

		SquadRoutingDecision decision = routingDecisionService.decide("step-1", output("changeType", "BUG_FIX"),
				List.of(secondEdge, firstEdge));

		assertEquals(1, decision.getCheckedEdges().size());
		assertEquals("edge-1", decision.getCheckedEdges().get(0).getEdgeId());
	}

	@Test
	void shouldRecordAllFailedConditionsBeforeDefaultSelection() {
		SquadEdge firstEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge secondEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals ENHANCEMENT", 20);

		SquadEdge fallbackEdge = defaultEdge("edge-3", "step-1", "step-4");

		SquadRoutingDecision decision = routingDecisionService.decide("step-1", output("changeType", "DOCUMENTATION"),
				List.of(fallbackEdge, secondEdge, firstEdge));

		assertEquals(SquadRoutingDecisionOutcome.DEFAULT_FALLBACK, decision.getOutcome());
		assertEquals("edge-3", decision.getSelectedEdgeId());
		assertEquals(3, decision.getCheckedEdges().size());
		assertEquals("edge-1", decision.getCheckedEdges().get(0).getEdgeId());
		assertFalse(decision.getCheckedEdges().get(0).getMatched());
		assertEquals("edge-2", decision.getCheckedEdges().get(1).getEdgeId());
		assertFalse(decision.getCheckedEdges().get(1).getMatched());
		assertEquals("edge-3", decision.getCheckedEdges().get(2).getEdgeId());
		assertTrue(decision.getCheckedEdges().get(2).getMatched());
	}

	@Test
	void shouldAttachFailedDecisionToNoMatchException() {
		SquadEdge edge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.decide("step-1", output("changeType", "ENHANCEMENT"), List.of(edge)));

		SquadRoutingDecision decision = exception.getDecision();

		assertEquals("No routing rule matched for step 'step-1' and no default edge exists.", exception.getMessage());
		assertEquals(SquadRoutingDecisionOutcome.NO_MATCH, decision.getOutcome());
		assertEquals(null, decision.getSelectedEdgeId());
		assertEquals(null, decision.getSelectedTargetStepId());
		assertEquals(1, decision.getCheckedEdges().size());
		assertEquals("edge-1", decision.getCheckedEdges().get(0).getEdgeId());
		assertFalse(decision.getCheckedEdges().get(0).getMatched());
	}

	@Test
	void shouldSelectSingleLegacyAlwaysEdge() {
		SquadEdge edge = alwaysEdge("edge-1", "step-1", "step-2");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", Map.of(), List.of(edge));

		assertSame(edge, selectedEdge);
	}

	@Test
	void shouldSelectSingleDefaultEdge() {
		SquadEdge edge = defaultEdge("edge-1", "step-1", "step-2");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", Map.of(), List.of(edge));

		assertSame(edge, selectedEdge);
	}

	@Test
	void shouldSelectFirstMatchingConditionalEdgeByPriority() {
		SquadEdge lowerPriorityEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge higherPriorityEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType in [BUG_FIX, HOTFIX]",
				20);

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", output("changeType", "BUG_FIX"),
				List.of(higherPriorityEdge, lowerPriorityEdge));

		assertSame(lowerPriorityEdge, selectedEdge);
	}

	@Test
	void shouldSelectLaterConditionalEdgeWhenEarlierEdgeDoesNotMatch() {
		SquadEdge firstEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge secondEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals ENHANCEMENT", 20);

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", output("changeType", "ENHANCEMENT"),
				List.of(secondEdge, firstEdge));

		assertSame(secondEdge, selectedEdge);
	}

	@Test
	void shouldSelectDefaultWhenNoConditionalEdgeMatches() {
		SquadEdge bugFixEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge enhancementEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals ENHANCEMENT", 20);

		SquadEdge fallbackEdge = defaultEdge("edge-3", "step-1", "step-4");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", output("changeType", "DOCUMENTATION"),
				List.of(fallbackEdge, enhancementEdge, bugFixEdge));

		assertSame(fallbackEdge, selectedEdge);
	}

	@Test
	void shouldPreferMatchingConditionalEdgeOverDefaultEdge() {
		SquadEdge bugFixEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge fallbackEdge = defaultEdge("edge-2", "step-1", "step-3");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", output("changeType", "BUG_FIX"),
				List.of(fallbackEdge, bugFixEdge));

		assertSame(bugFixEdge, selectedEdge);
	}

	@Test
	void shouldFailWhenNoConditionalEdgeMatchesAndNoDefaultExists() {
		SquadEdge edge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge("step-1", output("changeType", "ENHANCEMENT"),
						List.of(edge)));

		assertEquals("No routing rule matched for step 'step-1' and no default edge exists.", exception.getMessage());
	}

	@Test
	void shouldFailWhenOutgoingEdgeListIsEmpty() {
		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge("step-1", Map.of(), List.of()));

		assertEquals("No routing rule matched for step 'step-1' and no default edge exists.", exception.getMessage());
	}

	@Test
	void shouldFailWhenOutgoingEdgeListIsNull() {
		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge("step-1", Map.of(), null));

		assertEquals("No routing rule matched for step 'step-1' and no default edge exists.", exception.getMessage());
	}

	@Test
	void shouldIgnoreEdgesFromAnotherSourceStep() {
		SquadEdge unrelatedEdge = alwaysEdge("edge-1", "other-step", "step-2");

		SquadEdge expectedEdge = alwaysEdge("edge-2", "step-1", "step-3");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", Map.of(),
				List.of(unrelatedEdge, expectedEdge));

		assertSame(expectedEdge, selectedEdge);
	}

	@Test
	void shouldReturnSameResultRegardlessOfInputEdgeOrder() {
		SquadEdge firstEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge secondEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals BUG_FIX", 20);

		SquadEdge selectionFromFirstOrder = routingDecisionService.selectNextEdge("step-1",
				output("changeType", "BUG_FIX"), List.of(firstEdge, secondEdge));

		SquadEdge selectionFromSecondOrder = routingDecisionService.selectNextEdge("step-1",
				output("changeType", "BUG_FIX"), List.of(secondEdge, firstEdge));

		assertSame(firstEdge, selectionFromFirstOrder);
		assertSame(firstEdge, selectionFromSecondOrder);
	}

	@Test
	void shouldSelectConditionUsingNestedOutputField() {
		Map<String, Object> classification = new LinkedHashMap<>();
		classification.put("changeType", "BUG_FIX");

		Map<String, Object> sourceOutput = new LinkedHashMap<>();
		sourceOutput.put("classification", classification);

		SquadEdge edge = whenEdge("edge-1", "step-1", "step-2", "output.classification.changeType equals BUG_FIX", 10);

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", sourceOutput, List.of(edge));

		assertSame(edge, selectedEdge);
	}

	@Test
	void shouldUseDefaultWhenSourceOutputIsNull() {
		SquadEdge conditionalEdge = whenEdge("edge-1", "step-1", "step-2", "output.changeType equals BUG_FIX", 10);

		SquadEdge fallbackEdge = defaultEdge("edge-2", "step-1", "step-3");

		SquadEdge selectedEdge = routingDecisionService.selectNextEdge("step-1", null,
				List.of(conditionalEdge, fallbackEdge));

		assertSame(fallbackEdge, selectedEdge);
	}

	@Test
	void shouldRejectBlankSourceStepId() {
		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge(" ", Map.of(), List.of()));

		assertEquals("A source step id is required to select the next route.", exception.getMessage());
	}

	@Test
	void shouldRejectMultipleDefaultEdgesAtRuntime() {
		SquadEdge firstDefault = defaultEdge("edge-1", "step-1", "step-2");

		SquadEdge secondDefault = defaultEdge("edge-2", "step-1", "step-3");

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge("step-1", Map.of(), List.of(firstDefault, secondDefault)));

		assertEquals("Step 'step-1' has more than one default outgoing edge.", exception.getMessage());
	}

	@Test
	void shouldRejectNonDefaultAlwaysEdgeMixedWithOtherEdgesAtRuntime() {
		SquadEdge alwaysEdge = alwaysEdge("edge-1", "step-1", "step-2");

		SquadEdge conditionalEdge = whenEdge("edge-2", "step-1", "step-3", "output.changeType equals BUG_FIX", 10);

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> routingDecisionService.selectNextEdge("step-1", output("changeType", "BUG_FIX"),
						List.of(alwaysEdge, conditionalEdge)));

		assertEquals("Step 'step-1' has a non-default ALWAYS edge together with other outgoing edges.",
				exception.getMessage());
	}

	private static SquadEdge alwaysEdge(String id, String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).condition(null).priority(100).isDefault(false).build();
	}

	private static SquadEdge defaultEdge(String id, String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).condition(null).priority(100).isDefault(true).build();
	}

	private static SquadEdge whenEdge(String id, String sourceStepId, String targetStepId, String condition,
			Integer priority) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.WHEN).condition(condition).priority(priority).isDefault(false)
				.build();
	}

	private static Map<String, Object> output(String key, Object value) {
		Map<String, Object> output = new LinkedHashMap<>();
		output.put(key, value);
		return output;
	}
}
