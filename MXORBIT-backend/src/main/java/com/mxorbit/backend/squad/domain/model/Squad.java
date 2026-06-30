package com.mxorbit.backend.squad.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Squad(
        UUID id,
        String frontendDraftId,
        String name,
        String description,
        String type,
        String projectKey,
        String status,
        List<SquadStep> steps,
        List<SquadEdge> edges,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}