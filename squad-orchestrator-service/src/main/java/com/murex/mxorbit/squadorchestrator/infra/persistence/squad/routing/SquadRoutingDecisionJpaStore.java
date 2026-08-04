package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingEdgeEvaluation;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.SquadRoutingDecisionStore;
import com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request.SquadRoutingDecisionStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingDecisionEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.entity.SquadRoutingEdgeEvaluationEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.routing.mapper.SquadRoutingPersistenceMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class SquadRoutingDecisionJpaStore implements SquadRoutingDecisionStore {

	private final SquadRoutingDecisionRepository squadRoutingDecisionRepository;
	private final SquadRoutingEdgeEvaluationRepository squadRoutingEdgeEvaluationRepository;
	private final SquadRoutingPersistenceMapper squadRoutingPersistenceMapper;

	@Override
	public void save(SquadRoutingDecisionStoreRequest request) {
		log.trace("Saving routing decision with request: {}", request);
		String decisionId = buildDecisionId(request.getSquadRunId(), request.getDecisionSequence());
		squadRoutingDecisionRepository.save(squadRoutingPersistenceMapper.toDecisionEntity(request, decisionId));
		squadRoutingEdgeEvaluationRepository.saveAll(toEvaluationEntities(decisionId, request));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SquadRoutingDecision> findBySquadRunId(String squadRunId) {
		log.trace("Finding routing decisions by squad run id: {}", squadRunId);
		List<SquadRoutingDecisionEntity> decisionEntities = squadRoutingDecisionRepository
				.findBySquadRunIdOrderByDecisionSequenceAsc(squadRunId);

		if (decisionEntities.isEmpty()) {
			return List.of();
		}

		Map<String, List<SquadRoutingEdgeEvaluation>> evaluationsByDecisionId = findEvaluationsByDecisionId(
				decisionEntities);

		return decisionEntities.stream().map(entity -> squadRoutingPersistenceMapper.toDecision(entity,
				evaluationsByDecisionId.getOrDefault(entity.getId(), List.of()))).toList();
	}

	private List<SquadRoutingEdgeEvaluationEntity> toEvaluationEntities(String decisionId,
			SquadRoutingDecisionStoreRequest request) {
		List<SquadRoutingEdgeEvaluation> checkedEdges = request.getDecision().getCheckedEdges();

		return IntStream.range(0, checkedEdges.size())
				.mapToObj(evaluationOrder -> squadRoutingPersistenceMapper.toEvaluationEntity(
						checkedEdges.get(evaluationOrder), buildEvaluationId(decisionId, evaluationOrder), decisionId,
						evaluationOrder))
				.toList();
	}

	/**
	 * Loaded in a single query to avoid one lookup per decision when rendering a
	 * run trace.
	 */
	private Map<String, List<SquadRoutingEdgeEvaluation>> findEvaluationsByDecisionId(
			List<SquadRoutingDecisionEntity> decisionEntities) {
		List<String> decisionIds = decisionEntities.stream().map(SquadRoutingDecisionEntity::getId).toList();

		return squadRoutingEdgeEvaluationRepository
				.findByRoutingDecisionIdInOrderByRoutingDecisionIdAscEvaluationOrderAsc(decisionIds).stream()
				.collect(Collectors.groupingBy(SquadRoutingEdgeEvaluationEntity::getRoutingDecisionId,
						LinkedHashMap::new,
						Collectors.mapping(squadRoutingPersistenceMapper::toEvaluation, Collectors.toList())));
	}

	private String buildDecisionId(String squadRunId, Integer decisionSequence) {
		return squadRunId + "::routing::" + decisionSequence;
	}

	private String buildEvaluationId(String decisionId, int evaluationOrder) {
		return decisionId + "::evaluation::" + evaluationOrder;
	}
}
