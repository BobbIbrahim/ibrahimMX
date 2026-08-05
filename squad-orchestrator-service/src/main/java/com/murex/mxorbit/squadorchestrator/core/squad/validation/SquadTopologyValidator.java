package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.badRequest;
import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.describeStepLabel;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Validates squad shape: a single connected, acyclic path from one root to one
 * terminal.
 */
@Service
public class SquadTopologyValidator {

	private static final String NULL_BYTE_SEPARATOR = "\u0000";

	/** Validates each edge in isolation and indexes the resulting adjacency. */
	public SquadTopology build(List<SquadEdgeRequest> edges, Map<String, SquadStepRequest> stepMap) {
		Map<String, Set<String>> outgoingEdges = new HashMap<>();
		Map<String, Set<String>> incomingEdges = new HashMap<>();
		Map<String, Set<String>> undirectedEdges = new HashMap<>();
		Set<String> uniqueEdgeKeys = new HashSet<>();

		for (SquadEdgeRequest edge : edges) {
			if (edge == null) {
				throw badRequest("The workflow contains an invalid edge.");
			}

			String sourceStepId = edge.getSourceStepId();
			String targetStepId = edge.getTargetStepId();

			if (!stepMap.containsKey(sourceStepId)) {
				throw badRequest("Connection from " + describeStepLabel(sourceStepId, stepMap)
						+ " references an unknown source step.");
			}

			if (!stepMap.containsKey(targetStepId)) {
				throw badRequest("Connection to " + describeStepLabel(targetStepId, stepMap)
						+ " references an unknown target step.");
			}

			if (sourceStepId.equals(targetStepId)) {
				String stepLabel = describeStepLabel(sourceStepId, stepMap);
				throw badRequest(
						"Connection from " + stepLabel + " to " + stepLabel + " must connect two different steps.");
			}

			if (!uniqueEdgeKeys.add(sourceStepId + NULL_BYTE_SEPARATOR + targetStepId)) {
				throw badRequest("Connection from " + describeStepLabel(sourceStepId, stepMap) + " to "
						+ describeStepLabel(targetStepId, stepMap) + " is duplicated.");
			}

			link(outgoingEdges, sourceStepId, targetStepId);
			link(incomingEdges, targetStepId, sourceStepId);
			link(undirectedEdges, sourceStepId, targetStepId);
			link(undirectedEdges, targetStepId, sourceStepId);
		}

		return new SquadTopology(stepMap, outgoingEdges, incomingEdges, undirectedEdges);
	}

	/**
	 * Validates the graph as a whole once every edge is known to be well formed.
	 */
	public void validateStructure(SquadTopology topology) {
		validateRootsAndTerminals(topology);
		validateConnected(topology);
		validateAcyclic(topology);
	}

	private void validateRootsAndTerminals(SquadTopology topology) {
		List<String> roots = new ArrayList<>();
		List<String> terminals = new ArrayList<>();

		for (String stepId : topology.stepMap().keySet()) {
			if (topology.parentsOf(stepId).isEmpty()) {
				roots.add(stepId);
			}
			if (topology.childrenOf(stepId).isEmpty()) {
				terminals.add(stepId);
			}
		}

		if (roots.size() != 1 || topology.childrenOf(roots.get(0)).isEmpty()) {
			throw badRequest("The workflow must contain exactly one root step.");
		}

		if (terminals.isEmpty()) {
			throw badRequest("The workflow must contain at least one terminal step.");
		}

		for (String terminalStepId : terminals) {
			if (topology.parentsOf(terminalStepId).isEmpty()) {
				throw badRequest("The workflow must contain at least one terminal step.");
			}
		}
	}

	private void validateConnected(SquadTopology topology) {
		String startStepId = topology.stepMap().keySet().iterator().next();
		Set<String> visited = new HashSet<>();
		Deque<String> queue = new ArrayDeque<>();
		queue.add(startStepId);
		visited.add(startStepId);

		while (!queue.isEmpty()) {
			for (String neighborStepId : topology.undirectedNeighborsOf(queue.removeFirst())) {
				if (visited.add(neighborStepId)) {
					queue.addLast(neighborStepId);
				}
			}
		}

		for (String stepId : topology.stepMap().keySet()) {
			if (!visited.contains(stepId)) {
				throw badRequest(describeStepLabel(stepId, topology.stepMap()) + " is disconnected from the workflow.");
			}
		}
	}

	private void validateAcyclic(SquadTopology topology) {
		Map<String, Integer> incomingCounts = new HashMap<>();
		Deque<String> readySteps = new ArrayDeque<>();

		for (String stepId : topology.stepMap().keySet()) {
			int incomingCount = topology.parentsOf(stepId).size();
			incomingCounts.put(stepId, incomingCount);
			if (incomingCount == 0) {
				readySteps.add(stepId);
			}
		}

		int visitedCount = 0;
		while (!readySteps.isEmpty()) {
			visitedCount++;
			for (String targetStepId : topology.childrenOf(readySteps.removeFirst())) {
				if (incomingCounts.merge(targetStepId, -1, Integer::sum) == 0) {
					readySteps.addLast(targetStepId);
				}
			}
		}

		if (visitedCount != topology.stepMap().size()) {
			throw badRequest("The workflow contains a directed cycle.");
		}
	}

	private static void link(Map<String, Set<String>> adjacency, String fromStepId, String toStepId) {
		adjacency.computeIfAbsent(fromStepId, key -> new LinkedHashSet<>()).add(toStepId);
	}

	public record SquadTopology(Map<String, SquadStepRequest> stepMap, Map<String, Set<String>> outgoingEdges,
			Map<String, Set<String>> incomingEdges, Map<String, Set<String>> undirectedEdges) {

		public Set<String> childrenOf(String stepId) {
			return outgoingEdges.getOrDefault(stepId, Set.of());
		}

		public Set<String> parentsOf(String stepId) {
			return incomingEdges.getOrDefault(stepId, Set.of());
		}

		public Set<String> undirectedNeighborsOf(String stepId) {
			return undirectedEdges.getOrDefault(stepId, Set.of());
		}

		public Set<String> ancestorsOf(String stepId) {
			Set<String> ancestors = new HashSet<>();
			Deque<String> queue = new ArrayDeque<>();
			queue.add(stepId);

			while (!queue.isEmpty()) {
				for (String parentStepId : parentsOf(queue.removeFirst())) {
					if (ancestors.add(parentStepId)) {
						queue.addLast(parentStepId);
					}
				}
			}

			return ancestors;
		}
	}
}
