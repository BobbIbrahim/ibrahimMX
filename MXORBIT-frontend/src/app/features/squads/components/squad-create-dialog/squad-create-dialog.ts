import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';

import {
  SquadBuilderObjectType,
  SquadBuilderType,
} from '../../../../core/models/squad-builder.model';

export type SquadCreateDialogData = Record<string, never>;

export type SquadCreateDialogResult = {
  name: string;
  description: string;
  type: SquadBuilderType;
  projectKey: string;
  objectTypes: SquadBuilderObjectType[];
};

@Component({
  selector: 'app-squad-create-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatRadioModule,
  ],
  templateUrl: './squad-create-dialog.html',
  styleUrl: './squad-create-dialog.scss',
})
export class SquadCreateDialog {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly dialogRef = inject(
    MatDialogRef<SquadCreateDialog, SquadCreateDialogResult>,
  );

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

  readonly objectTypeOptions: SquadBuilderObjectType[] = ['JIRA', 'PEGA'];

  readonly form = this.formBuilder.group({
    type: ['hardcoded-flow' as SquadBuilderType, [Validators.required]],
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required, Validators.minLength(10)]],
    projectKey: ['', [Validators.required, Validators.minLength(2)]],
    jiraEnabled: [true],
    pegaEnabled: [false],
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

    const objectTypes: SquadBuilderObjectType[] = [];

    if (formValue.jiraEnabled) {
      objectTypes.push('JIRA');
    }

    if (formValue.pegaEnabled) {
      objectTypes.push('PEGA');
    }

    if (objectTypes.length === 0) {
      this.form.controls.jiraEnabled.setValue(true);
      objectTypes.push('JIRA');
    }

    const result: SquadCreateDialogResult = {
      type: formValue.type,
      name: formValue.name.trim(),
      description: formValue.description.trim(),
      projectKey: formValue.projectKey.trim().toUpperCase(),
      objectTypes,
    };

    this.dialogRef.close(result);
  }
}