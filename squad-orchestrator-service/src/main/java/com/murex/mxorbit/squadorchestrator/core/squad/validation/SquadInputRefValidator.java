package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentDefinition;
import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRefSourceType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SquadInputRefValidator {

	private final AgentRegistry agentRegistry;

	public void validate(CreateSquadRequest request) {
		List<SquadStepRequest> steps = request.getSteps() == null ? List.of() : request.getSteps();
		List<SquadEdgeRequest> edges = request.getEdges() == null ? List.of() : request.getEdges();

		if (steps.size() < 2) {
			throw badRequest("A workflow must contain at least two steps.");
		}

		Map<String, SquadStepRequest> stepMap = validateSteps(steps);
		WorkflowGraph graph = validateEdges(edges, stepMap);

		validateRootsAndTerminals(graph);
		validateConnectedGraph(graph);
		validateAcyclicGraph(graph);
		validateInputRefs(steps, stepMap, graph.reverseEdges());
	}

	private Map<String, SquadStepRequest> validateSteps(List<SquadStepRequest> steps) {
		Map<String, SquadStepRequest> stepMap = new LinkedHashMap<>();
		for (SquadStepRequest step : steps) {
			if (step == null) {
				throw badRequest("The workflow contains an invalid step.");
			}

			String stepId = step.getId();
			if (stepId == null || stepId.isBlank()) {
				throw badRequest("A workflow step must have a nonblank id.");
			}

			if (step.getName() == null || step.getName().isBlank()) {
				throw badRequest("Step " + describeStep(step) + " must have a nonblank name.");
			}

			String agentKey = resolveAgentKey(step);
			if (agentKey == null || agentKey.isBlank()) {
				throw badRequest("Step " + describeStep(step) + " must have an assigned agent.");
			}

			if (agentRegistry.findByKey(agentKey).isEmpty()) {
				throw badRequest("Step " + describeStep(step) + " references unknown agent '" + agentKey + "'.");
			}

			SquadStepRequest previous = stepMap.put(stepId, step);
			if (previous != null) {
				throw badRequest("The workflow contains duplicate step id '" + stepId + "'.");
			}
		}

		return stepMap;
	}

	private WorkflowGraph validateEdges(List<SquadEdgeRequest> edges, Map<String, SquadStepRequest> stepMap) {
		Map<String, Set<String>> outgoingEdges = new HashMap<>();
		Map<String, Set<String>> incomingEdges = new HashMap<>();
		Map<String, Set<String>> undirectedEdges = new HashMap<>();
		Map<String, Set<String>> reverseEdges = new HashMap<>();
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

			String edgeKey = sourceStepId + "\u0000" + targetStepId;
			if (!uniqueEdgeKeys.add(edgeKey)) {
				throw badRequest("Connection from " + describeStepLabel(sourceStepId, stepMap) + " to "
						+ describeStepLabel(targetStepId, stepMap) + " is duplicated.");
			}

			outgoingEdges.computeIfAbsent(sourceStepId, key -> new LinkedHashSet<>()).add(targetStepId);
			incomingEdges.computeIfAbsent(targetStepId, key -> new LinkedHashSet<>()).add(sourceStepId);
			undirectedEdges.computeIfAbsent(sourceStepId, key -> new LinkedHashSet<>()).add(targetStepId);
			undirectedEdges.computeIfAbsent(targetStepId, key -> new LinkedHashSet<>()).add(sourceStepId);
			reverseEdges.computeIfAbsent(targetStepId, key -> new LinkedHashSet<>()).add(sourceStepId);
		}

		return new WorkflowGraph(stepMap, outgoingEdges, incomingEdges, undirectedEdges, reverseEdges);
	}

	private void validateRootsAndTerminals(WorkflowGraph graph) {
		List<String> roots = new ArrayList<>();
		List<String> terminals = new ArrayList<>();

		for (String stepId : graph.stepMap().keySet()) {
			if (graph.incomingEdges().getOrDefault(stepId, Set.of()).isEmpty()) {
				roots.add(stepId);
			}
			if (graph.outgoingEdges().getOrDefault(stepId, Set.of()).isEmpty()) {
				terminals.add(stepId);
			}
		}

		if (roots.size() != 1) {
			throw badRequest("The workflow must contain exactly one root step.");
		}

		String rootStepId = roots.get(0);
		if (graph.outgoingEdges().getOrDefault(rootStepId, Set.of()).isEmpty()) {
			throw badRequest("The workflow must contain exactly one root step.");
		}

		if (terminals.size() != 1) {
			throw badRequest("The workflow must contain exactly one terminal step.");
		}

		String terminalStepId = terminals.get(0);
		if (graph.incomingEdges().getOrDefault(terminalStepId, Set.of()).isEmpty()) {
			throw badRequest("The workflow must contain exactly one terminal step.");
		}
	}

	private void validateConnectedGraph(WorkflowGraph graph) {
		String startStepId = graph.stepMap().keySet().iterator().next();
		Set<String> visited = new HashSet<>();
		Deque<String> queue = new ArrayDeque<>();
		queue.add(startStepId);
		visited.add(startStepId);

		while (!queue.isEmpty()) {
			String currentStepId = queue.removeFirst();
			for (String neighborStepId : graph.undirectedEdges().getOrDefault(currentStepId, Set.of())) {
				if (visited.add(neighborStepId)) {
					queue.addLast(neighborStepId);
				}
			}
		}

		if (visited.size() != graph.stepMap().size()) {
			for (String stepId : graph.stepMap().keySet()) {
				if (!visited.contains(stepId)) {
					throw badRequest(
							describeStepLabel(stepId, graph.stepMap()) + " is disconnected from the workflow.");
				}
			}
		}
	}

	private void validateAcyclicGraph(WorkflowGraph graph) {
		Map<String, Integer> incomingCounts = new HashMap<>();
		Deque<String> readySteps = new ArrayDeque<>();

		for (String stepId : graph.stepMap().keySet()) {
			int incomingCount = graph.incomingEdges().getOrDefault(stepId, Set.of()).size();
			incomingCounts.put(stepId, incomingCount);
			if (incomingCount == 0) {
				readySteps.add(stepId);
			}
		}

		int visitedCount = 0;
		while (!readySteps.isEmpty()) {
			String currentStepId = readySteps.removeFirst();
			visitedCount++;

			for (String targetStepId : graph.outgoingEdges().getOrDefault(currentStepId, Set.of())) {
				int nextIncomingCount = incomingCounts.get(targetStepId) - 1;
				incomingCounts.put(targetStepId, nextIncomingCount);
				if (nextIncomingCount == 0) {
					readySteps.addLast(targetStepId);
				}
			}
		}

		if (visitedCount != graph.stepMap().size()) {
			throw badRequest("The workflow contains a directed cycle.");
		}
	}

	private void validateInputRefs(List<SquadStepRequest> steps, Map<String, SquadStepRequest> stepMap,
			Map<String, Set<String>> reverseEdges) {
		boolean isRoot = true;
		for (SquadStepRequest step : steps) {
			Set<String> seenRefs = new HashSet<>();
			Set<String> seenTargetInputs = new HashSet<>();
			Set<String> ancestors = computeAncestors(step.getId(), reverseEdges);
			List<StepInputRef> inputRefs = step.getInputRefs();
			boolean stepIsRoot = ancestors.isEmpty();

			for (StepInputRef ref : inputRefs == null ? List.<StepInputRef>of() : inputRefs) {
				validateInputRef(step, ref, stepMap, ancestors, seenRefs, seenTargetInputs, stepIsRoot);
			}
		}
	}

	private void validateInputRef(SquadStepRequest step, StepInputRef ref, Map<String, SquadStepRequest> stepMap,
			Set<String> ancestors, Set<String> seenRefs, Set<String> seenTargetInputs, boolean stepIsRoot) {
		if (ref == null) {
			throw badRequest("Step " + describeStep(step) + " has a null inputRef.");
		}

		StepInputRefSourceType sourceType = ref.getSourceType();
		if (sourceType == null) {
			throw badRequest("Step " + describeStep(step) + " inputRef must have a sourceType.");
		}

		String targetInput = ref.getTargetInput();
		if (targetInput == null || targetInput.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef must have a targetInput.");
		}

		AgentDefinition targetAgentDefinition = validateCurrentStepAgent(step);
		validateTargetInput(step, targetAgentDefinition, targetInput);

		if (!seenTargetInputs.add(targetInput)) {
			throw badRequest(
					"Step " + describeStep(step) + " has a duplicate inputRef target input '" + targetInput + "'.");
		}

		if (sourceType == StepInputRefSourceType.MANUAL) {
			validateManualInputRef(step, ref, stepIsRoot);
		} else if (sourceType == StepInputRefSourceType.STEP_OUTPUT) {
			validateStepOutputInputRef(step, ref, stepMap, ancestors, seenRefs);
		} else {
			throw badRequest("Step " + describeStep(step) + " inputRef has unknown sourceType: " + sourceType);
		}
	}

	private void validateManualInputRef(SquadStepRequest step, StepInputRef ref, boolean stepIsRoot) {
		if (!stepIsRoot) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType MANUAL is only allowed on root steps.");
		}

		if (ref.getFromStepId() != null && !ref.getFromStepId().isBlank()) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType MANUAL must not have fromStepId.");
		}

		if (ref.getKey() != null && !ref.getKey().isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef with sourceType MANUAL must not have key.");
		}
	}

	private void validateStepOutputInputRef(SquadStepRequest step, StepInputRef ref,
			Map<String, SquadStepRequest> stepMap, Set<String> ancestors, Set<String> seenRefs) {
		String fromStepId = ref.getFromStepId();
		String outputKey = ref.getKey();
		String stepId = step.getId();

		if (fromStepId == null || fromStepId.isBlank()) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType STEP_OUTPUT must have fromStepId.");
		}

		if (outputKey == null || outputKey.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef with sourceType STEP_OUTPUT must have key.");
		}

		if (!stepMap.containsKey(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " references unknown source step "
					+ describeStepLabel(fromStepId, stepMap) + " in an inputRef.");
		}

		if (stepId.equals(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " cannot reference itself in an inputRef.");
		}

		if (!ancestors.contains(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " must reference an upstream ancestor.");
		}

		String duplicateKey = fromStepId + "\u0000" + outputKey;
		if (!seenRefs.add(duplicateKey)) {
			throw badRequest("Step " + describeStep(step) + " has a duplicate inputRef from "
					+ describeStepLabel(fromStepId, stepMap) + " using output key '" + outputKey + "'.");
		}

		validateInputRefOutput(step, fromStepId, outputKey, stepMap);
	}

	private AgentDefinition validateCurrentStepAgent(SquadStepRequest step) {
		if (!(step instanceof AiAgentStepRequest aiAgentStepRequest)) {
			throw badRequest("Step " + describeStep(step) + " must be an AI-agent step with an assigned agent.");
		}

		String agentKey = aiAgentStepRequest.getAgentKey();
		if (agentKey == null || agentKey.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " must be an AI-agent step with an assigned agent.");
		}

		return agentRegistry.findByKey(agentKey).orElseThrow(
				() -> badRequest("Step " + describeStep(step) + " references unknown agent '" + agentKey + "'."));
	}

	private void validateTargetInput(SquadStepRequest step, AgentDefinition agentDefinition, String targetInput) {
		if (!agentDefinition.getInputs().contains(targetInput)) {
			throw badRequest("Step " + describeStep(step) + " inputRef target input '" + targetInput
					+ "' is not declared by agent '" + agentDefinition.getName() + "'.");
		}
	}

	private void validateInputRefOutput(SquadStepRequest step, String fromStepId, String outputKey,
			Map<String, SquadStepRequest> stepMap) {
		SquadStepRequest sourceStep = stepMap.get(fromStepId);
		if (!(sourceStep instanceof AiAgentStepRequest aiAgentStepRequest)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " must reference a step assigned to an agent.");
		}

		String sourceAgentKey = aiAgentStepRequest.getAgentKey();
		List<String> outputs = agentRegistry.findByKey(sourceAgentKey)
				.map(agentDefinition -> agentDefinition.getOutputs())
				.orElseThrow(() -> badRequest(
						"Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
								+ " references unknown agent '" + sourceAgentKey + "'."));

		if (!outputs.contains(outputKey)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " references undeclared output key '" + outputKey + "'.");
		}
	}

	private Set<String> computeAncestors(String stepId, Map<String, Set<String>> reverseEdges) {
		Set<String> ancestors = new HashSet<>();
		Deque<String> queue = new ArrayDeque<>();
		queue.add(stepId);

		while (!queue.isEmpty()) {
			String currentStepId = queue.removeFirst();
			for (String parentStepId : reverseEdges.getOrDefault(currentStepId, Set.of())) {
				if (ancestors.add(parentStepId)) {
					queue.addLast(parentStepId);
				}
			}
		}

		return ancestors;
	}

	private static String resolveAgentKey(SquadStepRequest step) {
		if (step instanceof AiAgentStepRequest aiAgentStepRequest) {
			return aiAgentStepRequest.getAgentKey();
		}
		return null;
	}

	private static String describeStep(SquadStepRequest step) {
		if (step == null) {
			return "step";
		}

		String name = step.getName();
		if (name != null && !name.isBlank()) {
			return "'" + name.trim() + "'";
		}

		String id = step.getId();
		return "with id '" + (id == null || id.isBlank() ? "unknown" : id) + "'";
	}

	private static String describeStepLabel(String stepId, Map<String, SquadStepRequest> stepMap) {
		SquadStepRequest step = stepMap.get(stepId);
		if (step == null) {
			return "Step with id '" + stepId + "'";
		}
		return "Step " + describeStep(step);
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	private record WorkflowGraph(Map<String, SquadStepRequest> stepMap, Map<String, Set<String>> outgoingEdges,
			Map<String, Set<String>> incomingEdges, Map<String, Set<String>> undirectedEdges,
			Map<String, Set<String>> reverseEdges) {
	}
}
