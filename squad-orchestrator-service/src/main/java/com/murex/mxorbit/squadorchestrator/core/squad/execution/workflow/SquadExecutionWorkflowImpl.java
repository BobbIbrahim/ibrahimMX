package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@WorkflowImpl(taskQueues = "squad-orchestration-task-queue")
public class SquadExecutionWorkflowImpl implements SquadExecutionWorkflow {

    private final GetSquadActivity getSquadActivity = Workflow.newActivityStub(GetSquadActivity.class,
            buildActivityOptions());

    private final RunAiAgentActivity runAiAgentActivity = Workflow.newActivityStub(RunAiAgentActivity.class,
            buildActivityOptions());

    @Override
    public SquadExecutionResult execute(SquadExecutionRequest request) {
        GetSquadResult getSquadResult = getSquadActivity
                .getSquad(GetSquadRequest.builder().squadId(request.getSquadId()).build());

        Squad squad = getSquadResult.getSquad();

        List<SquadStep> orderedSteps = orderStepsSequentially(squad);
        List<SquadStepExecutionResult> stepResults = new ArrayList<>();

        for (SquadStep step : orderedSteps) {
            stepResults.add(executeSingleStep(squad, step));
        }

        return SquadExecutionResult.builder().squadId(squad.getId()).status("COMPLETED")
                .message("Executed " + stepResults.size() + " squad step(s) successfully.").build();
    }

    private SquadStepExecutionResult executeSingleStep(Squad squad, SquadStep step) {
        if (!(step instanceof AiAgentStep aiAgentStep)) {
            throw new IllegalArgumentException("Unsupported squad step type: " + step.getClass().getSimpleName());
        }

        SquadStepExecutionRequest stepRequest = SquadStepExecutionRequest.builder().squadId(squad.getId())
                .stepId(aiAgentStep.getId()).stepName(aiAgentStep.getName()).agentKey(aiAgentStep.getAgentKey())
                .build();

        return runAiAgentActivity.runAiAgent(stepRequest);
    }

    private List<SquadStep> orderStepsSequentially(Squad squad) {
        if (squad.getSteps().isEmpty()) {
            return List.of();
        }

        Map<String, SquadStep> stepsById = new HashMap<>();

        for (SquadStep step : squad.getSteps()) {
            stepsById.put(step.getId(), step);
        }

        Map<String, List<String>> outgoingTargetsBySource = new HashMap<>();
        Set<String> targetedStepIds = new HashSet<>();

        for (SquadEdge edge : squad.getEdges()) {
            if (!stepsById.containsKey(edge.getSourceStepId()) || !stepsById.containsKey(edge.getTargetStepId())) {
                throw new IllegalArgumentException("Invalid squad edge from " + edge.getSourceStepId() + " to "
                        + edge.getTargetStepId() + " for squad " + squad.getId());
            }

            outgoingTargetsBySource.computeIfAbsent(edge.getSourceStepId(), ignored -> new ArrayList<>())
                    .add(edge.getTargetStepId());

            targetedStepIds.add(edge.getTargetStepId());
        }

        SquadStep firstStep = findFirstStep(squad, targetedStepIds);

        List<SquadStep> orderedSteps = new ArrayList<>();
        Set<String> visitedStepIds = new HashSet<>();

        SquadStep currentStep = firstStep;

        while (currentStep != null && visitedStepIds.add(currentStep.getId())) {
            orderedSteps.add(currentStep);

            List<String> nextStepIds = outgoingTargetsBySource.getOrDefault(currentStep.getId(), List.of());

            if (nextStepIds.isEmpty()) {
                currentStep = null;
                continue;
            }

            if (nextStepIds.size() > 1) {
                throw new IllegalStateException("Sequential squad execution does not support branching. Step with id "
                        + currentStep.getId() + " has " + nextStepIds.size() + " outgoing edges.");
            }

            currentStep = stepsById.get(nextStepIds.get(0));
        }

        if (orderedSteps.size() != squad.getSteps().size()) {
            throw new IllegalStateException(
                    "Sequential squad execution could not include all steps. Check disconnected steps or loops.");
        }

        return orderedSteps;
    }

    private SquadStep findFirstStep(Squad squad, Set<String> targetedStepIds) {
        List<SquadStep> sourceCandidates = squad.getSteps().stream()
                .filter(step -> !targetedStepIds.contains(step.getId()))
                .toList();

        if (sourceCandidates.size() == 1) {
            return sourceCandidates.get(0);
        }

        if (sourceCandidates.isEmpty()) {
            throw new IllegalStateException(
                    "Sequential squad execution requires exactly one starting step, but none was found.");
        }

        throw new IllegalStateException(
                "Sequential squad execution requires exactly one starting step, but found "
                        + sourceCandidates.size()
                        + ".");
    }

    private ActivityOptions buildActivityOptions() {
        RetryOptions retryOptions = RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1))
                .setMaximumInterval(Duration.ofSeconds(20)).setBackoffCoefficient(2).setMaximumAttempts(3).build();

        return ActivityOptions.newBuilder().setRetryOptions(retryOptions).setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToCloseTimeout(Duration.ofMinutes(5)).build();
    }
}
