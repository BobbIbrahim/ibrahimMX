package com.murex.mxorbit.squadorchestrator.core.automation.deleter;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;

import java.util.UUID;

public interface AutomationDeleter {

	boolean deleteAutomation(UUID automationId);

	void deleteByAssignee(AssigneeType assigneeType, String assigneeId);
}
