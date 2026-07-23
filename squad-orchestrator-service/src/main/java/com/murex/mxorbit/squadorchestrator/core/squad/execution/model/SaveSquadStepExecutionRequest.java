package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import java.time.Instant;
import java.util.LinkedHashMap;
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
public class SaveSquadStepExecutionRequest {

	@NonNull
	private String squadRunId;

	@NonNull
	private String squadId;

	@NonNull
	private String stepId;

	@NonNull
	private String stepName;

	@NonNull
	private String status;

	private String message;

	private Instant startedAt;

	private Instant completedAt;

	private Long durationMs;

	@Builder.Default
	@NonNull
	private Map<String, Object> input = new LinkedHashMap<>();

	@Builder.Default
	@NonNull
	private Map<String, Object> output = new LinkedHashMap<>();
}
