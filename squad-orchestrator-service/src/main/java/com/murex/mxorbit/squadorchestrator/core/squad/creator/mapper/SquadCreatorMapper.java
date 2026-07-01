package com.murex.mxorbit.squadorchestrator.core.squad.creator.mapper;

import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.AiAgentStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.CreateSquadRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadEdgeRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.creator.request.SquadStepRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SquadCreatorMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "createdAt", source = "now")
	@Mapping(target = "updatedAt", source = "now")
	@Mapping(target = "steps", ignore = true)
	@Mapping(target = "edges", ignore = true)
	Squad toSquadBase(CreateSquadRequest request, UUID id, Instant now);

	default Squad toSquad(CreateSquadRequest request) {
		Instant now = Instant.now();
		Squad squad = toSquadBase(request, UUID.randomUUID(), now);
		squad.setSteps(request.getSteps().stream().map(this::toStep).toList());
		squad.setEdges(request.getEdges().stream().map(this::toEdge).toList());
		return squad;
	}

	default SquadStep toStep(SquadStepRequest request) {
		if (request instanceof AiAgentStepRequest r) {
			return toAiAgentStep(r);
		}
		throw new IllegalArgumentException("Unknown step type: " + request.getClass().getSimpleName());
	}

	AiAgentStep toAiAgentStep(AiAgentStepRequest request);

	@Mapping(target = "steps", ignore = true)
	@Mapping(target = "edges", ignore = true)
	CreateSquadStoreRequest toCreateSquadStoreRequestBase(CreateSquadRequest request);

	default CreateSquadStoreRequest toCreateSquadStoreRequest(CreateSquadRequest request) {
		CreateSquadStoreRequest storeRequest = toCreateSquadStoreRequestBase(request);
		storeRequest.setSteps(request.getSteps().stream().map(this::toStep).toList());
		storeRequest.setEdges(request.getEdges().stream().map(this::toEdge).toList());
		return storeRequest;
	}

	@Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
	SquadEdge toEdge(SquadEdgeRequest request);
}
