package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

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

	private Instant startedAt;

	private Instant completedAt;

	private Long durationMs;

	@Schema(description = "Step input parameters as a JSON object (Map<String, Object>)")
	private Map<String, Object> input;

	@Schema(description = "Step output result as a JSON object (Map<String, Object>)")
	private Map<String, Object> output;
}
