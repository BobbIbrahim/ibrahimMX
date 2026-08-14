package com.murex.mxorbit.squadorchestrator.core.automation.validation;

import com.murex.mxorbit.squadorchestrator.core.automation.creator.request.CreateAutomationRequest;
import com.murex.mxorbit.squadorchestrator.core.automation.model.AutomationFrequency;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Rejects an automation definition before it is persisted. Mirrors the
 * database constraints on the automations table exactly, per frequency.
 */
@Service
public class AutomationValidator {

    private static final int MIN_WEEKLY_DAY = 1;
    private static final int MAX_WEEKLY_DAY = 7;

    public void validate(CreateAutomationRequest request) {
        if (request == null) {
            throw badRequest("Automation request is required.");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw badRequest("Automation name must not be blank.");
        }

        if (request.getAssigneeType() == null) {
            throw badRequest("Automation assigneeType is required.");
        }

        if (request.getAssigneeId() == null || request.getAssigneeId().isBlank()) {
            throw badRequest("Automation assigneeId must not be blank.");
        }

        if (request.getFrequency() == null) {
            throw badRequest("Automation frequency is required.");
        }

        validateFrequencyFields(request);
    }

    private void validateFrequencyFields(CreateAutomationRequest request) {
        AutomationFrequency frequency = request.getFrequency();

        switch (frequency) {
            case INTERVAL -> validateInterval(request);
            case DAILY -> validateDailyOrWeekdays(request, "DAILY");
            case WEEKDAYS -> validateDailyOrWeekdays(request, "WEEKDAYS");
            case WEEKLY -> validateWeekly(request);
        }
    }

    private void validateInterval(CreateAutomationRequest request) {
        if (request.getEveryMinutes() == null) {
            throw badRequest("Automation frequency INTERVAL requires everyMinutes.");
        }

        if (request.getEveryMinutes() <= 0) {
            throw badRequest("Automation everyMinutes must be greater than zero.");
        }

        if (request.getRunTime() != null) {
            throw badRequest("Automation frequency INTERVAL must not specify runTime.");
        }

        if (request.getWeeklyDay() != null) {
            throw badRequest("Automation frequency INTERVAL must not specify weeklyDay.");
        }
    }

    private void validateDailyOrWeekdays(CreateAutomationRequest request, String frequencyName) {
        if (request.getRunTime() == null) {
            throw badRequest("Automation frequency " + frequencyName + " requires runTime.");
        }

        if (request.getWeeklyDay() != null) {
            throw badRequest("Automation frequency " + frequencyName + " must not specify weeklyDay.");
        }

        if (request.getEveryMinutes() != null) {
            throw badRequest("Automation frequency " + frequencyName + " must not specify everyMinutes.");
        }
    }

    private void validateWeekly(CreateAutomationRequest request) {
        if (request.getRunTime() == null) {
            throw badRequest("Automation frequency WEEKLY requires runTime.");
        }

        if (request.getWeeklyDay() == null) {
            throw badRequest("Automation frequency WEEKLY requires weeklyDay.");
        }

        if (request.getWeeklyDay() < MIN_WEEKLY_DAY || request.getWeeklyDay() > MAX_WEEKLY_DAY) {
            throw badRequest("Automation weeklyDay must be between 1 and 7.");
        }

        if (request.getEveryMinutes() != null) {
            throw badRequest("Automation frequency WEEKLY must not specify everyMinutes.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
