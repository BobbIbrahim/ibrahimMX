import { Component, OnInit, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';

import { AutopilotService } from '../../../../core/services/autopilot.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AutopilotCard } from '../../components/autopilot-card/autopilot-card';
import {
  AutopilotCreateDialog,
  AutopilotCreateDialogResult,
} from '../../components/autopilot-create-dialog/autopilot-create-dialog';
import {
  AutopilotDeleteConfirmDialog,
  AutopilotDeleteConfirmDialogData,
} from '../../components/autopilot-delete-confirm-dialog/autopilot-delete-confirm-dialog';

@Component({
  selector: 'app-autopilot-page',
  imports: [PageHeader, AutopilotCard],
  templateUrl: './autopilot-page.html',
  styleUrl: './autopilot-page.scss',
})
export class AutopilotPage implements OnInit {
  private readonly autopilotService = inject(AutopilotService);
  private readonly dialog = inject(MatDialog);

  readonly autopilots = this.autopilotService.getAutopilots();

  ngOnInit(): void {
    this.autopilotService.loadAutopilotsFromApi().subscribe({
      error: (error) => console.error('Failed to load autopilots:', error),
    });
  }

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

      this.autopilotService.addAutopilot(result).subscribe({
        error: (error) => console.error('Failed to create autopilot:', error),
      });
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

  onDeleteAutopilot(autopilotId: string): void {
    const autopilot = this.autopilots().find((existingAutopilot) => existingAutopilot.id === autopilotId);

    if (!autopilot) {
      return;
    }

    const dialogRef = this.dialog.open<
      AutopilotDeleteConfirmDialog,
      AutopilotDeleteConfirmDialogData,
      boolean
    >(AutopilotDeleteConfirmDialog, {
      data: {
        autopilotName: autopilot.name,
      },
      width: '30rem',
      maxWidth: '92vw',
      autoFocus: false,
      restoreFocus: true,
      disableClose: true,
      panelClass: 'autopilot-delete-confirm-dialog-panel',
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.autopilotService.deleteAutopilot(autopilotId).subscribe({
        error: (error) => console.error('Failed to delete autopilot:', error),
      });
    });
  }
}
