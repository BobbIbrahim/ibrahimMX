package com.murex.mxorbit.squadorchestrator.core.squad.execution.input;

import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRefSourceType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single definition of how a step's input is built from upstream outputs. The
 * workflow previews the same mapping it later executes, so reported input
 * always matches what the agent receives.
 */
public final class StepInputResolver {

	private StepInputResolver() {
	}

	/** Fails on any unresolved reference. Used when actually invoking the agent. */
	public static Map<String, Object> resolveStrict(String stepId, List<StepInputRef> inputRefs,
			Map<String, Map<String, Object>> stepOutputsByStepId, Map<String, Object> seedInput) {
		return resolve(stepId, inputRefs, stepOutputsByStepId, seedInput, true);
	}

	/**
	 * Substitutes null for unresolved references. Used to report in-progress status
	 * before the agent runs.
	 */
	public static Map<String, Object> resolveLenient(String stepId, List<StepInputRef> inputRefs,
			Map<String, Map<String, Object>> stepOutputsByStepId, Map<String, Object> seedInput) {
		return resolve(stepId, inputRefs, stepOutputsByStepId, seedInput, false);
	}

	private static Map<String, Object> resolve(String stepId, List<StepInputRef> inputRefs,
			Map<String, Map<String, Object>> stepOutputsByStepId, Map<String, Object> seedInput, boolean strict) {
		Map<String, Object> input = new LinkedHashMap<>();
		if (seedInput != null) {
			input.putAll(seedInput);
		}

		if (inputRefs == null || inputRefs.isEmpty()) {
			return input;
		}

		Map<String, Map<String, Object>> stepOutputs = stepOutputsByStepId == null ? Map.of() : stepOutputsByStepId;

		for (StepInputRef inputRef : inputRefs) {
			if (inputRef == null || inputRef.getSourceType() == StepInputRefSourceType.MANUAL) {
				continue;
			}

			resolveInputRef(stepId, inputRef, stepOutputs, input, strict);
		}

		return input;
	}

	private static void resolveInputRef(String stepId, StepInputRef inputRef,
			Map<String, Map<String, Object>> stepOutputs, Map<String, Object> input, boolean strict) {
		String targetInput = inputRef.getTargetInput();
		String fromStepId = inputRef.getFromStepId();
		String key = inputRef.getKey();

		if (targetInput == null || targetInput.isBlank()) {
			if (strict) {
				throw new StepInputResolutionException(stepId, fromStepId, key, "target-input-missing");
			}
			return;
		}

		Map<String, Object> fromStepOutput = stepOutputs.get(fromStepId);
		if (fromStepOutput == null) {
			failOrSkip(strict, stepId, fromStepId, key, "output-missing", targetInput, input);
			return;
		}

		if (!fromStepOutput.containsKey(key)) {
			failOrSkip(strict, stepId, fromStepId, key, "key-missing", targetInput, input);
			return;
		}

		if (strict && input.containsKey(targetInput)) {
			throw new StepInputResolutionException(stepId, fromStepId, key, "duplicate-target-input");
		}

		input.put(targetInput, fromStepOutput.get(key));
	}

	private static void failOrSkip(boolean strict, String stepId, String fromStepId, String key, String reason,
			String targetInput, Map<String, Object> input) {
		if (strict) {
			throw new StepInputResolutionException(stepId, fromStepId, key, reason);
		}
		input.put(targetInput, null);
	}
}
