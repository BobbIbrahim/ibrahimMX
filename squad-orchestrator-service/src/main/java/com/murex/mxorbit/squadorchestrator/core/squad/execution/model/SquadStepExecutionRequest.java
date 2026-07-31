package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
public class SquadStepExecutionRequest {

	@NonNull
	private String squadId;

	@NonNull
	private String stepId;

	@NonNull
	private String stepName;

	@NonNull
	private String agentKey;

	@Builder.Default
	@NonNull
	private List<StepInputRef> inputRefs = new ArrayList<>();

	@Builder.Default
	@NonNull
	private Map<String, Map<String, Object>> stepOutputsByStepId = new LinkedHashMap<>();

	@Builder.Default
	@NonNull
	private Map<String, Object> seedInput = new LinkedHashMap<>();
}
