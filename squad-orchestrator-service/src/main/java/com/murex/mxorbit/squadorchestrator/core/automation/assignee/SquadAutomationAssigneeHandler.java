package com.murex.mxorbit.squadorchestrator.core.automation.assignee;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow.SquadExecutionWorkflow;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.provider.SquadProvider;
import com.murex.mxorbit.squadorchestrator.core.squad.run.SquadRunMemoKeys;
import com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadRootInputValidator;

import io.temporal.client.WorkflowOptions;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles Autopilot automations whose assignee is a Squad. This is the only
 * place in the codebase that knows a Squad automation's Temporal task queue,
 * workflow type and memo shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SquadAutomationAssigneeHandler implements AutomationAssigneeHandler {

	private static final String TASK_QUEUE = "squad-orchestration-task-queue";
	private static final String WORKFLOW_ID_PREFIX = "squad-run-autopilot-";
	private static final String TRIGGER_TYPE_MEMO_KEY = "triggerType";
	private static final String AUTOMATION_ID_MEMO_KEY = "automationId";
	private static final String AUTOPILOT_TRIGGER_TYPE = "AUTOPILOT";

	private final SquadProvider squadProvider;
	private final SquadRootInputValidator squadRootInputValidator;

	@Override
	public AssigneeType supportedType() {
		return AssigneeType.SQUAD;
	}

	@Override
	public void validate(String assigneeId, Map<String, Object> input) {
		Squad squad = resolveSquadOrThrow(assigneeId);
		squadRootInputValidator.validate(squad, input);
	}

	@Override
	public String resolveName(String assigneeId) {
		return resolveSquadOrThrow(assigneeId).getName();
	}

	@Override
	public ScheduleActionStartWorkflow buildScheduleAction(Automation automation) {
		Squad squad = resolveSquadOrThrow(automation.getAssigneeId());

		String workflowId = WORKFLOW_ID_PREFIX + automation.getId();

		Map<String, Object> memo = new LinkedHashMap<>();
		memo.put(SquadRunMemoKeys.SQUAD_ID, squad.getId());
		memo.put(SquadRunMemoKeys.SQUAD_NAME, squad.getName());
		memo.put(TRIGGER_TYPE_MEMO_KEY, AUTOPILOT_TRIGGER_TYPE);
		memo.put(AUTOMATION_ID_MEMO_KEY, automation.getId().toString());

		WorkflowOptions workflowOptions = WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE)
				.setWorkflowId(workflowId).setMemo(memo).build();

		SquadExecutionRequest request = SquadExecutionRequest.builder().squadId(automation.getAssigneeId())
				.initialInput(automation.getInitialInput() == null ? Map.of() : automation.getInitialInput()).build();

		log.info("Building Squad Autopilot schedule action. automationId: {}, assigneeType: {}, assigneeId: {}",
				automation.getId(), automation.getAssigneeType(), automation.getAssigneeId());

		return ScheduleActionStartWorkflow.newBuilder().setWorkflowType(SquadExecutionWorkflow.class)
				.setOptions(workflowOptions).setArguments(request).build();
	}

	private Squad resolveSquadOrThrow(String squadId) {
		return squadProvider.getSquadById(squadId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Squad not found: " + squadId));
	}
}
