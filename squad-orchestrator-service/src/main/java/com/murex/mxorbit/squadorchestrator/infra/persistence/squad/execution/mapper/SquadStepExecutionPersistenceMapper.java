package com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.mapper;

import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SaveSquadStepExecutionRequest;
import com.murex.mxorbit.squadorchestrator.core.squad.execution.model.SquadStepExecutionData;
import com.murex.mxorbit.squadorchestrator.infra.persistence.squad.execution.entity.SquadStepExecutionEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SquadStepExecutionPersistenceMapper {

	@Mapping(target = "id", source = "entityId")
	@Mapping(target = "input", source = "request.input", qualifiedByName = "nullSafeCopy")
	@Mapping(target = "output", source = "request.output", qualifiedByName = "nullSafeCopy")
	SquadStepExecutionEntity toEntity(SaveSquadStepExecutionRequest request, String entityId);

	SquadStepExecutionData toStepExecutionData(SquadStepExecutionEntity entity);

	/**
	 * The JSON columns are non-nullable, so a missing map must persist as an empty
	 * object.
	 */
	@Named("nullSafeCopy")
	default Map<String, Object> nullSafeCopy(Map<String, Object> source) {
		return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
	}
}
