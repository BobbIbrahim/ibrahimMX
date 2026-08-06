package com.murex.mxorbit.squadorchestrator.core.automation.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Automation {

	@NonNull
	private UUID id;

	@NonNull
	private String name;

	@NonNull
	private AssigneeType assigneeType;

	@NonNull
	private String assigneeId;

	@NonNull
	private String temporalScheduleId;

	@NonNull
	private AutomationFrequency frequency;

	private LocalTime runTime;

	private Integer weeklyDay;

	private Integer everyMinutes;

	private Map<String, Object> initialInput;

	@NonNull
	private Instant createdAt;

	@NonNull
	private Instant updatedAt;
}
