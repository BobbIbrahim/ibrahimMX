package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Defensive runtime guard: squads are validated at creation, but a run may
 * replay a definition persisted before the current rules existed.
 */
@Service
public class SquadRoutingConfigurationGuard {

	public void validate(String sourceStepId, List<SquadEdge> outgoingEdges) {
		validateSingleDefaultEdge(sourceStepId, outgoingEdges);
		validateNoMixedRouting(sourceStepId, outgoingEdges);
		outgoingEdges.forEach(edge -> validateEdge(sourceStepId, edge));
	}

	private void validateSingleDefaultEdge(String sourceStepId, List<SquadEdge> outgoingEdges) {
		if (outgoingEdges.stream().filter(SquadEdgeRoutes::isDefault).count() > 1) {
			throw new SquadRoutingDecisionException(
					"Step '" + sourceStepId + "' has more than one default outgoing edge.");
		}
	}

	private void validateNoMixedRouting(String sourceStepId, List<SquadEdge> outgoingEdges) {
		if (outgoingEdges.size() > 1 && outgoingEdges.stream().anyMatch(SquadEdgeRoutes::isLegacyAlways)
				&& !outgoingEdges.stream().allMatch(SquadEdgeRoutes::isLegacyAlways)) {
			throw new SquadRoutingDecisionException(
					"Step '" + sourceStepId + "' mixes parallel ALWAYS edges with conditional routing.");
		}
	}

	private void validateEdge(String sourceStepId, SquadEdge edge) {
		if (edge.getRoutingType() == null) {
			throw new SquadRoutingDecisionException(
					"Edge '" + edge.getId() + "' from step '" + sourceStepId + "' has no routing type.");
		}

		if (edge.getPriority() == null) {
			throw new SquadRoutingDecisionException(
					"Edge '" + edge.getId() + "' from step '" + sourceStepId + "' has no routing priority.");
		}

		if (SquadEdgeRoutes.isConditional(edge) && (edge.getCondition() == null || edge.getCondition().isBlank())) {
			throw new SquadRoutingDecisionException("Edge '" + edge.getId() + "' from step '" + sourceStepId
					+ "' uses routing type WHEN but has no condition.");
		}
	}
}
