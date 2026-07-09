package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadStepStatus {

	@NonNull
	private String stepId;

	@NonNull
	private String stepName;

	@NonNull
	private SquadStepExecutionStatus status;

	private String message;
}
