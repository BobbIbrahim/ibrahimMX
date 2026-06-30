package com.mxorbit.backend.squad.application.port.in;

import com.mxorbit.backend.squad.domain.model.Squad;

import java.util.List;

public interface GetSquadsUseCase {

    List<Squad> getSquads();
}