import { Injectable, signal } from '@angular/core';

import { Squad } from '../models/squad.model';

@Injectable({
  providedIn: 'root',
})
export class SquadService {
  private readonly localStorageKey = 'mxorbit.saved-squads';

  private readonly initialSquads: Squad[] = [
    {
      id: 'squad-001',
      name: 'Release Readiness Squad',
      description:
        'Coordinates release validation by combining code review, test generation, and security checks.',
      type: 'hardcoded-flow',
      status: 'active',
      tags: ['Release', 'Validation', 'Quality'],
      metrics: {
        steps: 4,
        objects: 0,
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
      tags: ['Incident', 'Triage', 'Operations'],
      metrics: {
        steps: 0,
        objects: 0,
        edges: 0,
        members: 2,
      },
    },
    {
      id: 'squad-003',
      name: 'Regression Shield Squad',
      description: 'Runs a fixed validation flow to detect regression risks before deployment.',
      type: 'hardcoded-flow',
      status: 'paused',
      tags: ['Testing', 'Regression', 'Automation'],
      metrics: {
        steps: 3,
        objects: 0,
        edges: 2,
        members: 2,
      },
    },
  ];

  private readonly squadsSignal = signal<Squad[]>([
    ...this.loadSavedSquads(),
    ...this.initialSquads,
  ]);

  readonly squads = this.squadsSignal.asReadonly();

  getSquads() {
    return this.squads;
  }

  addSquad(squad: Squad): void {
    const savedSquads = this.loadSavedSquads();

    const updatedSavedSquads = [squad, ...savedSquads];

    this.persistSavedSquads(updatedSavedSquads);

    this.squadsSignal.set([...updatedSavedSquads, ...this.initialSquads]);
  }

  private loadSavedSquads(): Squad[] {
    try {
      const rawSavedSquads = localStorage.getItem(this.localStorageKey);

      if (!rawSavedSquads) {
        return [];
      }

      const parsedSavedSquads = JSON.parse(rawSavedSquads) as Squad[];

      if (!Array.isArray(parsedSavedSquads)) {
        return [];
      }

      return parsedSavedSquads;
    } catch {
      return [];
    }
  }

  private persistSavedSquads(squads: Squad[]): void {
    localStorage.setItem(this.localStorageKey, JSON.stringify(squads));
  }

  deleteSquad(squadId: string): void {
    this.squadsSignal.update((squads) => squads.filter((squad) => squad.id !== squadId));

    const savedSquads = this.loadSavedSquads();

    const updatedSavedSquads = savedSquads.filter((squad) => squad.id !== squadId);

    this.persistSavedSquads(updatedSavedSquads);
  }
}
