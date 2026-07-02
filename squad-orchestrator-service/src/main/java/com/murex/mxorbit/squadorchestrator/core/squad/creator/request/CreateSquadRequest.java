package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSquadRequest {

	@NonNull
	private String name;

	private String description;

	@NonNull
	private String type;

	@NonNull
	private List<SquadStepRequest> steps;

	@NonNull
	private List<SquadEdgeRequest> edges;
}
