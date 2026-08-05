package com.murex.mxorbit.squadorchestrator.infra.persistence.squad;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.mapper.SquadPersistenceMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class SquadJpaStore implements SquadStore {

	private final SquadRepository squadRepository;
	private final SquadPersistenceMapper squadPersistenceMapper;

	@Override
	public Squad save(CreateSquadStoreRequest request) {
		log.trace("Saving squad with request: {}", request);
		SquadEntity entity = squadPersistenceMapper.toSquadEntity(request);
		SquadEntity saved = squadRepository.save(entity);
		return squadPersistenceMapper.toSquad(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Squad> findAll() {
		log.trace("Finding all squads");
		return squadRepository.findAll().stream().map(squadPersistenceMapper::toSquad).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Squad> findById(String squadId) {
		log.trace("Finding squad by id: {}", squadId);
		return squadRepository.findById(squadId).map(squadPersistenceMapper::toSquad);
	}

	@Override
	public Optional<Squad> update(String squadId, CreateSquadStoreRequest request) {
		log.trace("Updating squad with id: {} and request: {}", squadId, request);

		return squadRepository.findById(squadId).map(entity -> {
			entity.setName(request.getName());
			entity.setDescription(request.getDescription());
			entity.setType(request.getType());
			entity.setUpdatedAt(Instant.now());

			entity.getEdges().clear();
			entity.getSteps().clear();

			squadRepository.flush();

			request.getSteps().stream().map(step -> squadPersistenceMapper.buildStepEntity(step, entity.getId()))
					.forEach(entity::addStep);

			request.getEdges().stream().map(squadPersistenceMapper::toEdgeEntity).forEach(entity::addEdge);

			SquadEntity saved = squadRepository.save(entity);

			return squadPersistenceMapper.toSquad(saved);
		});
	}

	@Override
	public boolean deleteById(String squadId) {
		log.trace("Deleting squad by id: {}", squadId);
		// Steps and edges go with the squad; run history keeps its own copy of squadId and is left intact.
		return squadRepository.findById(squadId).map(entity -> {
			squadRepository.delete(entity);
			return true;
		}).orElse(false);
	}
}
