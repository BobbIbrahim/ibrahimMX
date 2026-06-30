package com.mxorbit.backend.squad.domain.model;

import java.util.Map;
import java.util.UUID;

public record SquadStep(
        UUID id,
        String frontendStepId,
        String name,
        String description,
        String assignedAgentId,
        Map<String, Object> parameters,
        SquadPosition position
) {
}