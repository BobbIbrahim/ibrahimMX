package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable adjacency view of a squad. Every collection keeps insertion order
 * so traversal stays deterministic across Temporal replays.
 */
final class SquadGraph {

	private final Map<String, List<SquadEdge>> outgoingEdgesBySourceStepId;
	private final Map<String, List<SquadEdge>> incomingEdgesByTargetStepId;
	private final String rootStepId;

	private SquadGraph(Map<String, List<SquadEdge>> outgoingEdgesBySourceStepId,
			Map<String, List<SquadEdge>> incomingEdgesByTargetStepId, String rootStepId) {
		this.outgoingEdgesBySourceStepId = outgoingEdgesBySourceStepId;
		this.incomingEdgesByTargetStepId = incomingEdgesByTargetStepId;
		this.rootStepId = rootStepId;
	}

	static SquadGraph build(List<SquadStep> steps, List<SquadEdge> edges) {
		Map<String, List<SquadEdge>> outgoingEdgesBySourceStepId = new LinkedHashMap<>();
		Map<String, List<SquadEdge>> incomingEdgesByTargetStepId = new LinkedHashMap<>();
		for (SquadStep step : steps) {
			outgoingEdgesBySourceStepId.put(step.getId(), new ArrayList<>());
			incomingEdgesByTargetStepId.put(step.getId(), new ArrayList<>());
		}

		Set<String> targetStepIds = new LinkedHashSet<>();
		for (SquadEdge edge : edges) {
			List<SquadEdge> outgoingEdges = outgoingEdgesBySourceStepId.get(edge.getSourceStepId());
			if (outgoingEdges == null || !outgoingEdgesBySourceStepId.containsKey(edge.getTargetStepId())) {
				throw new SquadGraphException("Invalid edge " + edge.getId() + " references unknown steps. source="
						+ edge.getSourceStepId() + ", target=" + edge.getTargetStepId());
			}

			outgoingEdges.add(edge);
			incomingEdgesByTargetStepId.get(edge.getTargetStepId()).add(edge);
			targetStepIds.add(edge.getTargetStepId());
		}

		return new SquadGraph(outgoingEdgesBySourceStepId, incomingEdgesByTargetStepId,
				findSingleRootStepId(steps, targetStepIds));
	}

	String getRootStepId() {
		return rootStepId;
	}

	List<SquadEdge> outgoingEdges(String stepId) {
		return outgoingEdgesBySourceStepId.getOrDefault(stepId, List.of());
	}

	List<SquadEdge> incomingEdges(String stepId) {
		return incomingEdgesByTargetStepId.getOrDefault(stepId, List.of());
	}

	Set<String> stepIds() {
		return outgoingEdgesBySourceStepId.keySet();
	}

	Set<String> reachableFrom(String startStepId) {
		Set<String> reachableStepIds = new LinkedHashSet<>();
		ArrayDeque<String> queue = new ArrayDeque<>();
		queue.add(startStepId);

		while (!queue.isEmpty()) {
			String stepId = queue.removeFirst();
			if (!reachableStepIds.add(stepId)) {
				continue;
			}

			for (SquadEdge edge : outgoingEdges(stepId)) {
				queue.addLast(edge.getTargetStepId());
			}
		}

		return reachableStepIds;
	}

	private static String findSingleRootStepId(List<SquadStep> steps, Set<String> targetStepIds) {
		List<String> rootStepIds = steps.stream().map(SquadStep::getId)
				.filter(stepId -> !targetStepIds.contains(stepId)).toList();

		if (rootStepIds.size() != 1) {
			throw new SquadGraphException(
					"A squad must have exactly one root step but found " + rootStepIds.size() + ".");
		}

		return rootStepIds.get(0);
	}
}
