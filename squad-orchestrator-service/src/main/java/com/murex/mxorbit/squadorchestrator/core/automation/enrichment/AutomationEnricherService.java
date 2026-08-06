package com.murex.mxorbit.squadorchestrator.core.automation.enrichment;

import com.murex.mxorbit.squadorchestrator.core.automation.assignee.AutomationAssigneeHandler;
import com.murex.mxorbit.squadorchestrator.core.automation.assignee.AutomationAssigneeHandlers;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationDetails;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationStatus;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationScheduleDescription;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single, reusable place that turns a stored {@link Automation} into an
 * {@link AutomationDetails} by resolving its assignee name through
 * {@link AutomationAssigneeHandlers} and its live schedule state through
 * {@link AutomationSchedulerService}. Never branches on assignee type and
 * never references Squad-specific types, so the same logic serves get, list,
 * create, update, pause and resume without duplication. Nothing computed
 * here is persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationEnricherService {

	private final AutomationAssigneeHandlers automationAssigneeHandlers;
	private final AutomationSchedulerService automationSchedulerService;

	public AutomationDetails enrich(Automation automation) {
		log.debug("Enriching automation. automationId: {}, temporalScheduleId: {}", automation.getId(),
				automation.getTemporalScheduleId());

		AutomationAssigneeHandler handler = automationAssigneeHandlers.getHandler(automation.getAssigneeType());
		String assigneeName = handler.resolveName(automation.getAssigneeId());

		AutomationScheduleDescription description = automationSchedulerService
				.describeSchedule(automation.getTemporalScheduleId());

		AutomationStatus status = description.isPaused() ? AutomationStatus.PAUSED : AutomationStatus.ACTIVE;

		return AutomationDetails.builder().automation(automation).assigneeName(assigneeName).status(status)
				.nextRunAt(description.getNextRunAt()).lastRunId(description.getLastRunId()).build();
	}

	public List<AutomationDetails> enrichAll(List<Automation> automations) {
		return automations.stream().map(this::enrich).toList();
	}
}
