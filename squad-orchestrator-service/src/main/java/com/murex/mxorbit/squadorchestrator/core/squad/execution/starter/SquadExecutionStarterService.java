package com.murex.mxorbit.squadorchestrator.core.squad.execution.starter;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadRunStartResult;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRefSourceType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SquadExecutionStarterService implements SquadExecutionStarter {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";
	private static final String WORKFLOW_ID_PREFIX = "squad-run-";

	private final WorkflowClient workflowClient;
	private final SquadProvider squadProvider;

	@Override
	public Optional<SquadRunStartResult> startSquadRun(String squadId, Map<String, Object> initialInput) {
		return squadProvider.getSquadById(squadId).map(squad -> startWorkflow(squad, initialInput));
	}

	private SquadRunStartResult startWorkflow(Squad squad, Map<String, Object> initialInput) {
		validateManualInputs(squad, initialInput);

		String workflowId = WORKFLOW_ID_PREFIX + UUID.randomUUID();

		Map<String, Object> memo = Map.of(SquadRunMemoKeys.SQUAD_ID, squad.getId(), SquadRunMemoKeys.SQUAD_NAME,
				squad.getName());

		SquadExecutionWorkflow workflow = workflowClient.newWorkflowStub(SquadExecutionWorkflow.class,
				WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(workflowId).setMemo(memo).build());

		SquadExecutionRequest request = SquadExecutionRequest.builder().squadId(squad.getId())
				.initialInput(initialInput == null ? Map.of() : initialInput).build();

		WorkflowStub workflowStub = WorkflowStub.fromTyped(workflow);
		workflowStub.start(request);

		log.info("Started squad Temporal workflow. squadId: {}, squadRunId: {}", squad.getId(), workflowId);

		return SquadRunStartResult.builder().squadId(squad.getId()).squadRunId(workflowId).status("STARTED").build();
	}

	private void validateManualInputs(Squad squad, Map<String, Object> initialInput) {
		if (squad.getSteps() == null || squad.getSteps().isEmpty()) {
			return;
		}

		// Find the root step (step with no incoming edges)
		Set<String> sourceStepIds = squad.getEdges().stream().map(e -> e.getSourceStepId()).collect(Collectors.toSet());
		Optional<SquadStep> rootStepOpt = squad.getSteps().stream()
				.filter(step -> !sourceStepIds.contains(step.getId())).findFirst();

		if (rootStepOpt.isEmpty()) {
			return;
		}

		SquadStep rootStep = rootStepOpt.get();
		List<StepInputRef> manualInputRefs = rootStep.getInputRefs().stream()
				.filter(ref -> ref.getSourceType() == StepInputRefSourceType.MANUAL).collect(Collectors.toList());

		Map<String, Object> inputMap = initialInput == null ? Map.of() : initialInput;

		for (StepInputRef manualRef : manualInputRefs) {
			String targetInput = manualRef.getTargetInput();
			if (!inputMap.containsKey(targetInput)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Required manual input '" + targetInput + "' is missing from the run request.");
			}
		}
	}
}
