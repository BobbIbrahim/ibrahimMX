package com.murex.mxorbit.squadorchestrator.api.squad.response;

import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadApiResponse {

	@NonNull
	private String id;

	@NonNull
	private String name;

	private String description;

	@NonNull
	private List<SquadStepApiResponse> steps;

	@NonNull
	private List<SquadEdgeApiResponse> edges;
}
