import { TestBed } from '@angular/core/testing';

import { SquadBuilderStateService } from './squad-builder-state.service';
import { SquadApiResponse } from './squad.service';

describe('SquadBuilderStateService', () => {
  let service: SquadBuilderStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SquadBuilderStateService],
    });

    service = TestBed.inject(SquadBuilderStateService);
  });

  it('initializes an input ref with the first unmapped target input', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing input refs',
      type: 'hardcoded-flow',
    });

    const root = service.addStep();
    service.updateStep(root.id, { assignedAgentId: 'change-classifier' });

    const target = service.addStep();
    service.updateStep(target.id, { assignedAgentId: 'test-selector' });
    service.addEdge(root.id, target.id);
    service.selectStep(target.id);

    service.addSelectedStepInputRef();

    expect(service.selectedStep()?.inputRefs).toEqual([
      { sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: root.id, key: '' },
    ]);
  });

  it('does not add an input ref when every target input is already mapped', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing input refs',
      type: 'hardcoded-flow',
    });

    const root = service.addStep();
    service.updateStep(root.id, { assignedAgentId: 'change-classifier' });

    const target = service.addStep();
    service.updateStep(target.id, {
      assignedAgentId: 'test-selector',
      inputRefs: [
        { sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: root.id, key: 'change' },
        { sourceType: 'STEP_OUTPUT', targetInput: 'changeType', fromStepId: root.id, key: 'changeType' },
      ],
    });
    service.selectStep(target.id);

    service.addSelectedStepInputRef();

    expect(service.selectedStep()?.inputRefs).toHaveLength(2);
  });

  it('removes mappings whose target input is not declared by the new agent', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing input refs',
      type: 'hardcoded-flow',
    });

    const step = service.addStep();
    service.updateStep(step.id, {
      name: 'Step 1',
      parameters: { label: 'keep' },
      assignedAgentId: 'test-selector',
      inputRefs: [
        { sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: 'root', key: 'message' },
        { sourceType: 'STEP_OUTPUT', targetInput: 'changeType', fromStepId: 'root', key: 'summary' },
      ],
    });
    service.selectStep(step.id);

    service.updateSelectedStep({ assignedAgentId: 'change-classifier' });

    expect(service.selectedStep()).toEqual(
      expect.objectContaining({
        name: 'Step 1',
        assignedAgentId: 'change-classifier',
        parameters: { label: 'keep' },
        inputRefs: [{ sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: 'root', key: 'message' }],
      }),
    );
  });

  it('preserves target input when loading and saving a squad', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Workflow',
      description: 'Testing input refs',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Step 1',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [
            { sourceType: 'STEP_OUTPUT', targetInput: 'changeType', fromStepId: 'step-1', key: 'message' },
          ],
        },
      ],
      edges: [{ sourceStepId: 'step-1', targetStepId: 'step-2' }],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.draft()?.steps[1]?.inputRefs[0]).toEqual({
      sourceType: 'STEP_OUTPUT',
      targetInput: 'changeType',
      fromStepId: 'step-1',
      key: 'message',
    });
    expect(service.buildSavePayload()?.steps[1]?.inputRefs[0]).toEqual({
      sourceType: 'STEP_OUTPUT',
      targetInput: 'changeType',
      fromStepId: 'step-1',
      key: 'message',
    });
  });

  it('adds a MANUAL input ref to a root step with available inputs', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing manual input refs',
      type: 'hardcoded-flow',
    });

    const root = service.addStep();
    service.updateStep(root.id, { assignedAgentId: 'change-classifier' });
    service.selectStep(root.id);

    service.addSelectedStepManualInputRef();

    expect(service.selectedStep()?.inputRefs).toEqual([
      { sourceType: 'MANUAL', targetInput: 'change' },
    ]);
  });

  it('does not add a MANUAL input ref when step has ancestors', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing manual input refs',
      type: 'hardcoded-flow',
    });

    const root = service.addStep();
    service.updateStep(root.id, { assignedAgentId: 'change-classifier' });

    const target = service.addStep();
    service.updateStep(target.id, { assignedAgentId: 'test-selector' });
    service.addEdge(root.id, target.id);
    service.selectStep(target.id);

    service.addSelectedStepManualInputRef();

    expect(service.selectedStep()?.inputRefs).toHaveLength(0);
  });

  it('preserves MANUAL input refs when loading and saving a squad', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Workflow',
      description: 'Testing MANUAL input refs',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Step 1',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [{ sourceType: 'MANUAL', targetInput: 'change' }],
        },
      ],
      edges: [],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.draft()?.steps[0]?.inputRefs[0]).toEqual({
      sourceType: 'MANUAL',
      targetInput: 'change',
      fromStepId: undefined,
      key: undefined,
    });
    expect(service.buildSavePayload()?.steps[0]?.inputRefs[0]).toEqual({
      sourceType: 'MANUAL',
      targetInput: 'change',
      fromStepId: undefined,
      key: undefined,
    });
  });
});

