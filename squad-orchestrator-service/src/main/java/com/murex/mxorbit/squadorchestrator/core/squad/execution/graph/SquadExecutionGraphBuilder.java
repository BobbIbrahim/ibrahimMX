package com.murex.mxorbit.squadorchestrator.core.squad.execution.graph;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import io.temporal.failure.ApplicationFailure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SquadExecutionGraphBuilder {

	private SquadExecutionGraphBuilder() {
	}

	public static SquadExecutionGraph from(Squad squad) {
		Map<String, SquadStep> stepsById = new HashMap<>();

		for (SquadStep step : squad.getSteps()) {
			stepsById.put(step.getId(), step);
		}

		Map<String, List<String>> outgoingTargetsBySource = new HashMap<>();
		Set<String> targetedStepIds = new HashSet<>();

		for (SquadEdge edge : squad.getEdges()) {
			if (!stepsById.containsKey(edge.getSourceStepId()) || !stepsById.containsKey(edge.getTargetStepId())) {
				throw ApplicationFailure.newNonRetryableFailure("Invalid squad edge from " + edge.getSourceStepId()
						+ " to " + edge.getTargetStepId() + " for squad " + squad.getId(), "INVALID_SQUAD_GRAPH");
			}

			outgoingTargetsBySource.computeIfAbsent(edge.getSourceStepId(), ignored -> new ArrayList<>())
					.add(edge.getTargetStepId());

			targetedStepIds.add(edge.getTargetStepId());
		}

		return SquadExecutionGraph.builder().squad(squad).stepsById(stepsById)
				.outgoingTargetsBySource(outgoingTargetsBySource).targetedStepIds(targetedStepIds).build();
	}
}
