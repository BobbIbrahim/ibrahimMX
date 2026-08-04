package com.murex.mxorbit.squadorchestrator.core.squad.routing.store.request;

import com.murex.mxorbit.squadorchestrator.core.squad.routing.SquadRoutingDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadRoutingDecisionStoreRequest {

	@NonNull
	private String squadRunId;

	@NonNull
	private String squadId;

	@NonNull
	private Integer decisionSequence;

	@NonNull
	private SquadRoutingDecision decision;
}
