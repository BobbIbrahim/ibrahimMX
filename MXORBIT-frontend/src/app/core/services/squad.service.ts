import { Injectable, signal } from '@angular/core';

import { Squad } from '../models/squad.model';

@Injectable({
  providedIn: 'root',
})
export class SquadService {
  private readonly squadsSignal = signal<Squad[]>([
    {
      id: 'squad-001',
      name: 'Release Readiness Squad',
      description:
        'Coordinates release validation by combining code review, test generation, and security checks.',
      type: 'hardcoded-flow',
      status: 'active',
      projectKey: 'CORE',
      tags: ['Release', 'Validation', 'Quality'],
      metrics: {
        steps: 4,
        objects: 2,
        edges: 3,
        members: 3,
      },
    },
    {
      id: 'squad-002',
      name: 'Incident Triage Squad',
      description:
        'Uses prompt-driven collaboration to analyze incidents and recommend next actions.',
      type: 'prompt-squad',
      status: 'active',
      projectKey: 'OPS',
      tags: ['Incident', 'Triage', 'Operations'],
      metrics: {
        steps: 0,
        objects: 1,
        edges: 0,
        members: 2,
      },
    },
    {
      id: 'squad-003',
      name: 'Regression Shield Squad',
      description:
        'Runs a fixed validation flow to detect regression risks before deployment.',
      type: 'hardcoded-flow',
      status: 'paused',
      projectKey: 'QA',
      tags: ['Testing', 'Regression', 'Automation'],
      metrics: {
        steps: 3,
        objects: 2,
        edges: 2,
        members: 2,
      },
    },
  ]);

  readonly squads = this.squadsSignal.asReadonly();

  getSquads() {
    return this.squads;
  }
}