package com.murex.mxorbit.squadorchestrator.core.squad.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = StepInputRefDeserializer.class)
public class StepInputRef {

	@Builder.Default
	private StepInputRefSourceType sourceType = StepInputRefSourceType.STEP_OUTPUT;

	private String targetInput;

	private String fromStepId;

	private String key;
}
