package com.murex.mxorbit.squadorchestrator.infra.persistence.squad;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.mapper.SquadPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
