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

	/**
	 * Priority forced onto every default (isDefault = true) edge, regardless of
	 * any value a client may supply. Kept out of the 1-99 non-default range so
	 * it can never collide with a conditional route's priority.
	 */
	public static final int DEFAULT_ROUTE_PRIORITY = 999;

	/** Minimum accepted priority for a non-default edge. */
	public static final int MIN_ROUTE_PRIORITY = 1;

	/** Maximum accepted priority for a non-default edge. */
	public static final int MAX_ROUTE_PRIORITY = 99;

	@NonNull
	private String id;

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
