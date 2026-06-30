package com.mxorbit.backend.squad.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record SquadPositionRequest(
        @NotNull(message = "Step position x is required")
        Double x,

        @NotNull(message = "Step position y is required")
        Double y
) {
}