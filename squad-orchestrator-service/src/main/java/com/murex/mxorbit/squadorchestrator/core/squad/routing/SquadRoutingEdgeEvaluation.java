package com.murex.mxorbit.squadorchestrator.core.squad.routing;

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
public class SquadRoutingEdgeEvaluation {

	@NonNull
	private String edgeId;

	@NonNull
	private String targetStepId;

	@NonNull
	private SquadEdgeRoutingType routingType;

	private String condition;

	@NonNull
	private Integer priority;

	@Builder.Default
	private Boolean isDefault = false;

	@Builder.Default
	private Boolean matched = false;

	@NonNull
	private String reason;
}
