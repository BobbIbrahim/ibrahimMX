import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { SquadBuilderType } from '../../../../core/models/squad-builder.model';
import { DEFAULT_SQUAD_TYPE, SQUAD_TYPES } from '../../../../core/models/squad-type';

export type SquadCreateDialogData = Record<string, never>;

export type SquadCreateDialogResult = {
  name: string;
  description: string;
  type: SquadBuilderType;
};

@Component({
  selector: 'app-squad-create-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './squad-create-dialog.html',
  styleUrl: './squad-create-dialog.scss',
})
export class SquadCreateDialog {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(MatDialogRef<SquadCreateDialog, SquadCreateDialogResult>);

  readonly data = inject<SquadCreateDialogData>(MAT_DIALOG_DATA);

  readonly squadTypes = SQUAD_TYPES;

  readonly form = this.formBuilder.group({
    type: [DEFAULT_SQUAD_TYPE as SquadBuilderType, [Validators.required]],
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
  });

  cancel(): void {
    this.dialogRef.close();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.getRawValue();

    const result: SquadCreateDialogResult = {
      type: formValue.type,
      name: formValue.name.trim(),
      description: formValue.description.trim(),
    };

    this.dialogRef.close(result);
  }
}
