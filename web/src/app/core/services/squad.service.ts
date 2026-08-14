import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import {
  SquadBuilderInputRef,
  SquadSavePayload,
  SquadEdgeRoutingType,
} from '../models/squad-builder.model';
import { Squad } from '../models/squad.model';
import { normalizeSquadType } from '../models/squad-type';

import {
  SquadExecutionStatus,
  SquadRunListItem,
  SquadRunStartResponse,
} from '../models/squad-run.model';

export interface SquadApiStepResponse {
  id: string;
  name: string;
  type: string;
  agentKey: string;
  inputRefs?: SquadBuilderInputRef[];
}

export interface SquadApiEdgeResponse {
  sourceStepId: string;
  targetStepId: string;
  routingType?: SquadEdgeRoutingType | null;
  condition?: string | null;
  priority?: number | null;
  isDefault?: boolean | null;
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

  private readonly squadsSignal = signal<Squad[]>([]);

  readonly squads = this.squadsSignal.asReadonly();

  getSquads() {
    return this.squads;
  }

  loadSquadsFromApi(): Observable<Squad[]> {
    return this.http.get<SquadApiResponse[]>(`${this.baseUrl}/squads`).pipe(
      map((apiSquads) => apiSquads.map((apiSquad) => this.mapApiResponseToSquad(apiSquad))),
      tap((squads) => {
        this.squadsSignal.set(squads);
      }),
    );
  }

  getSquadByIdFromApi(squadId: string): Observable<SquadApiResponse> {
    return this.http.get<SquadApiResponse>(`${this.baseUrl}/squads/${squadId}`);
  }

  createSquad(payload: SquadSavePayload): Observable<SquadApiResponse> {
    return this.http.post<SquadApiResponse>(`${this.baseUrl}/squads`, payload);
  }

  updateSquad(squadId: string, payload: SquadSavePayload): Observable<SquadApiResponse> {
    return this.http.put<SquadApiResponse>(`${this.baseUrl}/squads/${squadId}`, payload);
  }

  addCreatedSquadFromApi(createdSquad: SquadApiResponse): void {
    const squad = this.mapApiResponseToSquad(createdSquad);

    this.squadsSignal.update((squads) => [squad, ...squads]);
  }

  upsertSquadFromApi(apiSquad: SquadApiResponse): void {
    const squad = this.mapApiResponseToSquad(apiSquad);

    this.squadsSignal.update((squads) => {
      const exists = squads.some((existingSquad) => existingSquad.id === squad.id);

      if (!exists) {
        return [squad, ...squads];
      }

      return squads.map((existingSquad) => (existingSquad.id === squad.id ? squad : existingSquad));
    });
  }

  deleteSquad(squadId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/squads/${squadId}`).pipe(
      tap(() => {
        this.squadsSignal.update((squads) => squads.filter((squad) => squad.id !== squadId));
      }),
    );
  }

  private mapApiResponseToSquad(apiSquad: SquadApiResponse): Squad {
    const uniqueAgentKeys = new Set(apiSquad.steps.map((step) => step.agentKey).filter(Boolean));

    return {
      id: apiSquad.id,
      name: apiSquad.name,
      description: apiSquad.description,
      type: normalizeSquadType(apiSquad.type),
      status: 'active',
      metrics: {
        steps: apiSquad.steps.length,
        edges: apiSquad.edges.length,
        members: uniqueAgentKeys.size,
      },
    };
  }

  startSquadRun(
    squadId: string,
    initialInput: Record<string, unknown> = {},
  ): Observable<SquadRunStartResponse> {
    return this.http.post<SquadRunStartResponse>(`${this.baseUrl}/squads/${squadId}/runs`, {
      input: initialInput,
    });
  }

  getSquadRuns(): Observable<SquadRunListItem[]> {
    return this.http.get<SquadRunListItem[]>(`${this.baseUrl}/squads/runs`);
  }

  getSquadRunStatus(squadRunId: string): Observable<SquadExecutionStatus> {
    return this.http.get<SquadExecutionStatus>(`${this.baseUrl}/squads/runs/${squadRunId}`);
  }

  cancelSquadRun(squadRunId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/squads/runs/${squadRunId}/cancel`, {});
  }

  deleteSquadRun(squadRunId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/squads/runs/${squadRunId}`);
  }
}
