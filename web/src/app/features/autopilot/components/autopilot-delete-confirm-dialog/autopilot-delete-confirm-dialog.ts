import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface AutopilotDeleteConfirmDialogData {
  autopilotName: string;
}

@Component({
  selector: 'app-autopilot-delete-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './autopilot-delete-confirm-dialog.html',
  styleUrl: './autopilot-delete-confirm-dialog.scss',
})
export class AutopilotDeleteConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<AutopilotDeleteConfirmDialog, boolean>);

  readonly data = inject<AutopilotDeleteConfirmDialogData>(MAT_DIALOG_DATA);

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirmDelete(): void {
    this.dialogRef.close(true);
  }
}
