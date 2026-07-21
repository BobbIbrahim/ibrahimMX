package com.murex.mxorbit.squadorchestrator.core.squad.updater;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.mapper.SquadCreatorMapper;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;
import com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadInputRefValidator;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadUpdaterService implements SquadUpdater {

	private final SquadStore squadStore;
	private final SquadCreatorMapper squadCreatorMapper;
	private final SquadInputRefValidator squadInputRefValidator;

	@Override
	public Optional<Squad> updateSquad(String squadId, CreateSquadRequest request) {
		log.debug("Updating squad with id: {} and request: {}", squadId, request);
		squadInputRefValidator.validate(request);
		return squadStore.update(squadId, squadCreatorMapper.toCreateSquadStoreRequest(request));
	}
}
