package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.mapper;

import com.murex.mxorbit.squadorchestrator.core.squad.model.AiAgentStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.Squad;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadEdge;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStep;
import com.murex.mxorbit.squadorchestrator.core.squad.model.SquadStepType;
import com.murex.mxorbit.squadorchestrator.core.squad.model.StepInputRef;
import com.murex.mxorbit.squadorchestrator.core.squad.store.request.CreateSquadStoreRequest;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEdgeEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadEntity;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.entity.SquadStepEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SquadPersistenceMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "steps", ignore = true)
	@Mapping(target = "edges", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	SquadEntity toSquadEntity(CreateSquadStoreRequest request);

	Squad toSquad(SquadEntity entity);

	@AfterMapping
	default void linkChildren(CreateSquadStoreRequest request, @MappingTarget SquadEntity entity) {
		Instant now = Instant.now();
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);
		request.getSteps().stream().map(step -> buildStepEntity(step, entity.getId())).forEach(entity::addStep);
		request.getEdges().stream().map(this::toEdgeEntity).forEach(entity::addEdge);
	}

	@Mapping(target = "id", source = "step.id")
	@Mapping(target = "squad", ignore = true)
	@Mapping(target = "type", ignore = true)
	@Mapping(target = "config", ignore = true)
	SquadStepEntity toStepEntity(SquadStep step, String squadId);

	default SquadStepEntity buildStepEntity(SquadStep step, String squadId) {
		SquadStepEntity entity = toStepEntity(step, squadId);
		if (step instanceof AiAgentStep aiAgentStep) {
			entity.setType(SquadStepType.AI_AGENT);
			entity.setConfig(java.util.Map.of("agentKey", aiAgentStep.getAgentKey()));
			return entity;
		}

		throw new IllegalArgumentException("Unsupported squad step type: " + step.getClass().getSimpleName());
	}

	@Mapping(target = "squad", ignore = true)
	@Mapping(target = "routingType", source = "routingType", defaultValue = "ALWAYS")
	@Mapping(target = "priority", source = "priority", defaultValue = "100")
	@Mapping(target = "isDefault", source = "isDefault", defaultValue = "false")
	SquadEdgeEntity toEdgeEntity(SquadEdge edge);

	default SquadStep toStep(SquadStepEntity entity) {
		List<StepInputRef> inputRefs = entity.getInputRefs();
		if (inputRefs == null) {
			inputRefs = new ArrayList<>();
		}

		return switch (entity.getType()) {
			case AI_AGENT ->
				AiAgentStep.builder().id(entity.getId()).name(entity.getName()).inputRefs(new ArrayList<>(inputRefs))
						.agentKey((String) entity.getConfig().get("agentKey")).build();
		};
	}

	@Mapping(target = "routingType", source = "routingType", defaultValue = "ALWAYS")
	@Mapping(target = "priority", source = "priority", defaultValue = "100")
	@Mapping(target = "isDefault", source = "isDefault", defaultValue = "false")
	SquadEdge toEdge(SquadEdgeEntity entity);
}
