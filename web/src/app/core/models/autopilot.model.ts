export type AutopilotAssigneeType = 'agent' | 'squad';

export type AutopilotTriggerType = 'schedule' | 'webhook';

export type AutopilotFrequency = 'interval' | 'daily' | 'weekdays' | 'weekly';

export type AutopilotIntervalUnit = 'minutes' | 'hours';

export interface Autopilot {
  id: string;
  name: string;

  assigneeType: AutopilotAssigneeType;
  assigneeId: string;
  assigneeName: string;

  projectId?: string;

  triggerType: AutopilotTriggerType;

  frequency: AutopilotFrequency;
  /** Time of day for daily, weekday and weekly schedules, in the browser's local zone. */
  runTime?: string;
  /** ISO weekday for weekly schedules, in the browser's local zone: Monday=1, Sunday=7. */
  weeklyDay?: number;
  /** Gap between runs for repeating schedules. */
  everyMinutes?: number;

  /** Values handed to the first step every time the schedule fires. */
  input: Record<string, string>;

  subscribers: string[];

  isActive: boolean;
}

/** A `HH:mm` time of day plus, for weekly schedules, the ISO weekday it falls on. */
export interface AutopilotScheduleTime {
  runTime?: string;
  weeklyDay?: number;
}

const MINUTES_PER_DAY = 24 * 60;

/**
 * The user picks a time in their own zone but the backend stores UTC, so the pair has to move
 * together: shifting past midnight also moves the weekday.
 */
function shiftScheduleTime(
  scheduleTime: AutopilotScheduleTime,
  direction: 1 | -1,
): AutopilotScheduleTime {
  if (!scheduleTime.runTime) {
    return { ...scheduleTime };
  }

  const [hours, minutes] = scheduleTime.runTime.split(':').map(Number);

  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return { ...scheduleTime };
  }

  // getTimezoneOffset() is UTC minus local, so adding it converts local to UTC.
  const shifted = hours * 60 + minutes + direction * new Date().getTimezoneOffset();
  const dayShift = Math.floor(shifted / MINUTES_PER_DAY);
  const minuteOfDay = shifted - dayShift * MINUTES_PER_DAY;

  const runTime = [
    String(Math.floor(minuteOfDay / 60)).padStart(2, '0'),
    String(minuteOfDay % 60).padStart(2, '0'),
  ].join(':');

  if (scheduleTime.weeklyDay === undefined || dayShift === 0) {
    return { ...scheduleTime, runTime };
  }

  return {
    ...scheduleTime,
    runTime,
    weeklyDay: ((scheduleTime.weeklyDay - 1 + dayShift + 7) % 7) + 1,
  };
}

export function toUtcScheduleTime(local: AutopilotScheduleTime): AutopilotScheduleTime {
  return shiftScheduleTime(local, 1);
}

export function toLocalScheduleTime(utc: AutopilotScheduleTime): AutopilotScheduleTime {
  return shiftScheduleTime(utc, -1);
}

/**
 * A recurring schedule has no meaningful execution date, so the API
 * represents runTime as an OffsetDateTime anchored to the fixed date
 * 1970-01-01, e.g. "1970-01-01T06:00:00Z". Only the `HH:mm` clock value
 * (already UTC) matters; the anchor date itself carries no meaning.
 */
const RUN_TIME_ANCHOR_DATE = '1970-01-01';

export function toApiRunTime(utcRunTime: string | undefined): string | undefined {
  return utcRunTime ? `${RUN_TIME_ANCHOR_DATE}T${utcRunTime}:00Z` : undefined;
}

export function fromApiRunTime(apiRunTime: string | undefined): string | undefined {
  if (!apiRunTime) {
    return undefined;
  }

  const match = /T(\d{2}:\d{2})/.exec(apiRunTime);

  return match ? match[1] : undefined;
}

export function formatAutopilotInterval(everyMinutes: number): string {
  if (everyMinutes % 60 !== 0) {
    return `Every ${everyMinutes} minutes`;
  }

  const hours = everyMinutes / 60;

  return hours === 1 ? 'Every hour' : `Every ${hours} hours`;
}

export function formatAutopilotWeekday(weekday: number): string {
  return ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][
    weekday - 1
  ] ?? 'Monday';
}
