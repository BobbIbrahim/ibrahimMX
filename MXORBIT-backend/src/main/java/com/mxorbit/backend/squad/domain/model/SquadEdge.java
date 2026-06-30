package com.mxorbit.backend.squad.domain.model;

import java.util.UUID;

public record SquadEdge(
        UUID id,
        String frontendEdgeId,
        String sourceStepId,
        String targetStepId
) {
}