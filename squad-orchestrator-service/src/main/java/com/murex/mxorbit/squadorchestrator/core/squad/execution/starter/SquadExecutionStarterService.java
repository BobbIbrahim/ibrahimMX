package com.murex.mxorbit.squadorchestrator.core.squad.execution.starter;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadExecutionStarterService implements SquadExecutionStarter {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";
	private static final String WORKFLOW_ID_PREFIX = "squad-run-";

	private final WorkflowClient workflowClient;
	private final SquadProvider squadProvider;

	@Override
	public Optional<SquadRunStartResult> startSquadRun(String squadId) {
		return squadProvider.getSquadById(squadId).map(this::startWorkflow);
	}

	private SquadRunStartResult startWorkflow(Squad squad) {
		String workflowId = WORKFLOW_ID_PREFIX + UUID.randomUUID();

		Map<String, Object> memo = Map.of(SquadRunMemoKeys.SQUAD_ID, squad.getId(), SquadRunMemoKeys.SQUAD_NAME,
				squad.getName());

		SquadExecutionWorkflow workflow = workflowClient.newWorkflowStub(SquadExecutionWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).setMemo(memo).build());

		SquadExecutionRequest request = SquadExecutionRequest.builder().squadId(squad.getId()).build();

		WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
		workflowStub.start(request);

		log.info("Started squad Temporal workflow. squadId: {}, squadRunId: {}", squad.getId(), workflowId);

		return SquadRunStartResult.builder().squadId(squad.getId()).squadRunId(workflowId).status("STARTED").build();
	}
}
