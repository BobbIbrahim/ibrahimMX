package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks which steps are runnable as a squad advances.
 *
 * <p>
 * An edge is <em>resolved</em> once its source step has decided whether to
 * traverse it. A step is released only when every incoming edge is resolved,
 * which turns any step with several predecessors into a join barrier. Steps
 * released together are independent and may run concurrently.
 *
 * <p>
 * All collections keep insertion order so the sequence of decisions is
 * identical on every Temporal replay.
 */
final class SquadExecutionPlan {

	private final SquadGraph graph;
	private final Set<String> settledStepIds = new LinkedHashSet<>();
	private final Map<String, Boolean> traversedByEdgeId = new LinkedHashMap<>();

	SquadExecutionPlan(SquadGraph graph) {
		this.graph = graph;
	}

	/** Steps whose predecessors are all resolved and at least one route reached. */
	List<String> releasedStepIds() {
		List<String> releasedStepIds = new ArrayList<>();

		for (String stepId : graph.stepIds()) {
			if (isPending(stepId) && isResolved(stepId) && isReached(stepId)) {
				releasedStepIds.add(stepId);
			}
		}

		return releasedStepIds;
	}

	/** Steps whose predecessors are all resolved but no route reached them. */
	List<String> unreachableStepIds() {
		List<String> unreachableStepIds = new ArrayList<>();

		for (String stepId : graph.stepIds()) {
			if (isPending(stepId) && isResolved(stepId) && !isReached(stepId)) {
				unreachableStepIds.add(stepId);
			}
		}

		return unreachableStepIds;
	}

	List<String> pendingStepIds() {
		return graph.stepIds().stream().filter(this::isPending).toList();
	}

	void markExecuted(String stepId) {
		settledStepIds.add(stepId);
	}

	/** Skipping a step withdraws every route leading out of it. */
	void markSkipped(String stepId) {
		settledStepIds.add(stepId);
		graph.outgoingEdges(stepId).forEach(edge -> traversedByEdgeId.put(edge.getId(), false));
	}

	void traverseAll(String stepId) {
		graph.outgoingEdges(stepId).forEach(edge -> traversedByEdgeId.put(edge.getId(), true));
	}

	void traverseOnly(String stepId, String selectedEdgeId) {
		graph.outgoingEdges(stepId)
				.forEach(edge -> traversedByEdgeId.put(edge.getId(), edge.getId().equals(selectedEdgeId)));
	}

	private boolean isPending(String stepId) {
		return !settledStepIds.contains(stepId);
	}

	private boolean isResolved(String stepId) {
		return graph.incomingEdges(stepId).stream().allMatch(edge -> traversedByEdgeId.containsKey(edge.getId()));
	}

	private boolean isReached(String stepId) {
		List<SquadEdge> incomingEdges = graph.incomingEdges(stepId);
		return incomingEdges.isEmpty()
				|| incomingEdges.stream().anyMatch(edge -> Boolean.TRUE.equals(traversedByEdgeId.get(edge.getId())));
	}
}
