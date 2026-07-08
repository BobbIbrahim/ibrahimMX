package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.run;

import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.run.entity.SquadRunEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SquadRunRepository extends JpaRepository<SquadRunEntity, String> {

	Optional<SquadRunEntity> findByWorkflowId(String workflowId);

	List<SquadRunEntity> findAllBySquadIdOrderByStartedAtDesc(String squadId);

	@Query(value = """
			select sr.squad_id as squadId,
			       s.name as squadName,
			       sr.workflow_id as workflowId,
			       sr.run_id as runId,
			       sr.started_at as startedAt
			from squad_runs sr
			join squad s on s.id = sr.squad_id
			order by sr.started_at desc
			""", nativeQuery = true)
	List<SquadRunWithSquadNameProjection> findAllWithSquadNameOrderByStartedAtDesc();

	interface SquadRunWithSquadNameProjection {
		String getSquadId();

		String getSquadName();

		String getWorkflowId();

		String getRunId();

		java.time.Instant getStartedAt();
	}
}
