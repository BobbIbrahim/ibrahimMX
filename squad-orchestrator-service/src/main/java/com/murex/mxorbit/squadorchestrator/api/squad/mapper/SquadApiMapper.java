package com.murex.mxorbit.squadorchestrator.api.squad.mapper;

import com.murex.mxorbit.squadorchestrator.api.squad.request.AiAgentStepApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.request.CreateSquadApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.request.SquadEdgeApiRequest;
import com.murex.mxorbit.squadorchestrator.api.squad.response.AiAgentStepApiResponse;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadApiResponse;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadEdgeApiResponse;
import com.murex.mxorbit.squadorchestrator.api.squad.response.SquadStepApiResponse;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.*;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SquadApiMapper {

	CreateSquadRequest toSquadCreateRequest(CreateSquadApiRequest request);

	default SquadStepRequest toSquadStepRequest(
			com.murex.mxorbit.squadorchestrator.api.squad.request.SquadStepApiRequest request) {
		if (request instanceof AiAgentStepApiRequest aiAgentRequest) {
			return toAiAgentStepRequest(aiAgentRequest);
		}
		throw new IllegalArgumentException("Unknown API step request type: " + request.getClass().getSimpleName());
	}

	AiAgentStepRequest toAiAgentStepRequest(AiAgentStepApiRequest request);

	@AfterMapping
	default void ensureInputRefs(AiAgentStepApiRequest request, @MappingTarget AiAgentStepRequest stepRequest) {
		if (stepRequest.getInputRefs() == null) {
			stepRequest.setInputRefs(new ArrayList<>());
		}
	}

	SquadEdgeRequest toSquadEdgeRequest(SquadEdgeApiRequest request);

	@Mapping(target = "steps", source = "steps")
	@Mapping(target = "edges", source = "edges")
	SquadApiResponse toSquadApiResponse(Squad squad);

	List<SquadApiResponse> toSquadApiResponses(List<Squad> squads);

	default SquadStepApiResponse toSquadStepApiResponse(SquadStep step) {
		if (step instanceof AiAgentStep aiAgentStep) {
			return toAiAgentStepApiResponse(aiAgentStep);
		}
		throw new IllegalArgumentException("Unknown step type: " + step.getClass().getSimpleName());
	}

	AiAgentStepApiResponse toAiAgentStepApiResponse(AiAgentStep step);

	@AfterMapping
	default void ensureInputRefs(AiAgentStep step, @MappingTarget AiAgentStepApiResponse stepResponse) {
		if (stepResponse.getInputRefs() == null) {
			stepResponse.setInputRefs(new ArrayList<>());
		}
	}

	SquadEdgeApiResponse toSquadEdgeApiResponse(SquadEdge edge);

}
