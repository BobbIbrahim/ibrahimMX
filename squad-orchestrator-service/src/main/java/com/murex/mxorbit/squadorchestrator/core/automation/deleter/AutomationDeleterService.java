package com.murex.mxorbit.squadorchestrator.core.automation.deleter;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService;
import com.murex.mxorbit.squadorchestrator.core.automation.store.AutomationStore;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationDeleterService implements AutomationDeleter {

	private final AutomationStore automationStore;
	private final AutomationSchedulerService automationSchedulerService;

	@Override
	public boolean deleteAutomation(UUID automationId) {
		log.debug("Deleting automation with id: {}", automationId);

		Automation existing = automationStore.findById(automationId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Automation not found: " + automationId));

		String temporalScheduleId = existing.getTemporalScheduleId();

		boolean deleted = automationStore.deleteById(automationId);

		try {
			automationSchedulerService.deleteSchedule(temporalScheduleId);
		} catch (RuntimeException temporalException) {
			log.error(
					"Temporal schedule deletion failed after the automation database row was already deleted. automationId: {}, temporalScheduleId: {}, operation: deleteAutomation",
					automationId, temporalScheduleId, temporalException);
			throw temporalException;
		}

		log.info("Automation deleted. automationId: {}, temporalScheduleId: {}", automationId, temporalScheduleId);
		return deleted;
	}
}
