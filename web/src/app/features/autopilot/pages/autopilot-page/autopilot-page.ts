import { Component, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';

import { AutopilotService } from '../../../../core/services/autopilot.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AutopilotCard } from '../../components/autopilot-card/autopilot-card';
import {
  AutopilotCreateDialog,
  AutopilotCreateDialogResult,
} from '../../components/autopilot-create-dialog/autopilot-create-dialog';

@Component({
  selector: 'app-autopilot-page',
  imports: [PageHeader, AutopilotCard],
  templateUrl: './autopilot-page.html',
  styleUrl: './autopilot-page.scss',
})
export class AutopilotPage {
  private readonly autopilotService = inject(AutopilotService);
  private readonly dialog = inject(MatDialog);

  readonly autopilots = this.autopilotService.getAutopilots();

  onCreateAutopilot(): void {
    const dialogRef = this.dialog.open<
      AutopilotCreateDialog,
      undefined,
      AutopilotCreateDialogResult
    >(AutopilotCreateDialog, {
      width: '40rem',
      maxWidth: '92vw',
      maxHeight: '90vh',
      autoFocus: false,
      restoreFocus: false,
      panelClass: 'autopilot-create-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (!result) {
        return;
      }

      this.autopilotService.addAutopilot(result);
    });
  }

  onPauseAutopilot(autopilotId: string): void {
    this.autopilotService.pauseAutopilot(autopilotId).subscribe({
      error: (error) => console.error('Failed to pause autopilot:', error),
    });
  }

  onResumeAutopilot(autopilotId: string): void {
    this.autopilotService.resumeAutopilot(autopilotId).subscribe({
      error: (error) => console.error('Failed to resume autopilot:', error),
    });
  }
}
