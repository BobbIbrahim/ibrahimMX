package com.murex.mxorbit.squadorchestrator.core.automation.creator.request;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationFrequency;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAutomationRequest {

	private String name;

	private AssigneeType assigneeType;

	private String assigneeId;

	private AutomationFrequency frequency;

	private OffsetDateTime runTime;

	private Integer weeklyDay;

	private Integer everyMinutes;

	private Map<String, Object> initialInput;

	private boolean startPaused;
}
