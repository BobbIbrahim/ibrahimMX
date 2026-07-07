package com.murex.mxorbit.squadorchestrator.core.squad.execution.graph;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import io.temporal.failure.ApplicationFailure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SquadExecutionGraphResolver {

	private SquadExecutionGraphResolver() {
	}

	public static List<List<SquadStep>> resolveExecutionBatches(SquadExecutionGraph graph) {
		Squad squad = graph.getSquad();

		if (squad.getSteps().isEmpty()) {
			return List.of();
		}

		Map<String, Integer> stepOrder = buildStepOrder(squad);
		Map<String, Integer> remainingDependenciesByStepId = buildRemainingDependencies(graph);

		List<String> startStepIds = findStartStepIds(squad, graph.getTargetedStepIds());

		if (startStepIds.size() != 1) {
			throw ApplicationFailure
					.newNonRetryableFailure("Squad execution requires exactly one starting step, but found "
							+ startStepIds.size() + " for squad " + squad.getId(), "INVALID_SQUAD_GRAPH");
		}

		List<List<SquadStep>> executionBatches = new ArrayList<>();
		Set<String> executedStepIds = new HashSet<>();
		List<String> readyStepIds = startStepIds;

		while (!readyStepIds.isEmpty()) {
			readyStepIds = sortStepIdsByOriginalOrder(readyStepIds, stepOrder);

			List<SquadStep> currentBatch = readyStepIds.stream().map(stepId -> graph.getStepsById().get(stepId))
					.toList();

			executionBatches.add(currentBatch);
			executedStepIds.addAll(readyStepIds);

			List<String> nextReadyStepIds = new ArrayList<>();

			for (String completedStepId : readyStepIds) {
				List<String> dependentStepIds = graph.getOutgoingTargetsBySource().getOrDefault(completedStepId,
						List.of());

				for (String dependentStepId : dependentStepIds) {
					int updatedRemainingDependencies = remainingDependenciesByStepId.get(dependentStepId) - 1;
					remainingDependenciesByStepId.put(dependentStepId, updatedRemainingDependencies);

					if (updatedRemainingDependencies == 0) {
						nextReadyStepIds.add(dependentStepId);
					}
				}
			}

			readyStepIds = nextReadyStepIds;
		}

		if (executedStepIds.size() != squad.getSteps().size()) {
			throw ApplicationFailure.newNonRetryableFailure(
					"Squad execution could not include all steps. Check disconnected steps or cycles for squad "
							+ squad.getId(),
					"INVALID_SQUAD_GRAPH");
		}

		return executionBatches;
	}

	private static Map<String, Integer> buildStepOrder(Squad squad) {
		Map<String, Integer> stepOrder = new HashMap<>();

		for (int index = 0; index < squad.getSteps().size(); index++) {
			stepOrder.put(squad.getSteps().get(index).getId(), index);
		}

		return stepOrder;
	}

	private static Map<String, Integer> buildRemainingDependencies(SquadExecutionGraph graph) {
		Map<String, Integer> remainingDependenciesByStepId = new HashMap<>();

		for (SquadStep step : graph.getSquad().getSteps()) {
			remainingDependenciesByStepId.put(step.getId(), 0);
		}

		for (List<String> targetStepIds : graph.getOutgoingTargetsBySource().values()) {
			for (String targetStepId : targetStepIds) {
				remainingDependenciesByStepId.put(targetStepId, remainingDependenciesByStepId.get(targetStepId) + 1);
			}
		}

		return remainingDependenciesByStepId;
	}

	private static List<String> findStartStepIds(Squad squad, Set<String> targetedStepIds) {
		return squad.getSteps().stream().filter(step -> !targetedStepIds.contains(step.getId())).map(SquadStep::getId)
				.toList();
	}

	private static List<String> sortStepIdsByOriginalOrder(List<String> stepIds, Map<String, Integer> stepOrder) {
		return stepIds.stream().sorted((left, right) -> Integer.compare(stepOrder.get(left), stepOrder.get(right)))
				.toList();
	}
}
