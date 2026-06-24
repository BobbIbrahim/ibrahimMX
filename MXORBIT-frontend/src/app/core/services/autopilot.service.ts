import { Injectable, signal } from '@angular/core';

import { Autopilot } from '../models/autopilot.model';

@Injectable({
  providedIn: 'root',
})
export class AutopilotService {
  private readonly autopilotsSignal = signal<Autopilot[]>([
    {
      id: 'autopilot-001',
      name: 'PR Review Reminder',
      runbook: `# Goal
Find stale pull requests that need review.

# Context
This autopilot runs for the CORE project repository.

# Steps
1. List all open pull requests in the repository.
2. Identify pull requests open for more than 24 hours without review.
3. Summarize each stale pull request.
4. Notify the team with links.`,
      assigneeType: 'agent',
      assigneeId: 'agent-001',
      assigneeName: 'Code Sentinel',
      projectId: 'project-001',
      projectKey: 'CORE', //tenant
      outputMode: 'create-issue',
      triggerType: 'schedule',
      frequency: 'weekdays',
      time: '10:00',
      timezone: 'GMT+3',
      subscribers: ['release-team'],
      isActive: true,
      nextRunLabel: 'Next run: weekdays at 10:00 GMT+3',
    },
    {
      id: 'autopilot-002',
      name: 'Incident Summary Sweep',
      runbook: `# Goal
Summarize open operational incidents.

# Context
This autopilot runs silently for the OPS operations workflow.

# Steps
1. Read open incidents.
2. Classify incidents by impact.
3. Generate a concise summary.
4. Prepare recommendations for follow-up.`,
      assigneeType: 'squad',
      assigneeId: 'squad-002',
      assigneeName: 'Incident Triage Squad',
      projectId: 'project-002',
      projectKey: 'OPS',
      outputMode: 'run-only',
      triggerType: 'schedule',
      frequency: 'daily',
      time: '09:00',
      timezone: 'GMT+3',
      subscribers: ['ops-team'],
      isActive: true,
      nextRunLabel: 'Next run: daily at 09:00 GMT+3',
    },
  ]);

  readonly autopilots = this.autopilotsSignal.asReadonly();

  getAutopilots() {
    return this.autopilots;
  }
}