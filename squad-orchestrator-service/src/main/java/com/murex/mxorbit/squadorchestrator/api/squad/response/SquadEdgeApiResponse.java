package com.murex.mxorbit.squadorchestrator.api.squad.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadEdgeApiResponse {

	@NonNull
	private String id;

	@NonNull
	private String sourceStepId;

	@NonNull
	private String targetStepId;
}
