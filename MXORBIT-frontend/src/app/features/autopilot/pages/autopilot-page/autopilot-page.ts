import { Component, inject } from '@angular/core';

import { AutopilotService } from '../../../../core/services/autopilot.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AutopilotCard } from '../../components/autopilot-card/autopilot-card';

@Component({
  selector: 'app-autopilot-page',
  imports: [PageHeader, AutopilotCard],
  templateUrl: './autopilot-page.html',
  styleUrl: './autopilot-page.scss',
})
export class AutopilotPage {
  private readonly autopilotService = inject(AutopilotService);

  readonly autopilots = this.autopilotService.getAutopilots();

  onCreateAutopilot(): void {
    console.log('New Autopilot clicked');
  }
}