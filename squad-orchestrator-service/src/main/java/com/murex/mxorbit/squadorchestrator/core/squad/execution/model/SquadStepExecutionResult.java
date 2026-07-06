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
public class SquadStepExecutionResult {

	@NonNull
	private String stepId;

	@NonNull
	private String status;

	@NonNull
	private String message;
}
