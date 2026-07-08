package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.run;

import com.murex.mxorbit.squadorchestrator.core.squad.run.model.StoredSquadRun;
import com.murex.mxorbit.squadorchestrator.core.squad.run.store.SquadRunStore;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.run.entity.SquadRunEntity;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class SquadRunJpaStore implements SquadRunStore {

	private final SquadRunRepository squadRunRepository;

	@Override
	public void save(String squadId, String workflowId, String runId) {
		log.trace("Saving squad run with squadId: {}, workflowId: {}, runId: {}", squadId, workflowId, runId);

		SquadRunEntity entity = SquadRunEntity.builder().squadId(squadId).workflowId(workflowId).runId(runId)
				.startedAt(Instant.now()).build();

		squadRunRepository.save(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredSquadRun> findAllWithSquadNames() {
		log.trace("Finding all squad runs with squad names");
		return squadRunRepository.findAllWithSquadNameOrderByStartedAtDesc().stream()
				.map(run -> StoredSquadRun.builder().squadId(run.getSquadId()).squadName(run.getSquadName())
						.workflowId(run.getWorkflowId()).runId(run.getRunId()).startedAt(run.getStartedAt()).build())
				.toList();
	}
}
