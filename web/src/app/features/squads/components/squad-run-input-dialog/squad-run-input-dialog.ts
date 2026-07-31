import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface SquadRunInputDialogData {
  stepName: string;
  agentName: string;
  inputKeys: string[];
}

@Component({
  selector: 'app-squad-run-input-dialog',
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './squad-run-input-dialog.html',
  styleUrl: './squad-run-input-dialog.scss',
})
export class SquadRunInputDialog {
  private readonly dialogRef = inject(MatDialogRef<SquadRunInputDialog, Record<string, string> | null>);

  readonly data = inject<SquadRunInputDialogData>(MAT_DIALOG_DATA);

  readonly values: Record<string, string> = {};

  cancel(): void {
    this.dialogRef.close(null);
  }

  confirm(): void {
    this.dialogRef.close({ ...this.values });
  }
}
