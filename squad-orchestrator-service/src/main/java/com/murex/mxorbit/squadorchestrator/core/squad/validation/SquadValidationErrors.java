package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared phrasing for squad definition rejections, so messages stay identical
 * across validators.
 */
final class SquadValidationErrors {

	private SquadValidationErrors() {
	}

	static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

	static String describeStep(SquadStepRequest step) {
		if (step == null) {
			return "step";
		}

		String name = step.getName();
		if (name != null && !name.isBlank()) {
			return "'" + name.trim() + "'";
		}

		String id = step.getId();
		return "with id '" + (id == null || id.isBlank() ? "unknown" : id) + "'";
	}

	static String describeStepLabel(String stepId, Map<String, SquadStepRequest> stepMap) {
		SquadStepRequest step = stepMap.get(stepId);
		if (step == null) {
			return "Step with id '" + stepId + "'";
		}
		return "Step " + describeStep(step);
	}
}
