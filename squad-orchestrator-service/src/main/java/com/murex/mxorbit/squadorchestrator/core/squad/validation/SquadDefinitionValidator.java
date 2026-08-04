package com.murex.mxorbit.squadorchestrator.core.squad.validation;

import static com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadValidationErrors.badRequest;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.validation.SquadTopologyValidator.SquadTopology;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Rejects a squad definition before it is persisted; each rule set lives in its
 * own validator.
 */
@Service
@RequiredArgsConstructor
public class SquadDefinitionValidator {

	private static final int MINIMUM_STEP_COUNT = 2;

	private final SquadStepValidator squadStepValidator;
	private final SquadTopologyValidator squadTopologyValidator;
	private final SquadEdgeRoutingValidator squadEdgeRoutingValidator;
	private final SquadInputRefValidator squadInputRefValidator;

	public void validate(CreateSquadRequest request) {
		List<SquadStepRequest> steps = request.getSteps() == null ? List.of() : request.getSteps();
		List<SquadEdgeRequest> edges = request.getEdges() == null ? List.of() : request.getEdges();

		if (steps.size() < MINIMUM_STEP_COUNT) {
			throw badRequest("A workflow must contain at least two steps.");
		}

		Map<String, SquadStepRequest> stepMap = squadStepValidator.validate(steps);
		SquadTopology topology = squadTopologyValidator.build(edges, stepMap);

		squadEdgeRoutingValidator.validate(edges);
		squadTopologyValidator.validateStructure(topology);
		squadInputRefValidator.validate(steps, topology);
	}
}
