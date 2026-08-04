package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.mapper;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingEdgeEvaluation;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request.SquadRoutingDecisionStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingDecisionEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingEdgeEvaluationEntity;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SquadRoutingPersistenceMapper {

	@Mapping(target = "id", source = "decisionId")
	@Mapping(target = "squadRunId", source = "request.squadRunId")
	@Mapping(target = "squadId", source = "request.squadId")
	@Mapping(target = "decisionSequence", source = "request.decisionSequence")
	@Mapping(target = "sourceStepId", source = "request.decision.sourceStepId")
	@Mapping(target = "selectedEdgeId", source = "request.decision.selectedEdgeId")
	@Mapping(target = "selectedTargetStepId", source = "request.decision.selectedTargetStepId")
	@Mapping(target = "outcome", source = "request.decision.outcome")
	@Mapping(target = "reason", source = "request.decision.reason")
	SquadRoutingDecisionEntity toDecisionEntity(SquadRoutingDecisionStoreRequest request, String decisionId);

	@Mapping(target = "id", source = "evaluationId")
	@Mapping(target = "routingDecisionId", source = "routingDecisionId")
	@Mapping(target = "evaluationOrder", source = "evaluationOrder")
	@Mapping(target = "edgeId", source = "evaluation.edgeId")
	@Mapping(target = "targetStepId", source = "evaluation.targetStepId")
	@Mapping(target = "routingType", source = "evaluation.routingType")
	@Mapping(target = "condition", source = "evaluation.condition")
	@Mapping(target = "priority", source = "evaluation.priority")
	@Mapping(target = "isDefault", source = "evaluation.isDefault", defaultValue = "false")
	@Mapping(target = "matched", source = "evaluation.matched", defaultValue = "false")
	@Mapping(target = "reason", source = "evaluation.reason")
	SquadRoutingEdgeEvaluationEntity toEvaluationEntity(SquadRoutingEdgeEvaluation evaluation, String evaluationId,
			String routingDecisionId, int evaluationOrder);

	SquadRoutingEdgeEvaluation toEvaluation(SquadRoutingEdgeEvaluationEntity entity);

	@Mapping(target = "sourceStepId", source = "entity.sourceStepId")
	@Mapping(target = "selectedEdgeId", source = "entity.selectedEdgeId")
	@Mapping(target = "selectedTargetStepId", source = "entity.selectedTargetStepId")
	@Mapping(target = "outcome", source = "entity.outcome")
	@Mapping(target = "reason", source = "entity.reason")
	@Mapping(target = "checkedEdges", source = "checkedEdges")
	SquadRoutingDecision toDecision(SquadRoutingDecisionEntity entity, List<SquadRoutingEdgeEvaluation> checkedEdges);
}
