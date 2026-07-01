package com.murex.mxorbit.squadorchestrator.api.squad.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SquadEdgeApiRequest {

	@NotBlank
	private String sourceStepId;

	@NotBlank
	private String targetStepId;
}
