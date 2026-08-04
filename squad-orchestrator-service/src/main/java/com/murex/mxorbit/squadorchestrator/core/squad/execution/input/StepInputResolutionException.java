package com.murex.mxorbit.squadorchestrator.core.squad.execution.input;

import lombok.Getter;

/**
 * Signals that a step input could not be resolved; carries the parts needed for
 * the activity failure message.
 */
@Getter
public class StepInputResolutionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final transient String stepId;
	private final transient String fromStepId;
	private final transient String key;
	private final transient String reason;

	public StepInputResolutionException(String stepId, String fromStepId, String key, String reason) {
		super(String.format("stepId=%s fromStepId=%s key=%s reason=%s", stepId, fromStepId, key, reason));
		this.stepId = stepId;
		this.fromStepId = fromStepId;
		this.key = key;
		this.reason = reason;
	}
}
