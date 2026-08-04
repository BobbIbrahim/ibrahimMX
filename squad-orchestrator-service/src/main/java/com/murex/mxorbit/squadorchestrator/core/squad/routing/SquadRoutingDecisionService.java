package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
	private final SquadRoutingConfigurationGuard routingConfigurationGuard;

	public SquadEdge selectNextEdge(String sourceStepId, Map<String, Object> sourceStepOutput,
			List<SquadEdge> outgoingEdges) {
		SquadRoutingDecision decision = decide(sourceStepId, sourceStepOutput, outgoingEdges);

		return collectCandidateEdges(sourceStepId, outgoingEdges).stream()
				.filter(edge -> edge.getId().equals(decision.getSelectedEdgeId())).findFirst()
				.orElseThrow(() -> new SquadRoutingDecisionException("Selected edge '" + decision.getSelectedEdgeId()
						+ "' is not an outgoing edge of step '" + sourceStepId + "'."));
	}

	public SquadRoutingDecision decide(String sourceStepId, Map<String, Object> sourceStepOutput,
			List<SquadEdge> outgoingEdges) {
		log.debug("Deciding next route. sourceStepId: {}", sourceStepId);

		if (sourceStepId == null || sourceStepId.isBlank()) {
			throw new SquadRoutingDecisionException("A source step id is required to select the next route.");
		}

		List<SquadEdge> candidateEdges = collectCandidateEdges(sourceStepId, outgoingEdges);

		if (candidateEdges.isEmpty()) {
			throw noMatchingRoute(sourceStepId, List.of());
		}

		return decideSingleUnconditionalEdge(sourceStepId, candidateEdges)
				.orElseGet(() -> decideByCondition(sourceStepId, sourceStepOutput, candidateEdges));
	}

	private List<SquadEdge> collectCandidateEdges(String sourceStepId, List<SquadEdge> outgoingEdges) {
		if (outgoingEdges == null) {
			return List.of();
		}

		return outgoingEdges.stream().filter(edge -> edge != null && sourceStepId.equals(edge.getSourceStepId()))
				.toList();
	}

	/**
	 * A lone unconditional or default edge needs no evaluation, keeping legacy
	 * linear squads untouched.
	 */
	private Optional<SquadRoutingDecision> decideSingleUnconditionalEdge(String sourceStepId,
			List<SquadEdge> candidateEdges) {
		if (candidateEdges.size() != 1) {
			return Optional.empty();
		}

		SquadEdge onlyEdge = candidateEdges.get(0);

		if (SquadEdgeRoutes.isLegacyAlways(onlyEdge)) {
			return Optional.of(successfulDecision(sourceStepId, onlyEdge, SquadRoutingDecisionOutcome.LEGACY_ALWAYS,
					LEGACY_ALWAYS_REASON, List.of(toEvaluation(onlyEdge, true, LEGACY_ALWAYS_REASON))));
		}

		if (SquadEdgeRoutes.isDefault(onlyEdge)) {
			return Optional.of(successfulDecision(sourceStepId, onlyEdge, SquadRoutingDecisionOutcome.DEFAULT_FALLBACK,
					SINGLE_DEFAULT_REASON, List.of(toEvaluation(onlyEdge, true, SINGLE_DEFAULT_REASON))));
		}

		return Optional.empty();
	}

	private SquadRoutingDecision decideByCondition(String sourceStepId, Map<String, Object> sourceStepOutput,
			List<SquadEdge> candidateEdges) {
		routingConfigurationGuard.validate(sourceStepId, candidateEdges);

		List<SquadRoutingEdgeEvaluation> checkedEdges = new ArrayList<>();

		for (SquadEdge edge : sortedConditionalEdges(candidateEdges)) {
			boolean matched = routingConditionEvaluator.evaluate(sourceStepOutput, edge.getCondition());
			checkedEdges.add(
					toEvaluation(edge, matched, matched ? CONDITION_MATCHED_REASON : CONDITION_DID_NOT_MATCH_REASON));

			if (matched) {
				return successfulDecision(sourceStepId, edge, SquadRoutingDecisionOutcome.CONDITIONAL_MATCH,
						CONDITIONAL_MATCH_REASON, checkedEdges);
			}
		}

		return candidateEdges.stream().filter(SquadEdgeRoutes::isDefault).findFirst().map(defaultEdge -> {
			checkedEdges.add(toEvaluation(defaultEdge, true, DEFAULT_SELECTED_REASON));
			return successfulDecision(sourceStepId, defaultEdge, SquadRoutingDecisionOutcome.DEFAULT_FALLBACK,
					DEFAULT_FALLBACK_REASON, checkedEdges);
		}).orElseThrow(() -> noMatchingRoute(sourceStepId, checkedEdges));
	}

	private List<SquadEdge> sortedConditionalEdges(List<SquadEdge> candidateEdges) {
		return candidateEdges.stream().filter(SquadEdgeRoutes::isConditional)
				.sorted(Comparator.comparing(SquadEdge::getPriority).thenComparing(SquadEdge::getId)).toList();
	}

	private SquadRoutingDecision successfulDecision(String sourceStepId, SquadEdge selectedEdge,
			SquadRoutingDecisionOutcome outcome, String reason, List<SquadRoutingEdgeEvaluation> checkedEdges) {
		log.debug("Selected route. sourceStepId: {}, edgeId: {}, targetStepId: {}, outcome: {}", sourceStepId,
				selectedEdge.getId(), selectedEdge.getTargetStepId(), outcome);

		return SquadRoutingDecision.builder().sourceStepId(sourceStepId).selectedEdgeId(selectedEdge.getId())
				.selectedTargetStepId(selectedEdge.getTargetStepId()).outcome(outcome).reason(reason)
				.checkedEdges(List.copyOf(checkedEdges)).build();
	}

	private SquadRoutingEdgeEvaluation toEvaluation(SquadEdge edge, boolean matched, String reason) {
		return SquadRoutingEdgeEvaluation.builder().edgeId(edge.getId()).targetStepId(edge.getTargetStepId())
				.routingType(edge.getRoutingType()).condition(edge.getCondition()).priority(edge.getPriority())
				.isDefault(SquadEdgeRoutes.isDefault(edge)).matched(matched).reason(reason).build();
	}

	private SquadRoutingDecisionException noMatchingRoute(String sourceStepId,
			List<SquadRoutingEdgeEvaluation> checkedEdges) {
		SquadRoutingDecision failedDecision = SquadRoutingDecision.builder().sourceStepId(sourceStepId)
				.outcome(SquadRoutingDecisionOutcome.NO_MATCH).reason(NO_MATCH_REASON)
				.checkedEdges(List.copyOf(checkedEdges)).build();

		return new SquadRoutingDecisionException(
				"No routing rule matched for step '" + sourceStepId + "' and no default edge exists.", failedDecision);
	}
}
