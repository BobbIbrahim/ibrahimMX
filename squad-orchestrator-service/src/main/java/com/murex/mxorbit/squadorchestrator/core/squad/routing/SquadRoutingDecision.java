package com.murex.mxorbit.squadorchestrator.core.squad.routing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;

import java.util.ArrayList;
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
public class SquadRoutingDecision {

	@NonNull
	private String sourceStepId;

	private String selectedEdgeId;

	private String selectedTargetStepId;

	@NonNull
	private SquadRoutingDecisionOutcome outcome;

	@NonNull
	private String reason;

	@Builder.Default
	@NonNull
	private List<SquadRoutingEdgeEvaluation> checkedEdges = new ArrayList<>();

	private SquadEdge selectedEdge;

	@JsonIgnore
	public boolean isSuccessful() {
		return selectedEdge != null;
	}
}
