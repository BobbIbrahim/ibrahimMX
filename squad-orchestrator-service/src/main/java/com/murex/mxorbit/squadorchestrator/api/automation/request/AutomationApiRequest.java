package com.murex.mxorbit.squadorchestrator.api.automation.request;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationFrequency;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared create/update request body for the standalone Automation API.
 * Frequency-specific field combinations are validated by the existing
 * {@code AutomationValidator} in the core layer, not duplicated here with Bean
 * Validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationApiRequest {

	private String name;

	private AssigneeType assigneeType;

	private String assigneeId;

	private AutomationFrequency frequency;

	private OffsetDateTime runTime;

	private Integer weeklyDay;

	private Integer everyMinutes;

	@Builder.Default
	private Map<String, Object> input = new LinkedHashMap<>();

	private boolean startPaused;
}
