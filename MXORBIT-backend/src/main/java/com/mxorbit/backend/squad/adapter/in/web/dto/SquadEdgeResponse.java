package com.mxorbit.backend.squad.adapter.in.web.dto;

import java.util.UUID;

public record SquadEdgeResponse(
        UUID id,
        String frontendEdgeId,
        String sourceStepId,
        String targetStepId
) {
}