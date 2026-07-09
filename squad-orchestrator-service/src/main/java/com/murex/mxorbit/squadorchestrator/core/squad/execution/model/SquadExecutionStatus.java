package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import com.murex.mxorbit.squadorchestrator.core.workflow.client.WorkflowRunStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadExecutionStatus {

	@NonNull
	private String squadId;

	@NonNull
	private WorkflowRunStatus overallStatus;

	@NonNull
	private List<SquadStepStatus> steps;
}
