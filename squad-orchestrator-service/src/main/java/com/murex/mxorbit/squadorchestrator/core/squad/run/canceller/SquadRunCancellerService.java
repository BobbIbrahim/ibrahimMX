package com.murex.mxorbit.squadorchestrator.core.squad.run.canceller;

import com.murex.mxorbit.squadorchestrator.core.workflow.client.TemporalClient;
import io.temporal.client.WorkflowStub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadRunCancellerService implements SquadRunCanceller {

    private final TemporalClient temporalClient;

    @Override
    public void cancelSquadRun(String squadRunId) {
        log.info("Cancelling squad run. squadRunId: {}", squadRunId);

        WorkflowStub workflowStub = temporalClient.getWorkflowStub(squadRunId);

        workflowStub.cancel();

        log.info("Successfully cancelled squad run. squadRunId: {}", squadRunId);
    }
}