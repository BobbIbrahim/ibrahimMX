import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface SquadDeleteConfirmDialogData {
  squadName: string;
}

@Component({
  selector: 'app-squad-delete-confirm-dialog',
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  templateUrl: './squad-delete-confirm-dialog.html',
  styleUrl: './squad-delete-confirm-dialog.scss',
})
export class SquadDeleteConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<SquadDeleteConfirmDialog, boolean>);

  readonly data = inject<SquadDeleteConfirmDialogData>(MAT_DIALOG_DATA);

  cancel(): void {
    this.dialogRef.close(false);
  }

  confirmDelete(): void {
    this.dialogRef.close(true);
  }
}
