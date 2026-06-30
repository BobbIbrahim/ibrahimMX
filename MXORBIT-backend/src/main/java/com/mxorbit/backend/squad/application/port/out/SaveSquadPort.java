package com.mxorbit.backend.squad.application.port.out;

import com.mxorbit.backend.squad.domain.model.Squad;

public interface SaveSquadPort {

    Squad saveSquad(Squad squad);
}