package com.mxorbit.backend.squad.application.service;

import com.mxorbit.backend.squad.application.port.in.CreateSquadUseCase;
import com.mxorbit.backend.squad.application.port.in.GetSquadsUseCase;
import com.mxorbit.backend.squad.application.port.out.LoadSquadsPort;
import com.mxorbit.backend.squad.application.port.out.SaveSquadPort;
import com.mxorbit.backend.squad.domain.model.Squad;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SquadApplicationService implements CreateSquadUseCase, GetSquadsUseCase {

    private static final String DEFAULT_SQUAD_STATUS = "draft";

    private final SaveSquadPort saveSquadPort;
    private final LoadSquadsPort loadSquadsPort;

    public SquadApplicationService(
            SaveSquadPort saveSquadPort,
            LoadSquadsPort loadSquadsPort
    ) {
        this.saveSquadPort = saveSquadPort;
        this.loadSquadsPort = loadSquadsPort;
    }

    @Override
    public Squad createSquad(Squad squad) {
        Squad squadToSave = ensureDefaultStatus(squad);

        return saveSquadPort.saveSquad(squadToSave);
    }

    @Override
    public List<Squad> getSquads() {
        return loadSquadsPort.loadSquads();
    }

    private Squad ensureDefaultStatus(Squad squad) {
        if (squad.status() != null && !squad.status().isBlank()) {
            return squad;
        }

        return new Squad(
                squad.id(),
                squad.frontendDraftId(),
                squad.name(),
                squad.description(),
                squad.type(),
                squad.projectKey(),
                DEFAULT_SQUAD_STATUS,
                squad.steps(),
                squad.edges(),
                squad.createdAt(),
                squad.updatedAt()
        );
    }
}