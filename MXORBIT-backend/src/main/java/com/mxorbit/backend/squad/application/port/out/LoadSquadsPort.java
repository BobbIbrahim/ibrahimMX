package com.mxorbit.backend.squad.application.port.out;

import com.mxorbit.backend.squad.domain.model.Squad;

import java.util.List;

public interface LoadSquadsPort {

    List<Squad> loadSquads();
}