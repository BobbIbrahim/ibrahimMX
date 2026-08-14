package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.entity.SquadStepExecutionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadStepExecutionRepository extends JpaRepository<SquadStepExecutionEntity, String> {

	List<SquadStepExecutionEntity> findBySquadRunIdOrderByIdAsc(String squadRunId);

	void deleteBySquadId(String squadId);

	void deleteBySquadRunId(String squadRunId);
}
