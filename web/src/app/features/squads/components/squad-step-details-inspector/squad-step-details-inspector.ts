import { Component, input, signal } from '@angular/core';

import { SelectedStepDetails } from './squad-step-details.types';

type StepDetailsTab = 'inputs' | 'outputs' | 'developer';

type InputInspectorRefRow = {
  targetInput: string;
  sourceStepName: string;
  outputKey: string;
  value: unknown;
  hasResolvedValue: boolean;
};

type OutputInspectorEntry = {
  outputKey: string;
  value: unknown;
};

@Component({
  selector: 'app-squad-step-details-inspector',
  templateUrl: './squad-step-details-inspector.html',
  styleUrl: './squad-step-details-inspector.scss',
})
export class SquadStepDetailsInspector {
  readonly step = input.required<SelectedStepDetails>();
  readonly stepNamesById = input<Record<string, string>>({});
  readonly expanded = input(false);

  readonly activeTab = signal<StepDetailsTab>('inputs');

  selectTab(tab: StepDetailsTab): void {
    this.activeTab.set(tab);
  }

  isExecutionDataMissing(step: SelectedStepDetails): boolean {
    return !step.hasExecutionData;
  }

  isJsonUnavailable(value: Record<string, unknown> | null | undefined): boolean {
    return value === null || value === undefined;
  }

  isEmptyJsonObject(value: Record<string, unknown> | null | undefined): boolean {
    if (!value) {
      return false;
    }

    return Object.keys(value).length === 0;
  }

  getInputInspectorRows(step: SelectedStepDetails): InputInspectorRefRow[] {
    if (step.configuredInputRefs.length === 0) {
      return [];
    }

    return step.configuredInputRefs.map((inputRef) => {
      const hasResolvedValue = Boolean(
        step.input && Object.prototype.hasOwnProperty.call(step.input, inputRef.targetInput),
      );

      return {
        targetInput: inputRef.targetInput,
        sourceStepName: this.resolveStepName(inputRef.fromStepId),
        outputKey: inputRef.key,
        value: hasResolvedValue ? step.input?.[inputRef.targetInput] : undefined,
        hasResolvedValue,
      };
    });
  }

  getOutputInspectorEntries(step: SelectedStepDetails): OutputInspectorEntry[] {
    if (!step.output || this.isEmptyJsonObject(step.output)) {
      return [];
    }

    return Object.entries(step.output).map(([outputKey, value]) => ({
      outputKey,
      value,
    }));
  }

  formatInspectorValue(value: unknown): string {
    if (typeof value === 'string') {
      return value;
    }

    if (typeof value === 'number' || typeof value === 'boolean') {
      return String(value);
    }

    if (value === null) {
      return 'null';
    }

    return JSON.stringify(value);
  }

  formatRawJson(value: Record<string, unknown> | null | undefined): string {
    return JSON.stringify(value, null, 2);
  }

  formatTimestamp(timestamp: number | null): string {
    if (!timestamp) {
      return '—';
    }

    return new Date(timestamp).toLocaleString();
  }

  formatDuration(durationMs: number | null): string {
    if (durationMs === null) {
      return '—';
    }

    const totalSeconds = Math.max(Math.floor(durationMs / 1000), 0);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    return [hours, minutes, seconds].map((value) => value.toString().padStart(2, '0')).join(':');
  }

  getInputsSummary(step: SelectedStepDetails): string {
    if (step.configuredInputRefs.length === 0) {
      return 'None';
    }

    const resolvedInputs = this.getInputInspectorRows(step).filter((row) => row.hasResolvedValue).length;
    return `${resolvedInputs}/${step.configuredInputRefs.length} resolved`;
  }

  getOutputsSummary(step: SelectedStepDetails): string {
    if (this.isExecutionDataMissing(step) || !step.output) {
      return 'Not available yet';
    }

    const outputEntries = this.getOutputInspectorEntries(step);

    if (outputEntries.length === 0) {
      return 'None';
    }

    return `${outputEntries.length} item${outputEntries.length > 1 ? 's' : ''}`;
  }

  private resolveStepName(stepId: string): string {
    const stepName = this.stepNamesById()[stepId];

    if (!stepName) {
      return 'Unknown source step';
    }

    return stepName;
  }

  private isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
  }
}
