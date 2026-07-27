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
});
