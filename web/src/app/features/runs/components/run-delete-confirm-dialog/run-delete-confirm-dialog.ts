import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface RunDeleteConfirmDialogData {
  squadName: string;
  squadRunId: string;
}

@Component({
  selector: 'app-run-delete-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './run-delete-confirm-dialog.html',
  styleUrl: './run-delete-confirm-dialog.scss',
})
export class RunDeleteConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<RunDeleteConfirmDialog, boolean>);

  readonly data = inject<RunDeleteConfirmDialogData>(MAT_DIALOG_DATA);

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirmDelete(): void {
    this.dialogRef.close(true);
  }
}
