import { Injectable, signal } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';

import { Autopilot } from '../models/autopilot.model';

@Injectable({
  providedIn: 'root',
})
export class AutopilotService {
  private readonly autopilotsSignal = signal<Autopilot[]>([
    {
      id: 'autopilot-002',
      name: 'Incident Summary Sweep',
      assigneeType: 'squad',
      assigneeId: 'squad-002',
      assigneeName: 'Incident Triage Squad',
      projectId: 'project-002',
      triggerType: 'schedule',
      frequency: 'interval',
      everyMinutes: 240,
      input: { change: 'Open operational incidents' },
      subscribers: ['ops-team'],
      isActive: true,
    },
  ]);

  readonly autopilots = this.autopilotsSignal.asReadonly();

  getAutopilots() {
    return this.autopilots;
  }

  addAutopilot(autopilot: Omit<Autopilot, 'id' | 'triggerType' | 'subscribers'>): void {
    this.autopilotsSignal.update((autopilots) => [
      ...autopilots,
      {
        ...autopilot,
        id: `autopilot-${crypto.randomUUID()}`,
        triggerType: 'schedule',
        subscribers: [],
      },
    ]);
  }

  pauseAutopilot(autopilotId: string): Observable<Autopilot> {
    return this.setActiveState(autopilotId, false);
  }

  resumeAutopilot(autopilotId: string): Observable<Autopilot> {
    return this.setActiveState(autopilotId, true);
  }

  private setActiveState(autopilotId: string, isActive: boolean): Observable<Autopilot> {
    const autopilot = this.autopilotsSignal().find((item) => item.id === autopilotId);

    if (!autopilot) {
      return throwError(() => new Error(`Autopilot '${autopilotId}' was not found.`));
    }

    const updatedAutopilot = { ...autopilot, isActive };

    this.autopilotsSignal.update((autopilots) =>
      autopilots.map((item) => (item.id === autopilotId ? updatedAutopilot : item)),
    );

    return of(updatedAutopilot);
  }
}
