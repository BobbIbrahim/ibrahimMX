package com.murex.mxorbit.squadorchestrator.core.automation.creator;

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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationCreatorService implements AutomationCreator {

	private static final String SCHEDULE_ID_PREFIX = "mxorbit-autopilot-";

	private final AutomationStore automationStore;
	private final AutomationValidator automationValidator;
	private final AutomationSchedulerService automationSchedulerService;

	@Override
	public Automation createAutomation(CreateAutomationRequest request) {
		automationValidator.validate(request);
		log.debug("Creating automation with name: {}, assigneeType: {}, assigneeId: {}", request.getName(),
				request.getAssigneeType(), request.getAssigneeId());

		UUID id = UUID.randomUUID();
		String temporalScheduleId = SCHEDULE_ID_PREFIX + id;
		Instant now = Instant.now();

		Automation automation = Automation.builder().id(id).name(request.getName())
				.assigneeType(request.getAssigneeType()).assigneeId(request.getAssigneeId())
				.temporalScheduleId(temporalScheduleId).frequency(request.getFrequency())
				.runTime(AutomationRunTimes.anchorToUtc(request.getRunTime())).weeklyDay(request.getWeeklyDay())
				.everyMinutes(request.getEveryMinutes()).initialInput(copyOf(request.getInitialInput()))
				.createdAt(now).updatedAt(now).build();

		// Temporal Schedule is created first; the database row is only persisted
		// once the schedule exists. If persistence then fails, the schedule is
		// compensated (deleted) so a partially created automation never lingers.
		automationSchedulerService.createSchedule(automation, request.isStartPaused());

		Automation saved;
		try {
			saved = automationStore.save(automation);
		} catch (RuntimeException persistenceException) {
			log.error(
					"Automation persistence failed after Temporal schedule was created; compensating by deleting the schedule. automationId: {}, temporalScheduleId: {}, operation: createAutomation",
					id, temporalScheduleId, persistenceException);
			try {
				automationSchedulerService.deleteSchedule(temporalScheduleId);
			} catch (RuntimeException compensationException) {
				log.error(
						"Temporal schedule compensation delete also failed after automation persistence failure. automationId: {}, temporalScheduleId: {}, operation: createAutomation",
						id, temporalScheduleId, compensationException);
				persistenceException.addSuppressed(compensationException);
			}
			throw persistenceException;
		}

		log.info("Automation created. automationId: {}, temporalScheduleId: {}", saved.getId(),
				saved.getTemporalScheduleId());
		return saved;
	}

	private static Map<String, Object> copyOf(Map<String, Object> input) {
		return input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
	}
}
