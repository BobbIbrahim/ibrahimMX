package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.GetSquadActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.activity.RunAiAgentActivity;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.graph.SquadExecutionGraph;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.graph.SquadExecutionGraphBuilder;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.graph.SquadExecutionGraphResolver;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.GetSquadResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@WorkflowImpl(taskQueues = "squad-orchestration-task-queue")
public class SquadExecutionWorkflowImpl implements SquadExecutionWorkflow {

    private final GetSquadActivity getSquadActivity = Workflow.newActivityStub(GetSquadActivity.class,
            buildActivityOptions());

    private final RunAiAgentActivity runAiAgentActivity = Workflow.newActivityStub(RunAiAgentActivity.class,
            buildActivityOptions());

    @Override
    public SquadExecutionResult execute(SquadExecutionRequest request) {
        GetSquadResult getSquadResult = getSquadActivity.getSquad(GetSquadRequest.builder()
                .squadId(request.getSquadId())
                .build());

        Squad squad = getSquadResult.getSquad();

        SquadExecutionGraph graph = SquadExecutionGraphBuilder.from(squad);
        List<List<SquadStep>> executionBatches = SquadExecutionGraphResolver.resolveExecutionBatches(graph);

        List<SquadStepExecutionResult> stepResults = new ArrayList<>();

        for (List<SquadStep> batch : executionBatches) {
            stepResults.addAll(executeBatch(squad, batch));
        }

        return SquadExecutionResult.builder().squadId(squad.getId()).status("COMPLETED")
                .message("Executed " + stepResults.size() + " squad step(s) successfully.").build();
    }

    private List<SquadStepExecutionResult> executeBatch(Squad squad, List<SquadStep> batch) {
        if (batch.size() == 1) {
            return List.of(executeSingleStep(squad, batch.get(0)));
        }

        List<Promise<SquadStepExecutionResult>> promises = new ArrayList<>();

        for (SquadStep step : batch) {
            promises.add(Async.function(() -> executeSingleStep(squad, step)));
        }

        List<SquadStepExecutionResult> results = new ArrayList<>();

        for (Promise<SquadStepExecutionResult> promise : promises) {
            results.add(promise.get());
        }

        return results;
    }

    private SquadStepExecutionResult executeSingleStep(Squad squad, SquadStep step) {
        if (!(step instanceof AiAgentStep aiAgentStep)) {
            throw ApplicationFailure.newNonRetryableFailure(
                    "Unsupported squad step type: " + step.getClass().getSimpleName(), "UNSUPPORTED_SQUAD_STEP_TYPE");
        }

        SquadStepExecutionRequest stepRequest = SquadStepExecutionRequest.builder().squadId(squad.getId())
                .stepId(aiAgentStep.getId()).stepName(aiAgentStep.getName()).agentKey(aiAgentStep.getAgentKey())
                .build();

        return runAiAgentActivity.runAiAgent(stepRequest);
    }

    private ActivityOptions buildActivityOptions() {
        RetryOptions retryOptions = RetryOptions.newBuilder().setInitialInterval(Duration.ofSeconds(1))
                .setMaximumInterval(Duration.ofSeconds(20)).setBackoffCoefficient(2).setMaximumAttempts(3).build();

        return ActivityOptions.newBuilder().setRetryOptions(retryOptions).setStartToCloseTimeout(Duration.ofSeconds(30))
                .setScheduleToCloseTimeout(Duration.ofMinutes(5)).build();
    }
}
