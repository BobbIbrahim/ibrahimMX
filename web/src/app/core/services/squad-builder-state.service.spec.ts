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
    service.updateStep(root.id, { assignedAgentId: 'code-sentinel' });

    const target = service.addStep();
    service.updateStep(target.id, { assignedAgentId: 'test-weaver' });
    service.addEdge(root.id, target.id);
    service.selectStep(target.id);

    service.addSelectedStepInputRef();

    expect(service.selectedStep()?.inputRefs).toEqual([
      { targetInput: 'code', fromStepId: root.id, key: '' },
    ]);
  });

  it('does not add an input ref when every target input is already mapped', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing input refs',
      type: 'hardcoded-flow',
    });

    const root = service.addStep();
    service.updateStep(root.id, { assignedAgentId: 'code-sentinel' });

    const target = service.addStep();
    service.updateStep(target.id, {
      assignedAgentId: 'test-weaver',
      inputRefs: [
        { targetInput: 'code', fromStepId: root.id, key: 'message' },
        { targetInput: 'requirements', fromStepId: root.id, key: 'summary' },
        { targetInput: 'testContext', fromStepId: root.id, key: 'message' },
      ],
    });
    service.selectStep(target.id);

    service.addSelectedStepInputRef();

    expect(service.selectedStep()?.inputRefs).toHaveLength(3);
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
      assignedAgentId: 'code-sentinel',
      inputRefs: [
        { targetInput: 'code', fromStepId: 'root', key: 'message' },
        { targetInput: 'context', fromStepId: 'root', key: 'summary' },
      ],
    });
    service.selectStep(step.id);

    service.updateSelectedStep({ assignedAgentId: 'test-weaver' });

    expect(service.selectedStep()).toEqual(
      expect.objectContaining({
        name: 'Step 1',
        assignedAgentId: 'test-weaver',
        parameters: { label: 'keep' },
        inputRefs: [{ targetInput: 'code', fromStepId: 'root', key: 'message' }],
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
          agentKey: 'code-sentinel',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          type: 'AI_AGENT',
          agentKey: 'test-weaver',
          inputRefs: [
            { targetInput: 'requirements', fromStepId: 'step-1', key: 'message' },
          ],
        },
      ],
      edges: [{ sourceStepId: 'step-1', targetStepId: 'step-2' }],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.draft()?.steps[1]?.inputRefs[0]).toEqual({
      targetInput: 'requirements',
      fromStepId: 'step-1',
      key: 'message',
    });
    expect(service.buildSavePayload()?.steps[1]?.inputRefs[0]).toEqual({
      targetInput: 'requirements',
      fromStepId: 'step-1',
      key: 'message',
    });
  });
});
