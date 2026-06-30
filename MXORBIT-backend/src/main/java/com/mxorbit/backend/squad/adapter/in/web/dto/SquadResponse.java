package com.mxorbit.backend.squad.adapter.in.web.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SquadResponse(
        UUID id,
        String frontendDraftId,
        String name,
        String description,
        String type,
        String projectKey,
        String status,
        List<SquadStepResponse> steps,
        List<SquadEdgeResponse> edges,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}