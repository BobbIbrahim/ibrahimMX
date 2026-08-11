package com.murex.mxorbit.squadorchestrator.core.automation.assignee;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;

import java.util.Map;

import io.temporal.client.schedules.ScheduleActionStartWorkflow;

/**
 * Polymorphic seam so Autopilot can validate, resolve and schedule an
 * automation's assignee without any assignee-specific branching outside this
 * package.
 */
public interface AutomationAssigneeHandler {

	AssigneeType supportedType();

	/**
	 * Rejects with 404 if the assignee is gone, 400 if the input does not fit it.
	 */
	void validate(String assigneeId, Map<String, Object> input);

	String resolveName(String assigneeId);

	ScheduleActionStartWorkflow buildScheduleAction(Automation automation);
}
