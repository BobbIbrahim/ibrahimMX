package com.murex.mxorbit.squadorchestrator.infra.persistence.automation.mapper;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.infra.persistence.automation.entity.AutomationEntity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AutomationPersistenceMapper {

	@Mapping(target = "initialInput", source = "initialInput", qualifiedByName = "nullSafeCopy")
	AutomationEntity toEntity(Automation automation);

	Automation toAutomation(AutomationEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "temporalScheduleId", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "initialInput", source = "initialInput", qualifiedByName = "nullSafeCopy")
	void updateEntity(Automation automation, @MappingTarget AutomationEntity entity);

	/**
	 * The JSON column is non-nullable, so a missing map must persist as an empty
	 * object.
	 */
	@Named("nullSafeCopy")
	default Map<String, Object> nullSafeCopy(Map<String, Object> source) {
		return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
	}
}
