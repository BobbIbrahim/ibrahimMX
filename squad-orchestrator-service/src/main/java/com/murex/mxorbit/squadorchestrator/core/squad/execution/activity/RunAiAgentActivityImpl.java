package com.murex.mxorbit.squadorchestrator.core.squad.execution.activity;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionResult;
import io.temporal.spring.boot.ActivityImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ActivityImpl(taskQueues = "squad-orchestration-task-queue")
public class RunAiAgentActivityImpl implements RunAiAgentActivity {

    private static final long TEMPORARY_TEST_DELAY_MILLIS = 3000L;

    @Override
    public SquadStepExecutionResult runAiAgent(SquadStepExecutionRequest request) {
        log.info("Running AI agent. squadId: {}, stepId: {}, stepName: {}, agentKey: {}", request.getSquadId(),
                request.getStepId(), request.getStepName(), request.getAgentKey());

        waitForStatusTesting();

        return SquadStepExecutionResult.builder().stepId(request.getStepId()).status("COMPLETED")
                .message("Executed step \"" + request.getStepName() + "\" using AI agent " + request.getAgentKey())
                .build();
    }

    private void waitForStatusTesting() {
        try {
            Thread.sleep(TEMPORARY_TEST_DELAY_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI agent execution test delay was interrupted.", exception);
        }
    }
}
