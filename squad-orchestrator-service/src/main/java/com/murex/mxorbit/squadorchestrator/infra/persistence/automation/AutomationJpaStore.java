package com.murex.mxorbit.squadorchestrator.infra.persistence.automation;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.store.AutomationStore;
import com.murex.mxorbit.squadorchestrator.infra.persistence.automation.entity.AutomationEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.automation.mapper.AutomationPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@Transactional
@RequiredArgsConstructor
public class AutomationJpaStore implements AutomationStore {

	private final AutomationRepository automationRepository;
	private final AutomationPersistenceMapper automationPersistenceMapper;

	@Override
	public Automation save(Automation automation) {
		log.trace("Saving automation. automationId: {}, temporalScheduleId: {}", automation.getId(),
				automation.getTemporalScheduleId());
		AutomationEntity entity = automationPersistenceMapper.toEntity(automation);
		AutomationEntity saved = automationRepository.save(entity);
		return automationPersistenceMapper.toAutomation(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Automation> findAll() {
		log.trace("Finding all automations");
		return automationRepository.findAll().stream().map(automationPersistenceMapper::toAutomation).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Automation> findById(UUID id) {
		log.trace("Finding automation by id: {}", id);
		return automationRepository.findById(id).map(automationPersistenceMapper::toAutomation);
	}

	@Override
	public Optional<Automation> update(UUID id, Automation automation) {
		log.trace("Updating automation. automationId: {}, temporalScheduleId: {}", id,
				automation.getTemporalScheduleId());

		return automationRepository.findById(id).map(entity -> {
			automationPersistenceMapper.updateEntity(automation, entity);
			AutomationEntity saved = automationRepository.save(entity);
			return automationPersistenceMapper.toAutomation(saved);
		});
	}

	@Override
	public boolean deleteById(UUID id) {
		log.trace("Deleting automation by id: {}", id);
		return automationRepository.findById(id).map(entity -> {
			automationRepository.delete(entity);
			return true;
		}).orElse(false);
	}
}
