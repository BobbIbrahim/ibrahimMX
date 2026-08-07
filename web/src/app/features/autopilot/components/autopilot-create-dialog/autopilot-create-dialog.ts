import { Component, ViewEncapsulation, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import {
  AutopilotAssigneeType,
  AutopilotFrequency,
  AutopilotIntervalUnit,
  formatAutopilotInterval,
  localTimeZoneLabel,
} from '../../../../core/models/autopilot.model';
import { AgentService } from '../../../../core/services/agent.service';
import { SquadService } from '../../../../core/services/squad.service';

export type AutopilotCreateDialogResult = {
  name: string;
  assigneeType: AutopilotAssigneeType;
  assigneeId: string;
  assigneeName: string;
  frequency: AutopilotFrequency;
  runTime?: string;
  weeklyDay?: number;
  everyMinutes?: number;
  input: Record<string, string>;
  isActive: boolean;
};

type AssigneeTypeOption = {
  value: AutopilotAssigneeType;
  label: string;
  icon: string;
  description: string;
  disabled?: boolean;
};

type FrequencyOption = {
  value: AutopilotFrequency;
  label: string;
  hint: string;
};

type WeekdayOption = {
  value: number;
  label: string;
};

@Component({
  selector: 'app-autopilot-create-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './autopilot-create-dialog.html',
  styleUrl: './autopilot-create-dialog.scss',
  encapsulation: ViewEncapsulation.None,
})
export class AutopilotCreateDialog {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly dialogRef =
    inject(MatDialogRef<AutopilotCreateDialog, AutopilotCreateDialogResult>);
  private readonly agentService = inject(AgentService);
  private readonly squadService = inject(SquadService);

  readonly assigneeTypes: AssigneeTypeOption[] = [
    {
      value: 'agent',
      label: 'Single agent',
      icon: 'smart_toy',
      description: 'Run one agent on its own.',
      disabled: true,
    },
    {
      value: 'squad',
      label: 'Squad',
      icon: 'groups',
      description: 'Run a whole squad workflow.',
    },
  ];

  readonly frequencies: FrequencyOption[] = [
    { value: 'interval', label: 'Repeating', hint: 'Minutes or hours' },
    { value: 'daily', label: 'Daily', hint: 'Every day' },
    { value: 'weekdays', label: 'Weekdays', hint: 'Mon to Fri' },
    { value: 'weekly', label: 'Weekly', hint: 'Once a week' },
  ];

  readonly intervalUnits: Array<{ value: AutopilotIntervalUnit; label: string }> = [
    { value: 'minutes', label: 'minutes' },
    { value: 'hours', label: 'hours' },
  ];

  readonly weekdays: WeekdayOption[] = [
    { value: 1, label: 'Monday' },
    { value: 2, label: 'Tuesday' },
    { value: 3, label: 'Wednesday' },
    { value: 4, label: 'Thursday' },
    { value: 5, label: 'Friday' },
    { value: 6, label: 'Saturday' },
    { value: 7, label: 'Sunday' },
  ];

  readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    assigneeType: ['squad' as AutopilotAssigneeType, [Validators.required]],
    assigneeId: ['', [Validators.required]],
    frequency: ['weekdays' as AutopilotFrequency, [Validators.required]],
    runTime: ['09:00', [Validators.required]],
    weeklyDay: [1],
    everyValue: [5],
    everyUnit: ['minutes' as AutopilotIntervalUnit],
    input: this.formBuilder.record<string>({}),
    isActive: [true],
  });

  readonly assigneeType = signal<AutopilotAssigneeType>('squad');
  readonly frequency = signal<AutopilotFrequency>('weekdays');
  readonly intervalUnit = signal<AutopilotIntervalUnit>('minutes');
  readonly inputKeys = signal<string[]>([]);
  readonly resolvingInputs = signal(false);

  readonly timeZoneLabel = localTimeZoneLabel();

  private readonly squads = this.squadService.getSquads();

  constructor() {
    this.squadService.loadSquadsFromApi().subscribe({
      error: (error) => console.error('Failed to load squads for the autopilot dialog:', error),
    });
  }

  get assignees(): Array<{ id: string; name: string; hint: string; icon: string }> {
    return this.assigneeType() === 'agent'
      ? this.agentService
        .agents()
        .map((agent) => ({
          id: agent.agentKey,
          name: agent.name,
          hint: agent.role,
          icon: 'smart_toy',
        }))
      : this.squads().map((squad) => ({
        id: squad.id,
        name: squad.name,
        hint: squad.description,
        icon: 'groups',
      }));
  }

  get selectedAssigneeName(): string {
    const assigneeId = this.form.controls.assigneeId.value;

    return this.assignees.find((assignee) => assignee.id === assigneeId)?.name ?? '';
  }

  selectAssigneeType(assigneeType: AutopilotAssigneeType): void {
    if (assigneeType === 'agent') {
      return;
    }

    this.assigneeType.set(assigneeType);
    this.form.controls.assigneeType.setValue(assigneeType);
    this.form.controls.assigneeId.setValue('');
    this.setInputKeys([]);
  }

  onAssigneeChange(): void {
    const assigneeId = this.form.controls.assigneeId.value;

    if (!assigneeId) {
      this.setInputKeys([]);
      return;
    }

    if (this.assigneeType() === 'agent') {
      this.setInputKeys(this.agentService.getAgentByKey(assigneeId)?.inputs ?? []);
      return;
    }

    this.resolvingInputs.set(true);

    this.squadService.getSquadByIdFromApi(assigneeId).subscribe({
      next: (squad) => {
        const targetStepIds = new Set(squad.edges.map((edge) => edge.targetStepId));
        const rootStep = squad.steps.find((step) => !targetStepIds.has(step.id));
        const manualInputs =
          rootStep?.inputRefs
            ?.filter((inputRef) => inputRef.sourceType === 'MANUAL')
            .map((inputRef) => inputRef.targetInput) ?? [];

        this.setInputKeys([...new Set(manualInputs)]);
        this.resolvingInputs.set(false);
      },
      error: (error) => {
        console.error('Failed to resolve the squad inputs:', error);
        this.setInputKeys([]);
        this.resolvingInputs.set(false);
      },
    });
  }

  selectFrequency(frequency: AutopilotFrequency): void {
    this.frequency.set(frequency);
    this.form.controls.frequency.setValue(frequency);

    const runTimeControl = this.form.controls.runTime;
    const weeklyDayControl = this.form.controls.weeklyDay;
    const everyValueControl = this.form.controls.everyValue;

    if (frequency === 'interval') {
      runTimeControl.clearValidators();
      weeklyDayControl.clearValidators();
      everyValueControl.setValidators([Validators.required, Validators.min(1)]);

      runTimeControl.setValue('');
      weeklyDayControl.setValue(1);
    } else {
      runTimeControl.setValidators([Validators.required]);
      everyValueControl.clearValidators();

      if (!runTimeControl.value) {
        runTimeControl.setValue('09:00');
      }

      if (frequency === 'weekly') {
        weeklyDayControl.setValidators([
          Validators.required,
          Validators.min(1),
          Validators.max(7),
        ]);
      } else {
        weeklyDayControl.clearValidators();
        weeklyDayControl.setValue(1);
      }
    }

    runTimeControl.updateValueAndValidity();
    weeklyDayControl.updateValueAndValidity();
    everyValueControl.updateValueAndValidity();
  }

  selectIntervalUnit(unit: AutopilotIntervalUnit): void {
    this.intervalUnit.set(unit);
    this.form.controls.everyUnit.setValue(unit);
  }

  get intervalPreview(): string {
    return formatAutopilotInterval(this.everyMinutes());
  }

  toggleActive(): void {
    this.form.controls.isActive.setValue(!this.form.controls.isActive.value);
  }

  cancel(): void {
    this.dialogRef.close();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.getRawValue();
    const assignee = this.assignees.find((option) => option.id === formValue.assigneeId);
    const isInterval = formValue.frequency === 'interval';
    const isWeekly = formValue.frequency === 'weekly';

    this.dialogRef.close({
      name: formValue.name.trim(),
      assigneeType: formValue.assigneeType,
      assigneeId: formValue.assigneeId,
      assigneeName: assignee?.name ?? formValue.assigneeId,
      frequency: formValue.frequency,
      runTime: isInterval ? undefined : formValue.runTime,
      weeklyDay: isWeekly ? formValue.weeklyDay : undefined,
      everyMinutes: isInterval ? this.everyMinutes() : undefined,
      input: formValue.input,
      isActive: formValue.isActive,
    });
  }

  private everyMinutes(): number {
    const { everyValue, everyUnit } = this.form.getRawValue();

    return everyUnit === 'hours' ? Number(everyValue) * 60 : Number(everyValue);
  }

  private setInputKeys(keys: string[]): void {
    const inputRecord = this.form.controls.input;

    Object.keys(inputRecord.controls).forEach((key) => inputRecord.removeControl(key));

    keys.forEach((key) =>
      inputRecord.addControl(key, this.formBuilder.control('', [Validators.required])),
    );

    this.inputKeys.set(keys);
  }
}
