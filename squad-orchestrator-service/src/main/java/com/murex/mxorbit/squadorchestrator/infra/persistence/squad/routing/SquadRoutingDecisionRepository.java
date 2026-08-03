package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingDecisionEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadRoutingDecisionRepository extends JpaRepository<SquadRoutingDecisionEntity, String> {

	List<SquadRoutingDecisionEntity> findBySquadRunIdOrderByDecisionSequenceAsc(String squadRunId);
}
