package com.mxorbit.backend.squad.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SquadStepRequest(
        @NotBlank(message = "Frontend step id is required")
        String id,

        @NotBlank(message = "Step name is required")
        String name,

        String description,

        @NotBlank(message = "Assigned agent id is required")
        String assignedAgentId,

        Map<String, Object> parameters,

        @Valid
        @NotNull(message = "Step position is required")
        SquadPositionRequest position
) {
}