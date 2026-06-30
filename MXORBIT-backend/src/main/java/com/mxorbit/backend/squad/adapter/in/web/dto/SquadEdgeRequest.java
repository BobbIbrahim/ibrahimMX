package com.mxorbit.backend.squad.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SquadEdgeRequest(
        @NotBlank(message = "Frontend edge id is required")
        String id,

        @NotBlank(message = "Source step id is required")
        String sourceStepId,

        @NotBlank(message = "Target step id is required")
        String targetStepId
) {
}