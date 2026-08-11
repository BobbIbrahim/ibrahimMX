package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Defensive runtime coverage: a squad definition persisted before conditional
 * routing existed must still replay correctly (plain ALWAYS fan-out), while
 * conditional-routing guards still apply to Decision-style sources.
 */
class SquadRoutingConfigurationGuardTest {

	private final SquadRoutingConfigurationGuard guard = new SquadRoutingConfigurationGuard();

	private static SquadEdge alwaysEdge(String id, String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).priority(1).isDefault(false).build();
	}

	private static SquadEdge defaultEdge(String id, String sourceStepId, String targetStepId) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.ALWAYS).priority(999).isDefault(true).build();
	}

	private static SquadEdge whenEdge(String id, String sourceStepId, String targetStepId, String condition) {
		return SquadEdge.builder().id(id).sourceStepId(sourceStepId).targetStepId(targetStepId)
				.routingType(SquadEdgeRoutingType.WHEN).priority(1).condition(condition).isDefault(false).build();
	}

	@Test
	void normalStepWithSingleOutgoingEdgeIsAccepted() {
		List<SquadEdge> edges = List.of(alwaysEdge("e1", "s1", "s2"));

		assertDoesNotThrow(() -> guard.validate("s1", edges));
	}

	@Test
	void normalStepWithMultipleAlwaysFanOutEdgesIsAccepted() {
		List<SquadEdge> edges = List.of(alwaysEdge("e1", "s1", "s2"), alwaysEdge("e2", "s1", "s4"));

		assertDoesNotThrow(() -> guard.validate("s1", edges));
	}

	@Test
	void decisionStepWithWhenAndDefaultIsAccepted() {
		List<SquadEdge> edges = List.of(whenEdge("e1", "s1", "s2", "output.x equals \"1\""),
				defaultEdge("e2", "s1", "s3"));

		assertDoesNotThrow(() -> guard.validate("s1", edges));
	}

	@Test
	void mixingFanOutAlwaysWithConditionalRoutingIsRejected() {
		List<SquadEdge> edges = List.of(alwaysEdge("e1", "s1", "s2"),
				whenEdge("e2", "s1", "s3", "output.x equals \"1\""));

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> guard.validate("s1", edges));
		assertTrue(exception.getMessage().contains("mixes parallel ALWAYS edges with conditional routing"));
	}

	@Test
	void multipleDefaultEdgesAreRejected() {
		List<SquadEdge> edges = List.of(defaultEdge("e1", "s1", "s2"), defaultEdge("e2", "s1", "s3"));

		SquadRoutingDecisionException exception = assertThrows(SquadRoutingDecisionException.class,
				() -> guard.validate("s1", edges));
		assertTrue(exception.getMessage().contains("more than one default outgoing edge"));
	}
}
