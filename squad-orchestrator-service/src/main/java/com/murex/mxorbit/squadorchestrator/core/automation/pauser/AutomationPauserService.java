package com.murex.mxorbit.squadorchestrator.core.automation.pauser;

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
public class AutomationPauserService implements AutomationPauser {

	private static final String PAUSE_NOTE = "Paused via Autopilot API";

	private final AutomationProvider automationProvider;
	private final AutomationSchedulerService automationSchedulerService;

	@Override
	public Automation pauseAutomation(UUID automationId) {
		log.debug("Pausing automation. automationId: {}", automationId);

		Automation automation = automationProvider.getAutomationById(automationId);

		automationSchedulerService.pauseSchedule(automation.getTemporalScheduleId(), PAUSE_NOTE);

		log.info("Automation paused. automationId: {}, temporalScheduleId: {}", automationId,
				automation.getTemporalScheduleId());
		return automation;
	}
}
