package com.murex.mxorbit.squadorchestrator.core.squad.execution.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SquadStepExecutionData {

	private String stepId;

	private String stepName;

	private Map<String, Object> input;

	private Map<String, Object> output;
}
