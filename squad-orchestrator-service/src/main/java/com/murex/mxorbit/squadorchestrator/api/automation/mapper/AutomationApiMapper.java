package com.murex.mxorbit.squadorchestrator.api.automation.mapper;

import com.murex.mxorbit.squadorchestrator.api.automation.request.AutomationApiRequest;
import com.murex.mxorbit.squadorchestrator.api.automation.response.AutomationApiResponse;
import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationDetails;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

/**
 * Pure data mapping between the standalone Automation API DTOs and the core
 * automation types. Does not validate frequency fields, resolve an assignee,
 * call Temporal, branch on {@code AssigneeType} or reference Squad types;
 * all of that stays in the core layer (AutomationValidator,
 * AutomationAssigneeHandlers, AutomationSchedulerService).
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AutomationApiMapper {

	@Mapping(target = "initialInput", source = "input", qualifiedByName = "copyOf")
	CreateAutomationRequest toCreateAutomationRequest(AutomationApiRequest request);

	@Mapping(target = "id", source = "automation.id")
	@Mapping(target = "name", source = "automation.name")
	@Mapping(target = "assigneeType", source = "automation.assigneeType")
	@Mapping(target = "assigneeId", source = "automation.assigneeId")
	@Mapping(target = "temporalScheduleId", source = "automation.temporalScheduleId")
	@Mapping(target = "frequency", source = "automation.frequency")
	@Mapping(target = "runTime", source = "automation.runTime")
	@Mapping(target = "weeklyDay", source = "automation.weeklyDay")
	@Mapping(target = "everyMinutes", source = "automation.everyMinutes")
	@Mapping(target = "input", source = "automation.initialInput", qualifiedByName = "copyOf")
	@Mapping(target = "createdAt", source = "automation.createdAt")
	@Mapping(target = "updatedAt", source = "automation.updatedAt")
	@Mapping(target = "assigneeName", source = "assigneeName")
	@Mapping(target = "status", source = "status")
	@Mapping(target = "nextRunAt", source = "nextRunAt")
	@Mapping(target = "lastRunId", source = "lastRunId")
	AutomationApiResponse toAutomationApiResponse(AutomationDetails details);

	List<AutomationApiResponse> toAutomationApiResponses(List<AutomationDetails> details);

	@Named("copyOf")
	default Map<String, Object> copyOf(Map<String, Object> input) {
		return input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
	}
}
