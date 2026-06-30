package com.mxorbit.backend.squad.adapter.in.web.dto;

import java.util.Map;
import java.util.UUID;

public record SquadStepResponse(
        UUID id,
        String frontendStepId,
        String name,
        String description,
        String assignedAgentId,
        Map<String, Object> parameters,
        SquadPositionRequest position
) {
}