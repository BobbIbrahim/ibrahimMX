package com.murex.mxorbit.squadorchestrator.api.squad.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateSquadApiRequest {

	@NotBlank
	private String name;

	private String description;

	@Valid
	@NotEmpty
	private List<SquadStepApiRequest> steps;

	@Valid
	@NotEmpty
	private List<SquadEdgeApiRequest> edges;
}
