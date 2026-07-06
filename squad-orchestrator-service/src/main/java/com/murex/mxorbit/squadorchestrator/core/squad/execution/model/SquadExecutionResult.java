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
public class SquadExecutionResult {

	@NonNull
	private String squadId;

	@NonNull
	private String status;

	@NonNull
	private String message;
}
