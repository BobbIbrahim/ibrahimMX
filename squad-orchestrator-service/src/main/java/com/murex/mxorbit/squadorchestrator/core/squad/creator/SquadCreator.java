package com.murex.mxorbit.squadorchestrator.core.squad.creator;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;

public interface SquadCreator {

	Squad createSquad(CreateSquadRequest request);
}
