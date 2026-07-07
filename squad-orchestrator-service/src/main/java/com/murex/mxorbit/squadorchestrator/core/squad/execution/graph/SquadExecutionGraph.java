package com.murex.mxorbit.squadorchestrator.core.squad.execution.graph;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadExecutionGraph {

	@NonNull
	private Squad squad;

	@NonNull
	private Map<String, SquadStep> stepsById;

	@NonNull
	private Map<String, List<String>> outgoingTargetsBySource;

	@NonNull
	private Set<String> targetedStepIds;
}
