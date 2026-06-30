package com.mxorbit.backend.squad.application.service;

import com.mxorbit.backend.squad.application.port.in.CreateSquadUseCase;
import com.mxorbit.backend.squad.application.port.out.SaveSquadPort;
import com.mxorbit.backend.squad.domain.model.Squad;
import org.springframework.stereotype.Service;

@Service
public class SquadApplicationService implements CreateSquadUseCase {

    private static final String DEFAULT_SQUAD_STATUS = "draft";

    private final SaveSquadPort saveSquadPort;

    public SquadApplicationService(SaveSquadPort saveSquadPort) {
        this.saveSquadPort = saveSquadPort;
    }

    @Override
    public Squad createSquad(Squad squad) {
        Squad squadToSave = ensureDefaultStatus(squad);

        return saveSquadPort.saveSquad(squadToSave);
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