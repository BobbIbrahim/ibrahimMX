package com.murex.mxorbit.squadorchestrator.core.automation.updater;

import com.murex.mxorbit.squadorchestrator.core.automation.assignee.AutomationAssigneeHandlers;
import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationRunTimes;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService;
import com.murex.mxorbit.squadorchestrator.core.automation.store.AutomationStore;
import com.murex.mxorbit.squadorchestrator.core.automation.validation.AutomationValidator;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationUpdaterService implements AutomationUpdater {

	private final AutomationStore automationStore;
	private final AutomationValidator automationValidator;
	private final AutomationAssigneeHandlers automationAssigneeHandlers;
	private final AutomationSchedulerService automationSchedulerService;

	@Override
	public Automation updateAutomation(UUID automationId, CreateAutomationRequest request) {
		log.debug("Updating automation with id: {}", automationId);

		Automation existing = automationStore.findById(automationId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation not found: " + automationId));

		automationValidator.validate(request);

		Instant now = Instant.now();

		Automation updated = Automation.builder().id(existing.getId()).name(request.getName())
				.assigneeType(request.getAssigneeType()).assigneeId(request.getAssigneeId())
				.temporalScheduleId(existing.getTemporalScheduleId()).frequency(request.getFrequency())
				.runTime(AutomationRunTimes.anchorToUtc(request.getRunTime())).weeklyDay(request.getWeeklyDay())
				.everyMinutes(request.getEveryMinutes()).initialInput(copyOf(request.getInitialInput()))
				.createdAt(existing.getCreatedAt()).updatedAt(now).build();

		// Validate the assignee/input generically (no assignee-type branching)
		// before the database write, so an invalid update is rejected before it
		// ever reaches the store. AutomationSchedulerService.updateSchedule
		// necessarily re-validates while building the schedule action, but that
		// is just an internal safety re-check, not a second source of truth.
		automationAssigneeHandlers.getHandler(updated.getAssigneeType()).validate(updated.getAssigneeId(),
				updated.getInitialInput());

		Automation saved = automationStore.update(automationId, updated).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation not found: " + automationId));

		try {
			automationSchedulerService.updateSchedule(saved);
		} catch (RuntimeException temporalException) {
			log.error(
					"Temporal schedule update failed after the automation database update already succeeded. automationId: {}, temporalScheduleId: {}, operation: updateAutomation",
					automationId, saved.getTemporalScheduleId(), temporalException);
			throw temporalException;
		}

		log.info("Automation updated. automationId: {}", automationId);
		return saved;
	}

	private static Map<String, Object> copyOf(Map<String, Object> input) {
		return input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
	}
}
