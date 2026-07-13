import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface SquadStopConfirmDialogData {
  squadName: string;
}

@Component({
  selector: 'app-squad-stop-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  templateUrl: './squad-stop-confirm-dialog.html',
  styleUrl: './squad-stop-confirm-dialog.scss',
})
export class SquadStopConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<SquadStopConfirmDialog, boolean>);

  readonly data = inject<SquadStopConfirmDialogData>(MAT_DIALOG_DATA);

  keepRunning(): void {
    this.dialogRef.close(false);
  }

  confirmStop(): void {
    this.dialogRef.close(true);
  }
}
