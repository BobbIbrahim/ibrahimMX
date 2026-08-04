package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingEdgeEvaluationEntity;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadRoutingEdgeEvaluationRepository extends JpaRepository<SquadRoutingEdgeEvaluationEntity, String> {

	List<SquadRoutingEdgeEvaluationEntity> findByRoutingDecisionIdInOrderByRoutingDecisionIdAscEvaluationOrderAsc(
			Collection<String> routingDecisionIds);
}
