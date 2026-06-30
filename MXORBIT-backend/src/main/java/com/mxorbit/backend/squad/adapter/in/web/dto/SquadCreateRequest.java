package com.mxorbit.backend.squad.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SquadCreateRequest(
        String id,

        @NotBlank(message = "Squad name is required")
        String name,

        String description,

        @NotBlank(message = "Squad type is required")
        String type,

        @NotBlank(message = "Project key is required")
        String projectKey,

        @Valid
        @Size(min = 2, message = "At least two steps are required")
        List<SquadStepRequest> steps,

        @Valid
        @NotEmpty(message = "At least one edge is required")
        List<SquadEdgeRequest> edges
) {
}