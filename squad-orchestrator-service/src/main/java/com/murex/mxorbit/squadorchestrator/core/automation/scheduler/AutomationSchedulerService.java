package com.murex.mxorbit.squadorchestrator.core.automation.scheduler;

import com.murex.mxorbit.squadorchestrator.core.automation.assignee.AutomationAssigneeHandler;
import com.murex.mxorbit.squadorchestrator.core.automation.assignee.AutomationAssigneeHandlers;
import com.murex.mxorbit.squadorchestrator.core.automation.model.Automation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.enums.v1.ScheduleOverlapPolicy;
import io.temporal.client.schedules.Schedule;
import io.temporal.client.schedules.ScheduleActionExecution;
import io.temporal.client.schedules.ScheduleActionExecutionStartWorkflow;
import io.temporal.client.schedules.ScheduleActionResult;
import io.temporal.client.schedules.ScheduleActionStartWorkflow;
import io.temporal.client.schedules.ScheduleAlreadyRunningException;
import io.temporal.client.schedules.ScheduleCalendarSpec;
import io.temporal.client.schedules.ScheduleClient;
import io.temporal.client.schedules.ScheduleDescription;
import io.temporal.client.schedules.ScheduleException;
import io.temporal.client.schedules.ScheduleHandle;
import io.temporal.client.schedules.ScheduleIntervalSpec;
import io.temporal.client.schedules.ScheduleOptions;
import io.temporal.client.schedules.SchedulePolicy;
import io.temporal.client.schedules.ScheduleRange;
import io.temporal.client.schedules.ScheduleSpec;
import io.temporal.client.schedules.ScheduleState;
import io.temporal.client.schedules.ScheduleUpdate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Generic Temporal Schedule integration for Autopilot automations. Builds the
 * assignee-specific workflow action through {@link AutomationAssigneeHandlers}
 * so this class never branches on assignee type or references Squad-specific
 * types. All times are UTC by application convention; no timezone is ever set
 * on the Temporal schedule spec.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationSchedulerService {

    private static final Duration CATCHUP_WINDOW = Duration.ofHours(1);

    private static final int WEEKDAYS_START = 1;
    private static final int WEEKDAYS_END = 5;

    private static final int TEMPORAL_SUNDAY = 0;
    private static final int STORED_SUNDAY = 7;

    private final ScheduleClient scheduleClient;
    private final AutomationAssigneeHandlers automationAssigneeHandlers;

    /**
     * Creates the Temporal schedule for a newly created automation. Does not
     * persist anything; the caller owns the database row.
     *
     * @param automation  automation to schedule
     * @param startPaused whether the schedule should be created in a paused
     *                    state
     */
    public void createSchedule(Automation automation, boolean startPaused) {
        String temporalScheduleId = automation.getTemporalScheduleId();
        log.debug("Creating Temporal schedule. automationId: {}, temporalScheduleId: {}, assigneeType: {}",
                automation.getId(), temporalScheduleId, automation.getAssigneeType());

        ScheduleActionStartWorkflow action = buildValidatedScheduleAction(automation);
        ScheduleSpec spec = buildScheduleSpec(automation);
        SchedulePolicy policy = buildSchedulePolicy();

        Schedule.Builder scheduleBuilder = Schedule.newBuilder().setAction(action).setSpec(spec).setPolicy(policy);
        if (startPaused) {
            scheduleBuilder.setState(ScheduleState.newBuilder().setPaused(true).build());
        }

        try {
            scheduleClient.createSchedule(temporalScheduleId, scheduleBuilder.build(),
                    ScheduleOptions.newBuilder().build());
        } catch (ScheduleAlreadyRunningException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Temporal schedule already exists: " + temporalScheduleId);
        }

        log.info("Temporal schedule created. automationId: {}, temporalScheduleId: {}", automation.getId(),
                temporalScheduleId);
    }

    /**
     * Fetches the current Temporal-side state of a schedule. Nothing here is
     * persisted; callers decide what, if anything, to store.
     */
    public AutomationScheduleDescription describeSchedule(String temporalScheduleId) {
        log.debug("Describing Temporal schedule. temporalScheduleId: {}", temporalScheduleId);

        ScheduleDescription description = scheduleClient.getHandle(temporalScheduleId).describe();

        boolean paused = description.getSchedule().getState() != null
                && description.getSchedule().getState().isPaused();

        List<Instant> nextActionTimes = description.getInfo().getNextActionTimes();
        Instant nextRunAt = nextActionTimes == null || nextActionTimes.isEmpty() ? null : nextActionTimes.get(0);

        String lastRunId = lastRunId(description);

        return AutomationScheduleDescription.builder().paused(paused).nextRunAt(nextRunAt).lastRunId(lastRunId)
                .build();
    }

    /**
     * Updates an existing Temporal schedule in place: same schedule ID, same
     * paused/unpaused state, new action and spec derived from the updated
     * automation.
     */
    public void updateSchedule(Automation automation) {
        String temporalScheduleId = automation.getTemporalScheduleId();
        log.debug("Updating Temporal schedule. automationId: {}, temporalScheduleId: {}", automation.getId(),
                temporalScheduleId);

        ScheduleActionStartWorkflow action = buildValidatedScheduleAction(automation);
        ScheduleSpec spec = buildScheduleSpec(automation);

        ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
        handle.update(input -> {
            Schedule updated = Schedule.newBuilder(input.getDescription().getSchedule()).setAction(action)
                    .setSpec(spec).build();
            return new ScheduleUpdate(updated);
        });

        log.info("Temporal schedule updated. automationId: {}, temporalScheduleId: {}", automation.getId(),
                temporalScheduleId);
    }

    /**
     * Pauses a schedule. {@code note} may be null or blank for the SDK default note.
     */
    public void pauseSchedule(String temporalScheduleId, String note) {
        log.debug("Pausing Temporal schedule. temporalScheduleId: {}", temporalScheduleId);

        ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
        if (note == null || note.isBlank()) {
            handle.pause();
        } else {
            handle.pause(note);
        }

        log.info("Temporal schedule paused. temporalScheduleId: {}", temporalScheduleId);
    }

    /**
     * Resumes a schedule. {@code note} may be null or blank for the SDK default note.
     */
    public void resumeSchedule(String temporalScheduleId, String note) {
        log.debug("Resuming Temporal schedule. temporalScheduleId: {}", temporalScheduleId);

        ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
        if (note == null || note.isBlank()) {
            handle.unpause();
        } else {
            handle.unpause(note);
        }

        log.info("Temporal schedule resumed. temporalScheduleId: {}", temporalScheduleId);
    }

    /**
     * Deletes a Temporal schedule. A schedule that is already gone is treated
     * as a successful delete; any other Temporal failure is rethrown.
     */
    public void deleteSchedule(String temporalScheduleId) {
        log.debug("Deleting Temporal schedule. temporalScheduleId: {}", temporalScheduleId);

        ScheduleHandle handle = scheduleClient.getHandle(temporalScheduleId);
        try {
            handle.delete();
        } catch (ScheduleException e) {
            if (isNotFound(e)) {
                log.debug("Temporal schedule already absent, treating delete as success. temporalScheduleId: {}",
                        temporalScheduleId);
                return;
            }
            throw e;
        }

        log.info("Temporal schedule deleted. temporalScheduleId: {}", temporalScheduleId);
    }

    private ScheduleActionStartWorkflow buildValidatedScheduleAction(Automation automation) {
        AutomationAssigneeHandler handler = automationAssigneeHandlers.getHandler(automation.getAssigneeType());
        handler.validate(automation.getAssigneeId(), automation.getInitialInput());
        return handler.buildScheduleAction(automation);
    }

    private ScheduleSpec buildScheduleSpec(Automation automation) {
        switch (automation.getFrequency()) {
            case INTERVAL:
                return intervalSpec(automation);
            case DAILY:
                return dailySpec(automation);
            case WEEKDAYS:
                return weekdaysSpec(automation);
            case WEEKLY:
                return weeklySpec(automation);
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported automation frequency: " + automation.getFrequency());
        }
    }

    private ScheduleSpec intervalSpec(Automation automation) {
        if (automation.getEveryMinutes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Automation frequency INTERVAL requires everyMinutes.");
        }

        ScheduleIntervalSpec interval = new ScheduleIntervalSpec(Duration.ofMinutes(automation.getEveryMinutes()));
        return ScheduleSpec.newBuilder().setIntervals(List.of(interval)).build();
    }

    private ScheduleSpec dailySpec(Automation automation) {
        ScheduleCalendarSpec calendar = calendarSpecBuilder(automation).build();
        return ScheduleSpec.newBuilder().setCalendars(List.of(calendar)).build();
    }

    private ScheduleSpec weekdaysSpec(Automation automation) {
        ScheduleCalendarSpec calendar = calendarSpecBuilder(automation)
                .setDayOfWeek(List.of(new ScheduleRange(WEEKDAYS_START, WEEKDAYS_END))).build();
        return ScheduleSpec.newBuilder().setCalendars(List.of(calendar)).build();
    }

    private ScheduleSpec weeklySpec(Automation automation) {
        if (automation.getWeeklyDay() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Automation frequency WEEKLY requires weeklyDay.");
        }

        // Stored weeklyDay uses Monday=1.  Sunday=7. Temporal's calendar day-of-week uses
        // Sunday=0.  Saturday=6. Monday(1) through Saturday(6) already line up between the
        // two numbering systems, so only the stored Sunday value (7) needs remapping to 0.
        int temporalDayOfWeek = automation.getWeeklyDay() == STORED_SUNDAY ? TEMPORAL_SUNDAY
                : automation.getWeeklyDay();

        ScheduleCalendarSpec calendar = calendarSpecBuilder(automation)
                .setDayOfWeek(List.of(new ScheduleRange(temporalDayOfWeek))).build();
        return ScheduleSpec.newBuilder().setCalendars(List.of(calendar)).build();
    }

    private ScheduleCalendarSpec.Builder calendarSpecBuilder(Automation automation) {
        if (automation.getRunTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Automation frequency " + automation.getFrequency() + " requires runTime.");
        }

        return ScheduleCalendarSpec.newBuilder()
                .setHour(List.of(new ScheduleRange(automation.getRunTime().getHour())))
                .setMinutes(List.of(new ScheduleRange(automation.getRunTime().getMinute())));
    }

    private SchedulePolicy buildSchedulePolicy() {
        return SchedulePolicy.newBuilder().setOverlap(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP)
                .setCatchupWindow(CATCHUP_WINDOW).setPauseOnFailure(false).build();
    }

    private String lastRunId(ScheduleDescription description) {
        List<ScheduleActionResult> recentActions = description.getInfo().getRecentActions();
        if (recentActions == null || recentActions.isEmpty()) {
            return null;
        }

        ScheduleActionExecution lastAction = recentActions.get(recentActions.size() - 1).getAction();
        if (lastAction instanceof ScheduleActionExecutionStartWorkflow) {
            return ((ScheduleActionExecutionStartWorkflow) lastAction).getFirstExecutionRunId();
        }
        return null;
    }

    private boolean isNotFound(ScheduleException exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof StatusRuntimeException statusException
                    && Status.Code.NOT_FOUND.equals(
                    statusException.getStatus().getCode()
            )) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
