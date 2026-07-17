package com.murex.mxorbit.squadorchestrator.core.squad.creator;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.mapper.SquadCreatorMapper;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.store.SquadStore;
import com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadInputRefValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadCreatorService implements SquadCreator {

	private final SquadStore squadStore;
	private final SquadCreatorMapper squadCreatorMapper;
	private final SquadInputRefValidator squadInputRefValidator;

	@Override
	public Squad createSquad(CreateSquadRequest request) {
		log.debug("Creating squad with request: {}", request);
		squadInputRefValidator.validate(request);
		Squad squad = squadStore.save(squadCreatorMapper.toCreateSquadStoreRequest(request));
		log.info("Squad created: {}", squad);
		return squad;
	}
}
