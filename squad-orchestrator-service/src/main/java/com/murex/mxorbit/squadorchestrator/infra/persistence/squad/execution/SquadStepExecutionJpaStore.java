package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.store.SquadStepExecutionStore;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.mapper.SquadStepExecutionPersistenceMapper;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
public class SquadStepExecutionJpaStore implements SquadStepExecutionStore {

	private final SquadStepExecutionRepository squadStepExecutionRepository;

	private final SquadStepExecutionPersistenceMapper squadStepExecutionPersistenceMapper;

	@Override
	public void save(SaveSquadStepExecutionRequest request) {
		String entityId = buildId(request.getSquadRunId(), request.getStepId());

		squadStepExecutionRepository.save(squadStepExecutionPersistenceMapper.toEntity(request, entityId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SquadStepExecutionData> findBySquadRunId(String squadRunId) {
		return squadStepExecutionRepository.findBySquadRunIdOrderByIdAsc(squadRunId).stream()
				.map(squadStepExecutionPersistenceMapper::toStepExecutionData).toList();
	}

	@Override
	public void deleteBySquadId(String squadId) {
		squadStepExecutionRepository.deleteBySquadId(squadId);
	}

	@Override
	public void deleteBySquadRunId(String squadRunId) {
		squadStepExecutionRepository.deleteBySquadRunId(squadRunId);
	}

	private String buildId(String squadRunId, String stepId) {
		return squadRunId + "::" + stepId;
	}
}
