import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';

import { SquadSavePayload } from '../models/squad-builder.model';
import { Squad } from '../models/squad.model';

export interface SquadApiStepResponse {
  id: string;
  name: string;
  type: string;
  agentKey: string;
}

export interface SquadApiEdgeResponse {
  sourceStepId: string;
  targetStepId: string;
}

export interface SquadApiResponse {
  id: string;
  name: string;
  description: string;
  type: string;
  steps: SquadApiStepResponse[];
  edges: SquadApiEdgeResponse[];
}

@Injectable({
  providedIn: 'root',
})
export class SquadService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = 'http://localhost:8080';
  private readonly localStorageKey = 'mxorbit.saved-squads';

  private readonly initialSquads: Squad[] = [
    {
      id: 'squad-001',
      name: 'Release Readiness Squad',
      description:
        'Coordinates release validation by combining code review, test generation, and security checks.',
      type: 'hardcoded-flow',
      status: 'active',
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

  createSquad(payload: SquadSavePayload) {
    return this.http.post<SquadApiResponse>(`${this.baseUrl}/squads`, payload);
  }

  addSquad(squad: Squad): void {
    const savedSquads = this.loadSavedSquads();

    const updatedSavedSquads = [squad, ...savedSquads];

    this.persistSavedSquads(updatedSavedSquads);

    this.squadsSignal.set([...updatedSavedSquads, ...this.initialSquads]);
  }

  deleteSquad(squadId: string): void {
    this.squadsSignal.update((squads) => squads.filter((squad) => squad.id !== squadId));

    const savedSquads = this.loadSavedSquads();

    const updatedSavedSquads = savedSquads.filter((squad) => squad.id !== squadId);

    this.persistSavedSquads(updatedSavedSquads);
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

  addCreatedSquadFromApi(createdSquad: SquadApiResponse): void {
    const uniqueAgentKeys = new Set(
        createdSquad.steps
            .map((step) => step.agentKey)
            .filter(Boolean),
    );

    const squad: Squad = {
      id: createdSquad.id,
      name: createdSquad.name,
      description: createdSquad.description,
      type: createdSquad.type as Squad['type'],
      status: 'active',
      metrics: {
        steps: createdSquad.steps.length,
        objects: 0,
        edges: createdSquad.edges.length,
        members: uniqueAgentKeys.size,
      },
    };

    this.addSquad(squad);
  }

}
