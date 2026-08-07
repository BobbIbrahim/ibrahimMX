package com.murex.mxorbit.squadorchestrator.api.squad.request;

import static com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge.MIN_ROUTE_PRIORITY;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdgeRoutingType;
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

	@Builder.Default
	private SquadEdgeRoutingType routingType = SquadEdgeRoutingType.ALWAYS;

	private String condition;

	@Builder.Default
	private Integer priority = MIN_ROUTE_PRIORITY;

	@Builder.Default
	private Boolean isDefault = false;
}
