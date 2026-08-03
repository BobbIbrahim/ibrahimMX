package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadRoutingDecisionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingEdgeEvaluation;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingDecisionEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingEdgeEvaluationEntity;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
public class SquadRoutingDecisionJpaStore {

	private final SquadRoutingDecisionRepository squadRoutingDecisionRepository;
	private final SquadRoutingEdgeEvaluationRepository squadRoutingEdgeEvaluationRepository;

	public void save(SaveSquadRoutingDecisionRequest request) {
		String decisionId = buildDecisionId(request.getSquadRunId(), request.getDecisionSequence());

		squadRoutingDecisionRepository.save(toDecisionEntity(request, decisionId));

		List<SquadRoutingEdgeEvaluationEntity> evaluationEntities = new ArrayList<>();
		List<SquadRoutingEdgeEvaluation> checkedEdges = request.getDecision().getCheckedEdges();

		for (int evaluationOrder = 0; evaluationOrder < checkedEdges.size(); evaluationOrder++) {
			SquadRoutingEdgeEvaluation evaluation = checkedEdges.get(evaluationOrder);
			evaluationEntities.add(toEvaluationEntity(decisionId, evaluationOrder, evaluation));
		}

		if (!evaluationEntities.isEmpty()) {
			squadRoutingEdgeEvaluationRepository.saveAll(evaluationEntities);
		}
	}

	@Transactional(readOnly = true)
	public List<SquadRoutingDecision> findBySquadRunId(String squadRunId) {
		return squadRoutingDecisionRepository.findBySquadRunIdOrderByDecisionSequenceAsc(squadRunId).stream()
				.map(this::toRoutingDecision).toList();
	}

	private SquadRoutingDecisionEntity toDecisionEntity(SaveSquadRoutingDecisionRequest request, String decisionId) {
		SquadRoutingDecision decision = request.getDecision();

		return SquadRoutingDecisionEntity.builder().id(decisionId).squadRunId(request.getSquadRunId())
				.squadId(request.getSquadId()).decisionSequence(request.getDecisionSequence())
				.sourceStepId(decision.getSourceStepId()).selectedEdgeId(decision.getSelectedEdgeId())
				.selectedTargetStepId(decision.getSelectedTargetStepId()).outcome(decision.getOutcome())
				.reason(decision.getReason()).build();
	}

	private SquadRoutingEdgeEvaluationEntity toEvaluationEntity(String decisionId, int evaluationOrder,
			SquadRoutingEdgeEvaluation evaluation) {
		return SquadRoutingEdgeEvaluationEntity.builder().id(buildEvaluationId(decisionId, evaluationOrder))
				.routingDecisionId(decisionId).evaluationOrder(evaluationOrder).edgeId(evaluation.getEdgeId())
				.targetStepId(evaluation.getTargetStepId()).routingType(evaluation.getRoutingType())
				.condition(evaluation.getCondition()).priority(evaluation.getPriority())
				.isDefault(Boolean.TRUE.equals(evaluation.getIsDefault()))
				.matched(Boolean.TRUE.equals(evaluation.getMatched())).reason(evaluation.getReason()).build();
	}

	private SquadRoutingDecision toRoutingDecision(SquadRoutingDecisionEntity decisionEntity) {
		List<SquadRoutingEdgeEvaluation> checkedEdges = squadRoutingEdgeEvaluationRepository
				.findByRoutingDecisionIdOrderByEvaluationOrderAsc(decisionEntity.getId()).stream()
				.map(this::toRoutingEdgeEvaluation).toList();

		return SquadRoutingDecision.builder().sourceStepId(decisionEntity.getSourceStepId())
				.selectedEdgeId(decisionEntity.getSelectedEdgeId())
				.selectedTargetStepId(decisionEntity.getSelectedTargetStepId()).outcome(decisionEntity.getOutcome())
				.reason(decisionEntity.getReason()).checkedEdges(new ArrayList<>(checkedEdges)).selectedEdge(null)
				.build();
	}

	private SquadRoutingEdgeEvaluation toRoutingEdgeEvaluation(SquadRoutingEdgeEvaluationEntity entity) {
		return SquadRoutingEdgeEvaluation.builder().edgeId(entity.getEdgeId()).targetStepId(entity.getTargetStepId())
				.routingType(entity.getRoutingType()).condition(entity.getCondition()).priority(entity.getPriority())
				.isDefault(entity.getIsDefault()).matched(entity.getMatched()).reason(entity.getReason()).build();
	}

	private String buildDecisionId(String squadRunId, Integer decisionSequence) {
		return squadRunId + "::routing::" + decisionSequence;
	}

	private String buildEvaluationId(String decisionId, int evaluationOrder) {
		return decisionId + "::evaluation::" + evaluationOrder;
	}
}
