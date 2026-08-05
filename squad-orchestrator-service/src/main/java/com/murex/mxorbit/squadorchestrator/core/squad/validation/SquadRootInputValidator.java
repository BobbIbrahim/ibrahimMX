package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.badRequest;

import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRefSourceType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Checks that a caller supplied every manual input the squad's root step declares.
 * Shared by manual runs and by scheduled automations.
 */
@Service
public class SquadRootInputValidator {

	public void validate(Squad squad, Map<String, Object> initialInput) {
		findRootStep(squad).ifPresent(rootStep -> validateRootStep(rootStep, initialInput));
	}

	/** A root step is never the target of an edge. */
	public Optional<SquadStep> findRootStep(Squad squad) {
		if (squad == null || squad.getSteps() == null || squad.getSteps().isEmpty()) {
			return Optional.empty();
		}

		Set<String> targetStepIds = squad.getEdges() == null
				? Set.of()
				: squad.getEdges().stream().map(edge -> edge.getTargetStepId()).collect(Collectors.toSet());

		return squad.getSteps().stream().filter(step -> !targetStepIds.contains(step.getId())).findFirst();
	}

	private static void validateRootStep(SquadStep rootStep, Map<String, Object> initialInput) {
		if (rootStep.getInputRefs() == null) {
			return;
		}

		Map<String, Object> inputMap = initialInput == null ? Map.of() : initialInput;

		rootStep.getInputRefs().stream().filter(ref -> ref.getSourceType() == StepInputRefSourceType.MANUAL)
				.map(StepInputRef::getTargetInput).filter(targetInput -> !inputMap.containsKey(targetInput))
				.findFirst().ifPresent(missingInput -> {
					throw badRequest(
							"Required manual input '" + missingInput + "' is missing from the run request.");
				});
	}
}
