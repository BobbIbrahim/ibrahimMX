package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadRoutingDecisionService {

	private static final String LEGACY_ALWAYS_REASON = "Unconditional legacy edge selected directly.";
	private static final String SINGLE_DEFAULT_REASON = "Single default edge selected directly.";
	private static final String CONDITION_MATCHED_REASON = "Condition matched.";
	private static final String CONDITION_DID_NOT_MATCH_REASON = "Condition did not match.";
	private static final String DEFAULT_SELECTED_REASON = "Default edge selected after no condition matched.";
	private static final String CONDITIONAL_MATCH_REASON = "First matching conditional edge selected.";
	private static final String DEFAULT_FALLBACK_REASON = "No conditional edge matched; default edge selected.";
	private static final String NO_MATCH_REASON = "No conditional edge matched and no default edge exists.";

	private final SquadRoutingConditionEvaluator routingConditionEvaluator;

	public SquadEdge selectNextEdge(String sourceStepId, Map<String, Object> sourceStepOutput,
			List<SquadEdge> outgoingEdges) {
		return decide(sourceStepId, sourceStepOutput, outgoingEdges).getSelectedEdge();
	}

	public SquadRoutingDecision decide(String sourceStepId, Map<String, Object> sourceStepOutput,
			List<SquadEdge> outgoingEdges) {
		if (sourceStepId == null || sourceStepId.isBlank()) {
			throw new SquadRoutingDecisionException("A source step id is required to select the next route.");
		}

		List<SquadEdge> sourceOutgoingEdges = outgoingEdges == null
				? List.of()
				: outgoingEdges.stream().filter(edge -> edge != null && sourceStepId.equals(edge.getSourceStepId()))
						.toList();

		if (sourceOutgoingEdges.isEmpty()) {
			throw noMatchingRoute(sourceStepId, List.of());
		}

		if (sourceOutgoingEdges.size() == 1) {
			SquadEdge onlyEdge = sourceOutgoingEdges.get(0);

			if (isLegacyAlwaysEdge(onlyEdge)) {
				return successfulDecision(sourceStepId, onlyEdge, SquadRoutingDecisionOutcome.LEGACY_ALWAYS,
						LEGACY_ALWAYS_REASON, List.of(toEvaluation(onlyEdge, true, LEGACY_ALWAYS_REASON)));
			}

			if (isDefaultEdge(onlyEdge)) {
				return successfulDecision(sourceStepId, onlyEdge, SquadRoutingDecisionOutcome.DEFAULT_FALLBACK,
						SINGLE_DEFAULT_REASON, List.of(toEvaluation(onlyEdge, true, SINGLE_DEFAULT_REASON)));
			}
		}

		validateRuntimeConfiguration(sourceStepId, sourceOutgoingEdges);

		List<SquadEdge> conditionalEdges = new ArrayList<>();
		SquadEdge defaultEdge = null;

		for (SquadEdge edge : sourceOutgoingEdges) {
			if (isDefaultEdge(edge)) {
				defaultEdge = edge;
			} else if (edge.getRoutingType() == SquadEdgeRoutingType.WHEN) {
				conditionalEdges.add(edge);
			}
		}

		conditionalEdges.sort(Comparator.comparing(SquadEdge::getPriority).thenComparing(SquadEdge::getId));

		List<SquadRoutingEdgeEvaluation> checkedEdges = new ArrayList<>();

		for (SquadEdge edge : conditionalEdges) {
			boolean matched = routingConditionEvaluator.evaluate(sourceStepOutput, edge.getCondition());

			checkedEdges.add(
					toEvaluation(edge, matched, matched ? CONDITION_MATCHED_REASON : CONDITION_DID_NOT_MATCH_REASON));

			if (matched) {
				return successfulDecision(sourceStepId, edge, SquadRoutingDecisionOutcome.CONDITIONAL_MATCH,
						CONDITIONAL_MATCH_REASON, checkedEdges);
			}
		}

		if (defaultEdge != null) {
			checkedEdges.add(toEvaluation(defaultEdge, true, DEFAULT_SELECTED_REASON));

			return successfulDecision(sourceStepId, defaultEdge, SquadRoutingDecisionOutcome.DEFAULT_FALLBACK,
					DEFAULT_FALLBACK_REASON, checkedEdges);
		}

		throw noMatchingRoute(sourceStepId, checkedEdges);
	}

	private SquadRoutingDecision successfulDecision(String sourceStepId, SquadEdge selectedEdge,
			SquadRoutingDecisionOutcome outcome, String reason, List<SquadRoutingEdgeEvaluation> checkedEdges) {
		return SquadRoutingDecision.builder().sourceStepId(sourceStepId).selectedEdgeId(selectedEdge.getId())
				.selectedTargetStepId(selectedEdge.getTargetStepId()).outcome(outcome).reason(reason)
				.checkedEdges(List.copyOf(checkedEdges)).selectedEdge(selectedEdge).build();
	}

	private SquadRoutingEdgeEvaluation toEvaluation(SquadEdge edge, boolean matched, String reason) {
		return SquadRoutingEdgeEvaluation.builder().edgeId(edge.getId()).targetStepId(edge.getTargetStepId())
				.routingType(edge.getRoutingType()).condition(edge.getCondition()).priority(edge.getPriority())
				.isDefault(Boolean.TRUE.equals(edge.getIsDefault())).matched(matched).reason(reason).build();
	}

	private void validateRuntimeConfiguration(String sourceStepId, List<SquadEdge> outgoingEdges) {
		long defaultEdgeCount = outgoingEdges.stream().filter(this::isDefaultEdge).count();

		if (defaultEdgeCount > 1) {
			throw new SquadRoutingDecisionException(
					"Step '" + sourceStepId + "' has more than one default outgoing edge.");
		}

		if (outgoingEdges.size() > 1) {
			boolean containsNonDefaultAlwaysEdge = outgoingEdges.stream().anyMatch(this::isLegacyAlwaysEdge);

			if (containsNonDefaultAlwaysEdge) {
				throw new SquadRoutingDecisionException("Step '" + sourceStepId
						+ "' has a non-default ALWAYS edge together with other outgoing edges.");
			}
		}

		for (SquadEdge edge : outgoingEdges) {
			if (edge.getRoutingType() == null) {
				throw new SquadRoutingDecisionException(
						"Edge '" + edge.getId() + "' from step '" + sourceStepId + "' has no routing type.");
			}

			if (edge.getPriority() == null) {
				throw new SquadRoutingDecisionException(
						"Edge '" + edge.getId() + "' from step '" + sourceStepId + "' has no routing priority.");
			}

			if (edge.getRoutingType() == SquadEdgeRoutingType.WHEN
					&& (edge.getCondition() == null || edge.getCondition().isBlank())) {
				throw new SquadRoutingDecisionException("Edge '" + edge.getId() + "' from step '" + sourceStepId
						+ "' uses routing type WHEN but has no condition.");
			}
		}
	}

	private boolean isLegacyAlwaysEdge(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && !Boolean.TRUE.equals(edge.getIsDefault());
	}

	private boolean isDefaultEdge(SquadEdge edge) {
		return edge.getRoutingType() == SquadEdgeRoutingType.ALWAYS && Boolean.TRUE.equals(edge.getIsDefault());
	}

	private SquadRoutingDecisionException noMatchingRoute(String sourceStepId,
			List<SquadRoutingEdgeEvaluation> checkedEdges) {
		String message = "No routing rule matched for step '" + sourceStepId + "' and no default edge exists.";

		SquadRoutingDecision failedDecision = SquadRoutingDecision.builder().sourceStepId(sourceStepId)
				.selectedEdgeId(null).selectedTargetStepId(null).outcome(SquadRoutingDecisionOutcome.NO_MATCH)
				.reason(NO_MATCH_REASON).checkedEdges(List.copyOf(checkedEdges)).selectedEdge(null).build();

		return new SquadRoutingDecisionException(message, failedDecision);
	}
}
