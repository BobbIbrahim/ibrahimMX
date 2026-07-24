package com.murex.mxorbit.squadorchestrator.core.squad.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepInputRef {

	@NonNull
	private String fromStepId;

	@NonNull
	private String key;

	private String targetInput;
}
