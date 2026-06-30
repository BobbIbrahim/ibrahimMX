package com.mxorbit.backend.squad.application.port.in;

import com.mxorbit.backend.squad.domain.model.Squad;

public interface CreateSquadUseCase {

    Squad createSquad(Squad squad);
}
