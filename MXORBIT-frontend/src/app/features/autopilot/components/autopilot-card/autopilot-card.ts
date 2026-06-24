import { Component, input } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Autopilot } from '../../../../core/models/autopilot.model';

@Component({
  selector: 'app-autopilot-card',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './autopilot-card.html',
  styleUrl: './autopilot-card.scss',
})
export class AutopilotCard {
  readonly autopilot = input.required<Autopilot>();

  get assigneeTypeLabel(): string {
    return this.autopilot().assigneeType === 'agent' ? 'Agent' : 'Squad';
  }

  get outputModeLabel(): string {
    return this.autopilot().outputMode === 'create-issue' ? 'Create Issue' : 'Run Only';
  }

  get triggerLabel(): string {
    return this.autopilot().triggerType === 'schedule' ? 'Schedule' : 'Webhook';
  }

  get frequencyLabel(): string {
    const frequency = this.autopilot().frequency;

    if (frequency === 'weekdays') {
      return 'Weekdays';
    }

    return frequency.charAt(0).toUpperCase() + frequency.slice(1);
  }
}