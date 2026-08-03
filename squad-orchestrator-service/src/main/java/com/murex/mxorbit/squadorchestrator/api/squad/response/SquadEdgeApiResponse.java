package com.murex.mxorbit.squadorchestrator.api.squad.response;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
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

	@NonNull
	private SquadEdgeRoutingType routingType;

	private String condition;

	@NonNull
	private Integer priority;

	@NonNull
	private Boolean isDefault;
}
