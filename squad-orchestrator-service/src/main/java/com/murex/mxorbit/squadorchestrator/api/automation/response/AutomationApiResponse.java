package com.murex.mxorbit.squadorchestrator.api.automation.response;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationFrequency;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationStatus;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat, read-only view of an {@code AutomationDetails}: the persisted
 * automation fields alongside its runtime enrichment (assignee name,
 * schedule status, next run and last run id). There is no nested
 * "automation" object in the JSON payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationApiResponse {

	private UUID id;

	private String name;

	private AssigneeType assigneeType;

	private String assigneeId;

	private String temporalScheduleId;

	private AutomationFrequency frequency;

	private LocalTime runTime;

	private Integer weeklyDay;

	private Integer everyMinutes;

	private Map<String, Object> input;

	private Instant createdAt;

	private Instant updatedAt;

	private String assigneeName;

	private AutomationStatus status;

	private Instant nextRunAt;

	private String lastRunId;
}
