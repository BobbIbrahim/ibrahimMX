package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.entity.SquadStepExecutionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SquadStepExecutionRepository extends JpaRepository<SquadStepExecutionEntity, String> {

	@Query("SELECT e FROM SquadStepExecutionEntity e WHERE e.squadRunId = :squadRunId ORDER BY e.id")
	List<SquadStepExecutionEntity> findBySquadRunId(@Param("squadRunId") String squadRunId);
}
