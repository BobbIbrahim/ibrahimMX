package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.badRequest;
import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.describeStep;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadStepValidator {

	private final AgentRegistry agentRegistry;

	/** Returns the steps indexed by id, preserving declaration order. */
	public Map<String, SquadStepRequest> validate(List<SquadStepRequest> steps) {
		Map<String, SquadStepRequest> stepMap = new LinkedHashMap<>();
		for (SquadStepRequest step : steps) {
			if (step == null) {
				throw badRequest("The workflow contains an invalid step.");
			}

			String stepId = step.getId();
			if (stepId == null || stepId.isBlank()) {
				throw badRequest("A workflow step must have a nonblank id.");
			}

			if (step.getName() == null || step.getName().isBlank()) {
				throw badRequest("Step " + describeStep(step) + " must have a nonblank name.");
			}

			String agentKey = resolveAgentKey(step);
			if (agentKey == null || agentKey.isBlank()) {
				throw badRequest("Step " + describeStep(step) + " must have an assigned agent.");
			}

			if (agentRegistry.findByKey(agentKey).isEmpty()) {
				throw badRequest("Step " + describeStep(step) + " references unknown agent '" + agentKey + "'.");
			}

			SquadStepRequest previous = stepMap.put(stepId, step);
			if (previous != null) {
				throw badRequest("The workflow contains duplicate step id '" + stepId + "'.");
			}
		}

		return stepMap;
	}

	private static String resolveAgentKey(SquadStepRequest step) {
		if (step instanceof AiAgentStepRequest aiAgentStepRequest) {
			return aiAgentStepRequest.getAgentKey();
		}
		return null;
	}
}
