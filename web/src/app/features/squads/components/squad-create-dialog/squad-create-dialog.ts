import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { SquadBuilderType } from '../../../../core/models/squad-builder.model';

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

  readonly squadTypes: Array<{
    value: SquadBuilderType;
    title: string;
    description: string;
  }> = [
    {
      value: 'hardcoded-flow',
      title: 'Hardcoded Flow',
      description: 'Build a fixed step-by-step workflow with explicit nodes and connections.',
    },
    {
      value: 'prompt-squad',
      title: 'Prompt Squad',
      description: 'Define a flexible prompt-driven squad that can reason about the task.',
    },
  ];

  readonly form = this.formBuilder.group({
    type: ['hardcoded-flow' as SquadBuilderType, [Validators.required]],
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
