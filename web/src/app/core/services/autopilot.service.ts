import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { Autopilot, fromApiRunTime, toApiRunTime, toLocalScheduleTime, toUtcScheduleTime } from '../models/autopilot.model';

type AutopilotApiAssigneeType = 'SQUAD' | 'AGENT';

type AutopilotApiStatus = 'ACTIVE' | 'PAUSED';

type AutopilotApiFrequency = 'INTERVAL' | 'DAILY' | 'WEEKDAYS' | 'WEEKLY';

const API_TO_FRONTEND_FREQUENCY: Record<AutopilotApiFrequency, Autopilot['frequency']> = {
  INTERVAL: 'interval',
  DAILY: 'daily',
  WEEKDAYS: 'weekdays',
  WEEKLY: 'weekly',
};

const FRONTEND_TO_API_FREQUENCY: Record<Autopilot['frequency'], AutopilotApiFrequency> = {
  interval: 'INTERVAL',
  daily: 'DAILY',
  weekdays: 'WEEKDAYS',
  weekly: 'WEEKLY',
};

interface AutopilotApiResponse {
  id: string;
  name: string;
  assigneeType: AutopilotApiAssigneeType;
  assigneeId: string;
  temporalScheduleId?: string;
  frequency: AutopilotApiFrequency;
  runTime?: string;
  weeklyDay?: number;
  everyMinutes?: number;
  input: Record<string, string>;
  createdAt?: string;
  updatedAt?: string;
  assigneeName: string;
  status: AutopilotApiStatus;
  nextRunAt?: string;
  lastRunId?: string;
}

interface AutopilotApiCreateRequest {
  name: string;
  assigneeType: 'SQUAD';
  assigneeId: string;
  frequency: AutopilotApiFrequency;
  runTime?: string;
  weeklyDay?: number;
  everyMinutes?: number;
  input: Record<string, string>;
  startPaused: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AutopilotService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = 'http://localhost:8080';

  private readonly autopilotsSignal = signal<Autopilot[]>([]);

  readonly autopilots = this.autopilotsSignal.asReadonly();

  getAutopilots() {
    return this.autopilots;
  }

  loadAutopilotsFromApi(): Observable<Autopilot[]> {
    return this.http.get<AutopilotApiResponse[]>(`${this.baseUrl}/automations`).pipe(
      map((apiAutopilots) => apiAutopilots.map((apiAutopilot) => this.mapApiResponseToAutopilot(apiAutopilot))),
      tap((autopilots) => {
        this.autopilotsSignal.set(autopilots);
      }),
    );
  }

  addAutopilot(autopilot: Omit<Autopilot, 'id' | 'triggerType' | 'subscribers'>): Observable<Autopilot> {
    const isInterval = autopilot.frequency === 'interval';
    const utcScheduleTime = isInterval
      ? { runTime: undefined, weeklyDay: undefined }
      : toUtcScheduleTime({ runTime: autopilot.runTime, weeklyDay: autopilot.weeklyDay });

    const request: AutopilotApiCreateRequest = {
      name: autopilot.name,
      assigneeType: 'SQUAD',
      assigneeId: autopilot.assigneeId,
      frequency: FRONTEND_TO_API_FREQUENCY[autopilot.frequency],
      runTime: toApiRunTime(utcScheduleTime.runTime),
      weeklyDay: utcScheduleTime.weeklyDay,
      everyMinutes: autopilot.everyMinutes,
      input: autopilot.input,
      startPaused: !autopilot.isActive,
    };

    return this.http.post<AutopilotApiResponse>(`${this.baseUrl}/automations`, request).pipe(
      map((response) => this.mapApiResponseToAutopilot(response)),
      tap((createdAutopilot) => {
        this.upsertAutopilot(createdAutopilot);
      }),
    );
  }

  pauseAutopilot(autopilotId: string): Observable<Autopilot> {
    return this.http.post<AutopilotApiResponse>(`${this.baseUrl}/automations/${autopilotId}/pause`, {}).pipe(
      map((response) => this.mapApiResponseToAutopilot(response)),
      tap((updatedAutopilot) => {
        this.upsertAutopilot(updatedAutopilot);
      }),
    );
  }

  resumeAutopilot(autopilotId: string): Observable<Autopilot> {
    return this.http.post<AutopilotApiResponse>(`${this.baseUrl}/automations/${autopilotId}/resume`, {}).pipe(
      map((response) => this.mapApiResponseToAutopilot(response)),
      tap((updatedAutopilot) => {
        this.upsertAutopilot(updatedAutopilot);
      }),
    );
  }

  deleteAutopilot(autopilotId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/automations/${autopilotId}`).pipe(
      tap(() => {
        this.removeAutopilot(autopilotId);
      }),
    );
  }

  private removeAutopilot(autopilotId: string): void {
    this.autopilotsSignal.update((autopilots) =>
      autopilots.filter((autopilot) => autopilot.id !== autopilotId),
    );
  }

  private upsertAutopilot(autopilot: Autopilot): void {
    this.autopilotsSignal.update((autopilots) => {
      const exists = autopilots.some((existingAutopilot) => existingAutopilot.id === autopilot.id);

      if (!exists) {
        return [...autopilots, autopilot];
      }

      return autopilots.map((existingAutopilot) =>
        existingAutopilot.id === autopilot.id ? autopilot : existingAutopilot,
      );
    });
  }

  private mapApiResponseToAutopilot(response: AutopilotApiResponse): Autopilot {
    const isInterval = response.frequency === 'INTERVAL';
    const localScheduleTime = isInterval
      ? { runTime: undefined, weeklyDay: undefined }
      : toLocalScheduleTime({ runTime: fromApiRunTime(response.runTime), weeklyDay: response.weeklyDay });

    return {
      id: response.id,
      name: response.name,
      assigneeType: response.assigneeType.toLowerCase() as Autopilot['assigneeType'],
      assigneeId: response.assigneeId,
      assigneeName: response.assigneeName,
      triggerType: 'schedule',
      frequency: API_TO_FRONTEND_FREQUENCY[response.frequency],
      runTime: localScheduleTime.runTime,
      weeklyDay: localScheduleTime.weeklyDay,
      everyMinutes: response.everyMinutes,
      input: response.input,
      subscribers: [],
      isActive: response.status === 'ACTIVE',
    };
  }
}
