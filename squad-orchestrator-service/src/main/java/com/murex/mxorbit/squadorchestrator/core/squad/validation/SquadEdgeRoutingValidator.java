package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingConditionEvaluator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SquadEdgeRoutingValidator {

	private final SquadRoutingConditionEvaluator routingConditionEvaluator;

	public void validate(List<SquadEdgeRequest> edges) {
		Map<String, List<SquadEdgeRequest>> edgesBySourceStepId = new LinkedHashMap<>();

		for (SquadEdgeRequest edge : edges) {
			validateEdgeRoutingProperties(edge);
			edgesBySourceStepId.computeIfAbsent(edge.getSourceStepId(), key -> new ArrayList<>()).add(edge);
		}

		edgesBySourceStepId.forEach(this::validateOutgoingEdgeRouting);
	}

	private void validateEdgeRoutingProperties(SquadEdgeRequest edge) {
		SquadEdgeRoutingType routingType = edge.getRoutingType();
		if (routingType == null) {
			throw badRequest(describeEdge(edge) + " must have a routing type.");
		}

		Integer priority = edge.getPriority();
		if (priority == null) {
			throw badRequest(describeEdge(edge) + " must have a priority.");
		}

		if (priority < 0) {
			throw badRequest(describeEdge(edge) + " must have a nonnegative priority.");
		}

		boolean hasCondition = edge.getCondition() != null && !edge.getCondition().isBlank();

		if (routingType == SquadEdgeRoutingType.WHEN) {
			if (!hasCondition) {
				throw badRequest(describeEdge(edge) + " uses routing type WHEN but has no condition.");
			}

			validateRoutingCondition(edge);
		}

		if (routingType == SquadEdgeRoutingType.ALWAYS && hasCondition) {
			throw badRequest(describeEdge(edge) + " uses routing type ALWAYS and must not define a condition.");
		}

		if (Boolean.TRUE.equals(edge.getIsDefault()) && routingType != SquadEdgeRoutingType.ALWAYS) {
			throw badRequest(describeEdge(edge) + " is a default edge and must use routing type ALWAYS.");
		}
	}

	private void validateRoutingCondition(SquadEdgeRequest edge) {
		try {
			routingConditionEvaluator.validate(edge.getCondition());
		} catch (IllegalArgumentException exception) {
			throw badRequest(describeEdge(edge) + " has an invalid routing condition: " + exception.getMessage());
		}
	}

	private void validateOutgoingEdgeRouting(String sourceStepId, List<SquadEdgeRequest> outgoingEdges) {
		long defaultEdgeCount = outgoingEdges.stream().filter(edge -> Boolean.TRUE.equals(edge.getIsDefault())).count();

		if (defaultEdgeCount > 1) {
			throw badRequest("Source step '" + sourceStepId + "' has more than one default outgoing edge.");
		}

		if (outgoingEdges.size() > 1) {
			boolean hasParallelEdge = outgoingEdges.stream().anyMatch(SquadEdgeRoutingValidator::isParallelEdge);
			boolean hasRoutedEdge = outgoingEdges.stream().anyMatch(edge -> !isParallelEdge(edge));

			if (hasParallelEdge && hasRoutedEdge) {
				throw badRequest("Source step '" + sourceStepId
						+ "' mixes parallel ALWAYS edges with conditional routing. Use either only "
						+ "ALWAYS edges to fan out, or WHEN edges with at most one default edge.");
			}
		}

		Set<Integer> whenPriorities = new HashSet<>();
		for (SquadEdgeRequest edge : outgoingEdges) {
			if (edge.getRoutingType() == SquadEdgeRoutingType.WHEN && !whenPriorities.add(edge.getPriority())) {
				throw badRequest("Source step '" + sourceStepId + "' has more than one WHEN edge with priority "
						+ edge.getPriority() + ".");
			}
		}
	}

	/**
	 * A non-default ALWAYS edge is always traversed, so siblings fan out in
	 * parallel.
	 */
	private static boolean isParallelEdge(SquadEdgeRequest edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && !Boolean.TRUE.equals(edge.getIsDefault());
	}

	private String describeEdge(SquadEdgeRequest edge) {
		return "Connection from step '" + edge.getSourceStepId() + "' to step '" + edge.getTargetStepId() + "'";
	}

	private ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
