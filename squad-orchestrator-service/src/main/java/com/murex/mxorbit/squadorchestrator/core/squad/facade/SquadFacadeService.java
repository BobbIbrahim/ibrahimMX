package com.murex.mxorbit.squadorchestrator.core.squad.facade;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.SquadCreator;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadFacadeService implements SquadFacade {

	private final SquadCreator squadCreator;
	private final SquadProvider squadProvider;

	@Override
	public Squad createSquad(CreateSquadRequest request) {
		return squadCreator.createSquad(request);
	}

	@Override
	public List<Squad> getSquads() {
		return squadProvider.getSquads();
	}
}
