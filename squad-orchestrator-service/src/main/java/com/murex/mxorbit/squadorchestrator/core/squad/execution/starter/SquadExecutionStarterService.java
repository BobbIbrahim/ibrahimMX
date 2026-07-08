package com.murex.mxorbit.squadorchestrator.core.squad.execution.starter;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.run.creator.SquadRunCreator;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadExecutionStarterService implements SquadExecutionStarter {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";

	private final WorkflowClient workflowClient;
	private final SquadRunCreator squadRunCreator;

	@Override
	public Optional<SquadRunStartResult> startSquadRun(String squadId) {
		return Optional.of(startWorkflow(squadId));
	}

	private SquadRunStartResult startWorkflow(String squadId) {
		String workflowId = "squad-" + squadId + "-run-" + System.currentTimeMillis();

		SquadExecutionWorkflow workflow = workflowClient.newWorkflowStub(SquadExecutionWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).build());

		SquadExecutionRequest request = SquadExecutionRequest.builder().squadId(squadId).build();

		WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
		workflowStub.start(request);

		String runId = workflowStub.getExecution().getRunId();

		squadRunCreator.createSquadRun(squadId, workflowId, runId);

		log.info("Started squad Temporal workflow. squadId: {}, workflowId: {}, runId: {}", squadId, workflowId, runId);

		return SquadRunStartResult.builder().squadId(squadId).workflowId(workflowId).runId(runId).status("STARTED")
				.build();
	}
}
