package com.murex.mxorbit.squadorchestrator.core.squad.creator.request;

import static com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge.MIN_ROUTE_PRIORITY;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
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

	@Builder.Default
	private SquadEdgeRoutingType routingType = SquadEdgeRoutingType.ALWAYS;

	private String condition;

	@Builder.Default
	private Integer priority = MIN_ROUTE_PRIORITY;

	@Builder.Default
	private Boolean isDefault = false;
}
