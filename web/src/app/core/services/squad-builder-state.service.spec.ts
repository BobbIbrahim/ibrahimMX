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
        {
          sourceType: 'STEP_OUTPUT',
          targetInput: 'changeType',
          fromStepId: root.id,
          key: 'changeType',
        },
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
        {
          sourceType: 'STEP_OUTPUT',
          targetInput: 'changeType',
          fromStepId: 'root',
          key: 'summary',
        },
      ],
    });
    service.selectStep(step.id);

    service.updateSelectedStep({ assignedAgentId: 'change-classifier' });

    expect(service.selectedStep()).toEqual(
      expect.objectContaining({
        name: 'Step 1',
        assignedAgentId: 'change-classifier',
        parameters: { label: 'keep' },
        inputRefs: [
          { sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: 'root', key: 'message' },
        ],
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
            {
              sourceType: 'STEP_OUTPUT',
              targetInput: 'changeType',
              fromStepId: 'step-1',
              key: 'message',
            },
          ],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: false,
        },
      ],
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
  it('adds safe routing defaults to a newly created edge', () => {
    service.createDraft({
      name: 'Workflow',
      description: 'Testing edge defaults',
      type: 'hardcoded-flow',
    });

    const source = service.addStep();
    const target = service.addStep();

    service.addEdge(source.id, target.id);

    expect(service.edges()).toEqual([
      expect.objectContaining({
        sourceStepId: source.id,
        targetStepId: target.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: false,
      }),
    ]);
  });

  it('loads legacy API edges with safe routing defaults', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Legacy Workflow',
      description: 'Testing legacy edge defaults',
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
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.edges()).toEqual([
      expect.objectContaining({
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: false,
      }),
    ]);
  });

  it('preserves routing metadata when loading and saving a conditional edge', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Conditional Workflow',
      description: 'Testing conditional routing metadata',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Change Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Test Selector',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.changeType equals BUG_FIX',
          priority: 1,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.edges()).toEqual([
      expect.objectContaining({
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.changeType equals BUG_FIX',
        priority: 1,
        isDefault: false,
      }),
    ]);

    expect(service.buildSavePayload()?.edges).toEqual([
      {
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.changeType equals BUG_FIX',
        priority: 1,
        isDefault: false,
      },
    ]);
  });

  it('preserves default routing metadata when loading and saving an edge', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Default Route Workflow',
      description: 'Testing default routing metadata',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Change Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Deployment Planner',
          type: 'AI_AGENT',
          agentKey: 'deployment-planner',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: true,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.buildSavePayload()?.edges).toEqual([
      {
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      },
    ]);
  });

  it('reconstructs exactly one conditional for a source with at least one WHEN edge', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Conditional Workflow',
      description: 'Testing conditional reconstruction',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals X',
          priority: 1,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const conditionals = service.conditionals();
    expect(conditionals).toHaveLength(1);
    expect(conditionals[0]).toEqual(
      expect.objectContaining({
        name: 'Conditional',
        sourceStepId: 'step-1',
        position: {
          x: expect.any(Number),
          y: expect.any(Number),
        },
      }),
    );
  });

  it('reconstructs only one conditional for a source with multiple WHEN edges', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Multi-condition Workflow',
      description: 'Testing multiple WHEN edges',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Target 2',
          type: 'AI_AGENT',
          agentKey: 'deployment-planner',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals X',
          priority: 1,
          isDefault: false,
        },
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-3',
          routingType: 'WHEN',
          condition: 'output.type equals Y',
          priority: 2,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const conditionals = service.conditionals();
    expect(conditionals).toHaveLength(1);
    expect(conditionals[0]).toEqual(
      expect.objectContaining({
        name: 'Conditional',
        sourceStepId: 'step-1',
      }),
    );
  });

  it('reconstructs separate conditionals for different sources with WHEN edges', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Multi-source Workflow',
      description: 'Testing separate conditionals',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Splitter',
          type: 'AI_AGENT',
          agentKey: 'deployment-planner',
          inputRefs: [],
        },
        {
          id: 'step-4',
          name: 'Target 2',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals X',
          priority: 1,
          isDefault: false,
        },
        {
          sourceStepId: 'step-3',
          targetStepId: 'step-4',
          routingType: 'WHEN',
          condition: 'output.priority > 5',
          priority: 1,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const conditionals = service.conditionals();
    expect(conditionals).toHaveLength(2);
    expect(conditionals.some((c) => c.sourceStepId === 'step-1')).toBe(true);
    expect(conditionals.some((c) => c.sourceStepId === 'step-3')).toBe(true);
  });

  it('does not reconstruct a conditional for a source with only ALWAYS edges', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Linear Workflow',
      description: 'Testing no conditional',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.conditionals()).toHaveLength(0);
  });

  it('preserves routing metadata after conditional reconstruction', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Routing Metadata Workflow',
      description: 'Testing routing preservation',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Target 2',
          type: 'AI_AGENT',
          agentKey: 'deployment-planner',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals BUG',
          priority: 1,
          isDefault: false,
        },
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-3',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: true,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const edges = service.edges();
    expect(edges).toHaveLength(2);
    expect(edges).toContainEqual(
      expect.objectContaining({
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      }),
    );
    expect(edges).toContainEqual(
      expect.objectContaining({
        sourceStepId: 'step-1',
        targetStepId: 'step-3',
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      }),
    );
  });

  it('excludes reconstructed conditionals from buildSavePayload', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Save Payload Workflow',
      description: 'Testing conditional exclusion',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals X',
          priority: 1,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const payload = service.buildSavePayload();
    expect(payload).toBeDefined();
    // buildSavePayload should not include conditionals field
    expect('conditionals' in (payload ?? {})).toBe(false);
    // Only edges and steps should be present
    expect(payload?.steps).toHaveLength(2);
    expect(payload?.edges).toHaveLength(1);
  });

  it('resets step and conditional selection after loading from API', () => {
    service.createDraft({
      name: 'Draft',
      description: 'Before loading',
      type: 'hardcoded-flow',
    });

    const step = service.addStep();
    service.selectStep(step.id);

    expect(service.selectedStepId()).toBe(step.id);

    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'API Workflow',
      description: 'Testing selection reset',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Step 1',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
      ],
      edges: [],
    };

    service.loadDraftFromApi(apiSquad);

    expect(service.selectedStepId()).toBeNull();
    expect(service.selectedConditionalId()).toBeNull();
  });

  it('reconstructs conditionals with correct positioning relative to source step', () => {
    const apiSquad: SquadApiResponse = {
      id: 'squad-1',
      name: 'Positioning Workflow',
      description: 'Testing position calculation',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target',
          type: 'AI_AGENT',
          agentKey: 'test-selector',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.type equals X',
          priority: 1,
          isDefault: false,
        },
      ],
    };

    service.loadDraftFromApi(apiSquad);

    const sourceStep = service.steps()[0];
    const conditional = service.conditionals()[0];

    expect(conditional?.position.x).toBe(sourceStep.position.x + 240);
    expect(conditional?.position.y).toBe(sourceStep.position.y);
  });

  describe('addConditionalRoute', () => {
    it('creates a valid WHEN route', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing conditional route creation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeDefined();
      expect(result?.sourceStepId).toBe(source.id);
      expect(result?.targetStepId).toBe(target.id);
      expect(result?.routingType).toBe('WHEN');
      expect(result?.condition).toBe('output.type equals BUG');
      expect(result?.priority).toBe(1);
      expect(result?.isDefault).toBe(false);
      expect(service.edges()).toContainEqual(result);
    });

    it('creates a valid default (ALWAYS) route', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing default route creation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      expect(result).toBeDefined();
      expect(result?.sourceStepId).toBe(source.id);
      expect(result?.targetStepId).toBe(target.id);
      expect(result?.routingType).toBe('ALWAYS');
      expect(result?.condition).toBeNull();
      expect(result?.priority).toBe(100);
      expect(result?.isDefault).toBe(true);
    });

    it('uses sourceStepId from conditional, not conditionalId', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing source step resolution',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result?.sourceStepId).toBe(source.id);
      expect(result?.sourceStepId).not.toBe(conditionalId);
    });

    it('preserves normalized condition exactly', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing condition preservation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const normalizedCondition = 'output.status equals "success value"';
      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: normalizedCondition,
        priority: 1,
        isDefault: false,
      });

      expect(result?.condition).toBe(normalizedCondition);
    });

    it('rejects route when no draft exists', () => {
      const result = service.addConditionalRoute({
        conditionalId: 'non-existent',
        targetStepId: 'non-existent',
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects route when conditional does not exist', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing unknown conditional',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      const result = service.addConditionalRoute({
        conditionalId: 'non-existent-conditional',
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects route when target step does not exist', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing unknown target',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: 'non-existent-target',
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects route when source and target are the same', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing self-route rejection',
        type: 'hardcoded-flow',
      });

      const step = service.addStep();
      service.updateStep(step.id, { assignedAgentId: 'change-classifier' });

      service.addConditional(step.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: step.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects duplicate source-target edge', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing duplicate prevention',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      // Create first route
      service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      // Try to create duplicate
      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.a equals B',
        priority: 2,
        isDefault: false,
      });

      expect(result).toBeNull();
      expect(service.edges()).toHaveLength(1);
    });

    it('rejects negative priority', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing priority validation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: -1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects decimal priority', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing decimal priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1.5,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects NaN priority', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing NaN priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: NaN,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects infinite priority', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing infinite priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: Infinity,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects WHEN route with blank condition', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing blank condition rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: '   ',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects WHEN route with null condition', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing null condition rejection for WHEN',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: null,
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects invalid condition according to validator', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing condition validation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'invalid condition text',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects WHEN route marked as default', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing WHEN-default rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: true,
      });

      expect(result).toBeNull();
    });

    it('rejects ALWAYS route with a condition', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing ALWAYS-condition rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'ALWAYS',
        condition: 'output.x equals Y',
        priority: 100,
        isDefault: true,
      });

      expect(result).toBeNull();
    });

    it('rejects duplicate priority under the same source for WHEN routes', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing duplicate priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      // Create first route with priority 1
      service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.a equals A',
        priority: 1,
        isDefault: false,
      });

      // Try to create another route with same priority
      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.b equals B',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
      expect(service.edges()).toHaveLength(1);
    });

    it('allows same priority under different source steps', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing different sources',
        type: 'hardcoded-flow',
      });

      const source1 = service.addStep();
      service.updateStep(source1.id, { assignedAgentId: 'change-classifier' });

      const source2 = service.addStep();
      service.updateStep(source2.id, { assignedAgentId: 'deployment-planner' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source1.id);
      const cond1 = service.selectedConditionalId();

      service.addConditional(source2.id);
      const cond2 = service.selectedConditionalId();

      // Create route from source1
      const result1 = service.addConditionalRoute({
        conditionalId: cond1!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.a equals A',
        priority: 1,
        isDefault: false,
      });

      // Create route from source2 with same priority
      const result2 = service.addConditionalRoute({
        conditionalId: cond2!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.b equals B',
        priority: 1,
        isDefault: false,
      });

      expect(result1).toBeDefined();
      expect(result2).toBeDefined();
      expect(service.edges()).toHaveLength(2);
    });

    it('rejects a second default route for the same source', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing duplicate default rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      // Create first default route
      service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      // Try to create second default route
      const result = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      expect(result).toBeNull();
      expect(service.edges()).toHaveLength(1);
    });
  });

  describe('updateConditionalRoute', () => {
    it('updates a valid WHEN route', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing update WHEN route',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      expect(updated).toBeDefined();
      expect(updated?.condition).toBe('output.type equals FEATURE');
      expect(updated?.priority).toBe(2);
    });

    it('changes route target successfully', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing target change',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      expect(updated?.targetStepId).toBe(target2.id);
    });

    it('preserves edge id and source step id', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing preservation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const originalId = edge!.id;
      const originalSourceId = edge!.sourceStepId;

      const updated = service.updateConditionalRoute(originalId, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      expect(updated?.id).toBe(originalId);
      expect(updated?.sourceStepId).toBe(originalSourceId);
    });

    it('updates normalized condition exactly', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing normalized condition',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const newCondition = 'output.status equals "success value"';
      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: newCondition,
        priority: 1,
        isDefault: false,
      });

      expect(updated?.condition).toBe(newCondition);
    });

    it('converts default to WHEN', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing default to WHEN conversion',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      expect(updated?.routingType).toBe('WHEN');
      expect(updated?.isDefault).toBe(false);
    });

    it('converts WHEN to default when no other default exists', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing WHEN to default conversion',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      expect(updated?.routingType).toBe('ALWAYS');
      expect(updated?.isDefault).toBe(true);
    });

    it('rejects duplicate target for same source', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing duplicate target rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      const result = service.updateConditionalRoute(edge2!.id, {
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects duplicate priority for same source', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing duplicate priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      const result = service.updateConditionalRoute(edge2!.id, {
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('allows same priority change on the same edge', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing same priority allowed',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const updated = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 1,
        isDefault: false,
      });

      expect(updated).toBeDefined();
      expect(updated?.priority).toBe(1);
    });

    it('rejects invalid condition', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing invalid condition rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const result = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'invalid condition text',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects invalid priority', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing invalid priority rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const result = service.updateConditionalRoute(edge!.id, {
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: -1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('rejects second default under same source', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing second default rejection',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const result = service.updateConditionalRoute(edge2!.id, {
        targetStepId: target2.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      expect(result).toBeNull();
    });

    it('returns null when no draft exists', () => {
      const result = service.updateConditionalRoute('edge-123', {
        targetStepId: 'target-456',
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });

    it('returns null when edge does not exist', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing unknown edge',
        type: 'hardcoded-flow',
      });

      const result = service.updateConditionalRoute('non-existent-edge', {
        targetStepId: 'target-456',
        routingType: 'WHEN',
        condition: 'output.x equals Y',
        priority: 1,
        isDefault: false,
      });

      expect(result).toBeNull();
    });
  });

  describe('removeConditionalRoute', () => {
    it('removes a conditional route successfully', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing removal',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const result = service.removeConditionalRoute(edge!.id);

      expect(result).toBe(true);
      expect(service.edges()).toHaveLength(0);
    });

    it('returns false when no draft exists', () => {
      const result = service.removeConditionalRoute('edge-123');

      expect(result).toBe(false);
    });

    it('returns false when edge does not exist', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing unknown edge',
        type: 'hardcoded-flow',
      });

      const result = service.removeConditionalRoute('non-existent-edge');

      expect(result).toBe(false);
      expect(service.edges()).toHaveLength(0);
    });

    it('preserves conditional and steps after removal', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing preservation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      service.removeConditionalRoute(edge!.id);

      expect(service.conditionals()).toHaveLength(1);
      expect(service.steps()).toHaveLength(2);
    });

    it('reconciles input references after removal', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing input reconciliation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, {
        assignedAgentId: 'test-selector',
        inputRefs: [
          { sourceType: 'STEP_OUTPUT', targetInput: 'change', fromStepId: source.id, key: 'change' },
        ],
      });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      service.removeConditionalRoute(edge!.id);

      // Input refs should be reconciled (kept if source is still ancestor)
      const targetStep = service.steps().find((s) => s.id === target.id);
      // Since source is no longer connected, input refs should be cleared
      expect(targetStep?.inputRefs).toHaveLength(0);
    });

    it('removes only the requested edge', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing selective removal',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'WHEN',
        condition: 'output.type equals FEATURE',
        priority: 2,
        isDefault: false,
      });

      service.removeConditionalRoute(edge1!.id);

      expect(service.edges()).toHaveLength(1);
      expect(service.edges()[0]?.id).toBe(edge2!.id);
    });
  });

  describe('replaceConditionalDefaultRoute', () => {
    it('replaces the current default route atomically', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing atomic replacement',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      // Create a WHEN edge
      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      // Create a default edge
      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      // Replace edge1 as default
      const updated = service.replaceConditionalDefaultRoute(edge1!.id);

      expect(updated?.isDefault).toBe(true);
      expect(updated?.routingType).toBe('ALWAYS');
      expect(updated?.condition).toBeNull();
      expect(updated?.priority).toBe(100);

      // Verify old default is no longer default
      const oldDefault = service.edges().find((e) => e.id === edge2!.id);
      expect(oldDefault?.isDefault).toBe(false);
    });

    it('preserves both edge IDs', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing edge ID preservation',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      const edge2 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      const originalEdge1Id = edge1!.id;
      const originalEdge2Id = edge2!.id;

      service.replaceConditionalDefaultRoute(edge1!.id);

      const edges = service.edges();
      expect(edges.some((e) => e.id === originalEdge1Id)).toBe(true);
      expect(edges.some((e) => e.id === originalEdge2Id)).toBe(true);
    });

    it('normalizes input references after replacement', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing normalization',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target1 = service.addStep();
      service.updateStep(target1.id, { assignedAgentId: 'test-selector' });

      const target2 = service.addStep();
      service.updateStep(target2.id, { assignedAgentId: 'deployment-planner' });

      service.addConditional(source.id);
      const conditionalId = service.selectedConditionalId();

      const edge1 = service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target1.id,
        routingType: 'WHEN',
        condition: 'output.type equals BUG',
        priority: 1,
        isDefault: false,
      });

      service.addConditionalRoute({
        conditionalId: conditionalId!,
        targetStepId: target2.id,
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      // Should not error, input refs should be normalized
      const updated = service.replaceConditionalDefaultRoute(edge1!.id);

      expect(updated).toBeDefined();
    });

    it('returns null when edge does not exist', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing unknown edge',
        type: 'hardcoded-flow',
      });

      const result = service.replaceConditionalDefaultRoute('non-existent-edge');

      expect(result).toBeNull();
    });

    it('returns null when source does not own conditional', () => {
      service.createDraft({
        name: 'Workflow',
        description: 'Testing non-conditional source',
        type: 'hardcoded-flow',
      });

      const source = service.addStep();
      service.updateStep(source.id, { assignedAgentId: 'change-classifier' });

      const target = service.addStep();
      service.updateStep(target.id, { assignedAgentId: 'test-selector' });

      // Create a simple edge without conditional
      service.addEdge(source.id, target.id);

      const edge = service.edges()[0];

      const result = service.replaceConditionalDefaultRoute(edge!.id);

      expect(result).toBeNull();
    });
  });
});
