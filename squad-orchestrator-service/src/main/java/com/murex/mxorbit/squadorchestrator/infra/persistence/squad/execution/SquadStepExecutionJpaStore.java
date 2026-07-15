package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.entity.SquadStepExecutionEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
public class SquadStepExecutionJpaStore {

	private final SquadStepExecutionRepository squadStepExecutionRepository;

	public void save(SaveSquadStepExecutionRequest request) {
		squadStepExecutionRepository.save(toEntity(request));
	}

	public List<SquadStepExecutionData> findBySquadRunId(String squadRunId) {
		return squadStepExecutionRepository.findBySquadRunIdOrderByIdAsc(squadRunId).stream()
				.map(this::toSquadStepExecutionData).toList();
	}

	private SquadStepExecutionEntity toEntity(SaveSquadStepExecutionRequest request) {
		return SquadStepExecutionEntity.builder().id(buildId(request.getSquadRunId(), request.getStepId()))
				.squadRunId(request.getSquadRunId()).squadId(request.getSquadId()).stepId(request.getStepId())
				.stepName(request.getStepName()).status(request.getStatus()).message(request.getMessage())
				.input(copy(request.getInput())).output(copy(request.getOutput())).build();
	}

	private SquadStepExecutionData toSquadStepExecutionData(SquadStepExecutionEntity entity) {
		return SquadStepExecutionData.builder().stepId(entity.getStepId()).stepName(entity.getStepName())
				.input(entity.getInput()).output(entity.getOutput()).build();
	}

	private String buildId(String squadRunId, String stepId) {
		return squadRunId + "::" + stepId;
	}

	private Map<String, Object> copy(Map<String, Object> source) {
		return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
	}
}
