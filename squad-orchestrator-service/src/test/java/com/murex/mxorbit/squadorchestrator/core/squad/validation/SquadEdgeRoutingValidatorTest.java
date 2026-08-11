package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConditionEvaluator;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regression coverage for the fan-out-vs-conditional-routing rules: a normal
 * (non-Decision) source step may fan out to any number of unconditional
 * ALWAYS edges, while conditional (WHEN/DEFAULT) validation only applies once
 * a source step actually uses conditional routing.
 */
class SquadEdgeRoutingValidatorTest {

	private final SquadEdgeRoutingValidator validator = new SquadEdgeRoutingValidator(
			new SquadRoutingConditionEvaluator());

	private static SquadEdgeRequest alwaysEdge(String sourceStepId, String targetStepId) {
		return SquadEdgeRequest.builder().sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).priority(1).isDefault(false).build();
	}

	private static SquadEdgeRequest defaultEdge(String sourceStepId, String targetStepId) {
		return SquadEdgeRequest.builder().sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).priority(999).isDefault(true).build();
	}

	private static SquadEdgeRequest whenEdge(String sourceStepId, String targetStepId, int priority,
			String condition) {
		return SquadEdgeRequest.builder().sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.WHEN).priority(priority).condition(condition).isDefault(false)
				.build();
	}

	@Test
	void normalSingleEdgeGraphIsAccepted() {
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"));

		assertDoesNotThrow(() -> validator.validate(edges));
	}

	@Test
	void normalTwoBranchFanOutGraphIsAccepted() {
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"), alwaysEdge("s1", "s4"));

		assertDoesNotThrow(() -> validator.validate(edges));
	}

	@Test
	void normalFanOutAndConvergenceGraphIsAccepted() {
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"), alwaysEdge("s1", "s4"),
				alwaysEdge("s2", "s3"), alwaysEdge("s4", "s3"));

		assertDoesNotThrow(() -> validator.validate(edges));
	}

	@Test
	void conditionalMetadataOnNormalFanOutSourceIsRejected() {
		// A plain fan-out ALWAYS edge mixed with a WHEN edge from the same source
		// injects conditional routing metadata onto what would otherwise be a
		// normal step; the source becomes an ambiguous mix of fan-out + routing.
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"),
				whenEdge("s1", "s3", 1, "output.x equals \"1\""));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> validator.validate(edges));
		assertTrue(exception.getReason().contains("mixes parallel ALWAYS edges with conditional routing"));
	}

	@Test
	void decisionStepWithValidWhenRoutesIsAccepted() {
		List<SquadEdgeRequest> edges = List.of(whenEdge("s1", "s2", 1, "output.x equals \"1\""),
				whenEdge("s1", "s3", 2, "output.x equals \"2\""));

		assertDoesNotThrow(() -> validator.validate(edges));
	}

	@Test
	void decisionStepWithOneDefaultIsAccepted() {
		List<SquadEdgeRequest> edges = List.of(whenEdge("s1", "s2", 1, "output.x equals \"1\""),
				defaultEdge("s1", "s3"));

		assertDoesNotThrow(() -> validator.validate(edges));
	}

	@Test
	void decisionStepWithMultipleDefaultRoutesIsRejected() {
		List<SquadEdgeRequest> edges = List.of(defaultEdge("s1", "s2"), defaultEdge("s1", "s3"));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> validator.validate(edges));
		assertTrue(exception.getReason().contains("more than one default outgoing edge"));
	}

	@Test
	void mixedAmbiguousDecisionRoutingIsRejected() {
		// Non-default ALWAYS ("legacy" fan-out) edge mixed with WHEN routes makes
		// the WHEN routes unreachable siblings of an unconditional branch.
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"),
				whenEdge("s1", "s3", 1, "output.x equals \"1\""), defaultEdge("s1", "s4"));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> validator.validate(edges));
		assertTrue(exception.getReason().contains("mixes parallel ALWAYS edges with conditional routing"));
	}

	@Test
	void existingLegacyNonConditionalGraphFixturesRemainAccepted() {
		// Squads authored before conditional routing existed: a linear chain of
		// single unconditional ALWAYS edges.
		List<SquadEdgeRequest> edges = List.of(alwaysEdge("s1", "s2"), alwaysEdge("s2", "s3"),
				alwaysEdge("s3", "s4"));

		assertDoesNotThrow(() -> validator.validate(edges));
	}
}
