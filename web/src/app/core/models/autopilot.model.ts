export type AutopilotAssigneeType = 'agent' | 'squad';

export type AutopilotOutputMode = 'create-issue' | 'run-only';

export type AutopilotTriggerType = 'schedule' | 'webhook';

export type AutopilotFrequency = 'daily' | 'weekdays' | 'weekly';

export interface Autopilot {
  id: string;
  name: string;
  runbook: string;

  assigneeType: AutopilotAssigneeType;
  assigneeId: string;
  assigneeName: string;

  projectId?: string;
  projectKey?: string;

  outputMode: AutopilotOutputMode;
  triggerType: AutopilotTriggerType;

  frequency: AutopilotFrequency;
  time: string;
  timezone: string;

  subscribers: string[];

  isActive: boolean;
  nextRunLabel: string;
}