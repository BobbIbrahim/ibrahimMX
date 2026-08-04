package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.badRequest;
import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.describeStep;
import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.describeStepLabel;

import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentDefinition;
import com.murex.mxorbit.squadorchestrator.core.squad.agent.AgentRegistry;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRefSourceType;
import com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadTopologyValidator.SquadTopology;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SquadInputRefValidator {

	private static final String NULL_BYTE_SEPARATOR = "\u0000";

	private final AgentRegistry agentRegistry;

	public void validate(List<SquadStepRequest> steps, SquadTopology topology) {
		for (SquadStepRequest step : steps) {
			validateStepInputRefs(step, topology);
		}
	}

	private void validateStepInputRefs(SquadStepRequest step, SquadTopology topology) {
		List<StepInputRef> inputRefs = step.getInputRefs();
		if (inputRefs == null || inputRefs.isEmpty()) {
			return;
		}

		Set<String> ancestors = topology.ancestorsOf(step.getId());
		Set<String> seenRefs = new HashSet<>();
		Set<String> seenTargetInputs = new HashSet<>();

		for (StepInputRef inputRef : inputRefs) {
			validateInputRef(step, inputRef, topology.stepMap(), ancestors, seenRefs, seenTargetInputs,
					ancestors.isEmpty());
		}
	}

	private void validateInputRef(SquadStepRequest step, StepInputRef ref, Map<String, SquadStepRequest> stepMap,
			Set<String> ancestors, Set<String> seenRefs, Set<String> seenTargetInputs, boolean stepIsRoot) {
		if (ref == null) {
			throw badRequest("Step " + describeStep(step) + " has a null inputRef.");
		}

		StepInputRefSourceType sourceType = ref.getSourceType();
		if (sourceType == null) {
			throw badRequest("Step " + describeStep(step) + " inputRef must have a sourceType.");
		}

		String targetInput = ref.getTargetInput();
		if (targetInput == null || targetInput.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef must have a targetInput.");
		}

		validateTargetInput(step, requireAgentDefinition(step), targetInput);

		if (!seenTargetInputs.add(targetInput)) {
			throw badRequest(
					"Step " + describeStep(step) + " has a duplicate inputRef target input '" + targetInput + "'.");
		}

		switch (sourceType) {
			case MANUAL :
				validateManualInputRef(step, ref, stepIsRoot);
				break;
			case STEP_OUTPUT :
				validateStepOutputInputRef(step, ref, stepMap, ancestors, seenRefs);
				break;
			default :
				throw badRequest("Step " + describeStep(step) + " inputRef has unknown sourceType: " + sourceType);
		}
	}

	private void validateManualInputRef(SquadStepRequest step, StepInputRef ref, boolean stepIsRoot) {
		if (!stepIsRoot) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType MANUAL is only allowed on root steps.");
		}

		if (ref.getFromStepId() != null && !ref.getFromStepId().isBlank()) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType MANUAL must not have fromStepId.");
		}

		if (ref.getKey() != null && !ref.getKey().isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef with sourceType MANUAL must not have key.");
		}
	}

	private void validateStepOutputInputRef(SquadStepRequest step, StepInputRef ref,
			Map<String, SquadStepRequest> stepMap, Set<String> ancestors, Set<String> seenRefs) {
		String fromStepId = ref.getFromStepId();
		String outputKey = ref.getKey();

		if (fromStepId == null || fromStepId.isBlank()) {
			throw badRequest(
					"Step " + describeStep(step) + " inputRef with sourceType STEP_OUTPUT must have fromStepId.");
		}

		if (outputKey == null || outputKey.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " inputRef with sourceType STEP_OUTPUT must have key.");
		}

		if (!stepMap.containsKey(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " references unknown source step "
					+ describeStepLabel(fromStepId, stepMap) + " in an inputRef.");
		}

		if (step.getId().equals(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " cannot reference itself in an inputRef.");
		}

		if (!ancestors.contains(fromStepId)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " must reference an upstream ancestor.");
		}

		if (!seenRefs.add(fromStepId + NULL_BYTE_SEPARATOR + outputKey)) {
			throw badRequest("Step " + describeStep(step) + " has a duplicate inputRef from "
					+ describeStepLabel(fromStepId, stepMap) + " using output key '" + outputKey + "'.");
		}

		validateInputRefOutput(step, fromStepId, outputKey, stepMap);
	}

	private AgentDefinition requireAgentDefinition(SquadStepRequest step) {
		if (!(step instanceof AiAgentStepRequest aiAgentStepRequest)) {
			throw badRequest("Step " + describeStep(step) + " must be an AI-agent step with an assigned agent.");
		}

		String agentKey = aiAgentStepRequest.getAgentKey();
		if (agentKey == null || agentKey.isBlank()) {
			throw badRequest("Step " + describeStep(step) + " must be an AI-agent step with an assigned agent.");
		}

		return agentRegistry.findByKey(agentKey).orElseThrow(
				() -> badRequest("Step " + describeStep(step) + " references unknown agent '" + agentKey + "'."));
	}

	private void validateTargetInput(SquadStepRequest step, AgentDefinition agentDefinition, String targetInput) {
		if (!agentDefinition.getInputs().contains(targetInput)) {
			throw badRequest("Step " + describeStep(step) + " inputRef target input '" + targetInput
					+ "' is not declared by agent '" + agentDefinition.getName() + "'.");
		}
	}

	private void validateInputRefOutput(SquadStepRequest step, String fromStepId, String outputKey,
			Map<String, SquadStepRequest> stepMap) {
		SquadStepRequest sourceStep = stepMap.get(fromStepId);
		if (!(sourceStep instanceof AiAgentStepRequest aiAgentStepRequest)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " must reference a step assigned to an agent.");
		}

		String sourceAgentKey = aiAgentStepRequest.getAgentKey();
		List<String> outputs = agentRegistry.findByKey(sourceAgentKey).map(AgentDefinition::getOutputs)
				.orElseThrow(() -> badRequest(
						"Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
								+ " references unknown agent '" + sourceAgentKey + "'."));

		if (!outputs.contains(outputKey)) {
			throw badRequest("Step " + describeStep(step) + " inputRef from " + describeStepLabel(fromStepId, stepMap)
					+ " references undeclared output key '" + outputKey + "'.");
		}
	}
}
