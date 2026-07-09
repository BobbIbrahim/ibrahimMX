package com.murex.mxorbit.squadorchestrator.core.squad.execution.workflow;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionStatus;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadExecutionResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SquadExecutionWorkflow {

	@WorkflowMethod
	SquadExecutionResult execute(SquadExecutionRequest request);

	@QueryMethod
	SquadExecutionStatus getExecutionStatus();
}
