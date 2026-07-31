package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

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
public class SquadExecutionRequest {

	@NonNull
	private String squadId;

	@Builder.Default
	@NonNull
	private Map<String, Object> initialInput = new LinkedHashMap<>();
}
