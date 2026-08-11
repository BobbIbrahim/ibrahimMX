package com.murex.mxorbit.squadorchestrator.core.automation.deleter;

import com.murex.mxorbit.squadorchestrator.core.automation.model.AssigneeType;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService;
import com.murex.mxorbit.squadorchestrator.core.automation.store.AutomationStore;

import java.util.List;
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

		boolean deleted = deleteAutomationAndSchedule(existing, existing.getAssigneeType(), existing.getAssigneeId());

		log.info("Automation deleted. automationId: {}, temporalScheduleId: {}", automationId,
				existing.getTemporalScheduleId());
		return deleted;
	}

	@Override
	public void deleteByAssignee(AssigneeType assigneeType, String assigneeId) {
		log.debug("Deleting automations by assignee. assigneeType: {}, assigneeId: {}", assigneeType, assigneeId);

		List<Automation> matching = automationStore.findAllByAssignee(assigneeType, assigneeId);

		for (Automation automation : matching) {
			deleteAutomationAndSchedule(automation, assigneeType, assigneeId);
		}

		log.info("Automations deleted for assignee. assigneeType: {}, assigneeId: {}, count: {}", assigneeType,
				assigneeId, matching.size());
	}

	/**
	 * Deletes the database row first, then the Temporal schedule. Temporal cleanup
	 * is still attempted when the database row was concurrently absent. If Temporal
	 * deletion fails unexpectedly, the failure is logged with safe identifying
	 * fields and rethrown; the database row is never restored.
	 *
	 * @return whether the Automation database row was deleted
	 */
	private boolean deleteAutomationAndSchedule(Automation automation, AssigneeType assigneeType, String assigneeId) {
		UUID automationId = automation.getId();
		String temporalScheduleId = automation.getTemporalScheduleId();

		boolean deleted = automationStore.deleteById(automationId);

		try {
			automationSchedulerService.deleteSchedule(temporalScheduleId);
		} catch (RuntimeException temporalException) {
			log.error(
					"Temporal schedule deletion failed after the automation database deletion was attempted. "
							+ "automationId: {}, temporalScheduleId: {}, assigneeType: {}, assigneeId: {}, "
							+ "databaseRowDeleted: {}",
					automationId, temporalScheduleId, assigneeType, assigneeId, deleted, temporalException);
			throw temporalException;
		}

		return deleted;
	}
}
