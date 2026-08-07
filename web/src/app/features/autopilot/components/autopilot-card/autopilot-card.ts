import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import {
  Autopilot,
  formatAutopilotInterval,
  formatAutopilotWeekday,
} from '../../../../core/models/autopilot.model';

@Component({
  selector: 'app-autopilot-card',
  imports: [MatIconModule],
  templateUrl: './autopilot-card.html',
  styleUrl: './autopilot-card.scss',
})
export class AutopilotCard {
  readonly autopilot = input.required<Autopilot>();
  readonly pauseRequested = output<string>();
  readonly resumeRequested = output<string>();
  readonly deleteAutopilot = output<string>();

  get assigneeTypeLabel(): string {
    return this.autopilot().assigneeType === 'agent' ? 'Agent' : 'Squad';
  }

  get triggerLabel(): string {
    return this.autopilot().triggerType === 'schedule' ? 'Schedule' : 'Webhook';
  }

  get frequencyLabel(): string {
    const frequency = this.autopilot().frequency;

    if (frequency === 'interval') {
      return 'Repeating';
    }

    if (frequency === 'weekdays') {
      return 'Weekdays';
    }

    return frequency.charAt(0).toUpperCase() + frequency.slice(1);
  }

  get isInterval(): boolean {
    return this.autopilot().frequency === 'interval';
  }

  get intervalLabel(): string {
    const { everyMinutes = 60, runTime, weeklyDay } = this.autopilot();

    if (!this.isInterval) {
      const day =
        this.autopilot().frequency === 'weekly'
          ? `${formatAutopilotWeekday(weeklyDay ?? 1)} · `
          : '';

      return `${day}${runTime ?? ''}`;
    }

    return everyMinutes % 60 === 0 ? `Every ${everyMinutes / 60}h` : `Every ${everyMinutes}m`;
  }

  get inputEntries(): Array<{ key: string; value: string }> {
    return Object.entries(this.autopilot().input).map(([key, value]) => ({ key, value }));
  }

  get nextRunLabel(): string {
    if (!this.autopilot().isActive) {
      return 'Paused';
    }

    if (this.isInterval) {
      return formatAutopilotInterval(this.autopilot().everyMinutes ?? 60);
    }

    const weeklyDay =
      this.autopilot().frequency === 'weekly'
        ? ` on ${formatAutopilotWeekday(this.autopilot().weeklyDay ?? 1)}`
        : '';

    return `${this.frequencyLabel}${weeklyDay} at ${this.autopilot().runTime}`;
  }

  onToggleActive(): void {
    const autopilot = this.autopilot();

    if (autopilot.isActive) {
      this.pauseRequested.emit(autopilot.id);
      return;
    }

    this.resumeRequested.emit(autopilot.id);
  }

  onDeleteRequested(): void {
    this.deleteAutopilot.emit(this.autopilot().id);
  }
}
