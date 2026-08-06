package com.murex.mxorbit.squadorchestrator.core.automation.resumer;

import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;
import com.murex.mxorbit.squadorchestrator.core.automation.provider.AutomationProvider;
import com.murex.mxorbit.squadorchestrator.core.automation.scheduler.AutomationSchedulerService;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationResumerService implements AutomationResumer {

	private static final String RESUME_NOTE = "Resumed via Autopilot API";

	private final AutomationProvider automationProvider;
	private final AutomationSchedulerService automationSchedulerService;

	@Override
	public Automation resumeAutomation(UUID automationId) {
		log.debug("Resuming automation. automationId: {}", automationId);

		Automation automation = automationProvider.getAutomationById(automationId);

		automationSchedulerService.resumeSchedule(automation.getTemporalScheduleId(), RESUME_NOTE);

		log.info("Automation resumed. automationId: {}, temporalScheduleId: {}", automationId,
				automation.getTemporalScheduleId());
		return automation;
	}
}
