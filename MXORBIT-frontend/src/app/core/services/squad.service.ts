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
      type: 'hardcoded-flow',
      status: 'live',
      tenant: 'CORE',
      description: 'Coordinates code review, test generation, deployment checks, and release notes preparation.',
      tags: ['JIRA', 'CI/CD', 'QA'],
      stepsCount: 6,
      objectsCount: 14,
      edgesCount: 8,
      membersCount: 3,
    },
    {
      id: 'squad-002',
      name: 'Incident Triage Squad',
      type: 'prompt-squad',
      status: 'draft',
      tenant: 'OPS',
      description: 'Analyzes incidents, classifies impact, proposes remediation steps, and prepares summaries.',
      tags: ['INCIDENT', 'PEGA', 'OPS'],
      stepsCount: 4,
      objectsCount: 9,
      edgesCount: 5,
      membersCount: 2,
    },
  ]);

  readonly squads = this.squadsSignal.asReadonly();

  getSquads() {
    return this.squads;
  }
}