import { SquadStepDetailsInspector } from './squad-step-details-inspector';
import { TestBed } from '@angular/core/testing';

describe('SquadStepDetailsInspector', () => {
  it('resolves rows from flat target-input keyed step input', () => {
    TestBed.configureTestingModule({});

    const component = TestBed.runInInjectionContext(() => new SquadStepDetailsInspector());

    const rows = component.getInputInspectorRows({
      stepId: 'step-2',
      stepName: 'Step 2',
      agentName: 'Test Weaver',
      status: 'COMPLETED',
      message: null,
      startedAt: null,
      completedAt: null,
      durationMs: null,
      configuredInputRefs: [
        {
          targetInput: 'requirements',
          fromStepId: 'step-1',
          key: 'message',
        },
        {
          targetInput: 'context',
          fromStepId: 'step-1',
          key: 'summary',
        },
      ],
      input: {
        requirements: 'Value produced by Step 1',
      },
      output: null,
      hasExecutionData: true,
    });

    expect(rows).toEqual([
      {
        targetInput: 'requirements',
        sourceStepName: 'Unknown source step',
        outputKey: 'message',
        value: 'Value produced by Step 1',
        hasResolvedValue: true,
      },
      {
        targetInput: 'context',
        sourceStepName: 'Unknown source step',
        outputKey: 'summary',
        value: undefined,
        hasResolvedValue: false,
      },
    ]);
  });

  it('reports COMPLETED status and outputs for a successfully completed step', () => {
    TestBed.configureTestingModule({});

    const component = TestBed.runInInjectionContext(() => new SquadStepDetailsInspector());

    const step = {
      stepId: 'step-3',
      stepName: 'Step 3',
      agentName: 'Deployment Planner',
      status: 'COMPLETED' as const,
      message: null,
      startedAt: null,
      completedAt: null,
      durationMs: null,
      configuredInputRefs: [],
      input: { change: 'x' },
      output: { message: 'Deployment plan ready' },
      hasExecutionData: true,
    };

    expect(component.getOutputInspectorEntries(step)).toEqual([{ outputKey: 'message', value: 'Deployment plan ready' }]);
    expect(component.isExecutionDataMissing(step)).toBe(false);
  });

  it('exposes status and error message for a failed step', () => {
    TestBed.configureTestingModule({});

    const component = TestBed.runInInjectionContext(() => new SquadStepDetailsInspector());

    const step = {
      stepId: 'step-2',
      stepName: 'Step 2',
      agentName: 'Test Selector',
      status: 'FAILED' as const,
      message: 'Step "Step 2" failed: agent unavailable',
      startedAt: null,
      completedAt: null,
      durationMs: null,
      configuredInputRefs: [],
      input: { change: 'x' },
      output: { error: 'agent unavailable' },
      hasExecutionData: true,
    };

    expect(step.status).toBe('FAILED');
    expect(step.message).toContain('agent unavailable');
    expect(component.getOutputInspectorEntries(step)).toEqual([{ outputKey: 'error', value: 'agent unavailable' }]);
  });

  it('handles absent optional output and error values without throwing', () => {
    TestBed.configureTestingModule({});

    const component = TestBed.runInInjectionContext(() => new SquadStepDetailsInspector());

    const step = {
      stepId: 'step-1',
      stepName: 'Step 1',
      agentName: 'Change Classifier',
      status: 'PENDING' as const,
      message: null,
      startedAt: null,
      completedAt: null,
      durationMs: null,
      configuredInputRefs: [],
      input: undefined,
      output: undefined,
      hasExecutionData: false,
    };

    expect(component.isJsonUnavailable(step.output)).toBe(true);
    expect(component.isJsonUnavailable(step.input)).toBe(true);
    expect(component.getOutputInspectorEntries(step)).toEqual([]);
    expect(component.getOutputsSummary(step)).toBe('Not available yet');
  });
});
