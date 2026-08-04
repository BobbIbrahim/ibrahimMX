package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;

final class SquadEdgeRoutes {

	private SquadEdgeRoutes() {
	}

	/**
	 * Unconditional edge kept for linear squads authored before conditional
	 * routing.
	 */
	static boolean isLegacyAlways(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && !isDefault(edge);
	}

	static boolean isDefault(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && Boolean.TRUE.equals(edge.getIsDefault());
	}

	static boolean isConditional(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.WHEN;
	}
}
