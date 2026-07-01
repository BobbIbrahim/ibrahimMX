package com.murex.mxorbit.squadorchestrator.core.squad.facade;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.SquadCreator;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadFacadeService implements SquadFacade {

	private final SquadCreator squadCreator;

	@Override
	public Squad createSquad(CreateSquadRequest request) {
		return squadCreator.createSquad(request);
	}
}
