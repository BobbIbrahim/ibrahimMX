package com.murex.mxorbit.squadorchestrator.core.squad.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadEdge {

	@NonNull
	private String id;

	@NonNull
	private String sourceStepId;

	@NonNull
	private String targetStepId;
}
