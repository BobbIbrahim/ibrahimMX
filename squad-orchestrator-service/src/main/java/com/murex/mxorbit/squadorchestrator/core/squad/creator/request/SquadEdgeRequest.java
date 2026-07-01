package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadEdgeRequest {

	@NonNull
	private String sourceStepId;

	@NonNull
	private String targetStepId;
}
