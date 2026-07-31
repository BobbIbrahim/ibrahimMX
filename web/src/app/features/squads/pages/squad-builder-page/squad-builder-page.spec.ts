import { signal, type WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { vi } from 'vitest';

import { AgentService } from '../../../../core/services/agent.service';
import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { SquadService } from '../../../../core/services/squad.service';
import { SquadBuilderPage } from './squad-builder-page';

type BuilderAgent = {
  agentKey: string;
  name: string;
  role: string;
  inputs: string[];
  outputs: string[];
};

describe('SquadBuilderPage', () => {
  let selectedStepSignal: WritableSignal<any | null>;
  let stepsSignal: WritableSignal<any[]>;
  let edgesSignal: WritableSignal<any[]>;
  let agentsSignal: WritableSignal<BuilderAgent[]>;
  let getAncestorStepsSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    selectedStepSignal = signal<any | null>(null);
    stepsSignal = signal<any[]>([]);
    edgesSignal = signal<any[]>([]);
    agentsSignal = signal<BuilderAgent[]>([
      {
        agentKey: 'code-sentinel',
        name: 'Code Sentinel',
        role: 'Code Review Specialist',
        inputs: ['code', 'requirements', 'context'],
        outputs: ['message'],
      },
      {
        agentKey: 'test-weaver',
        name: 'Test Weaver',
        role: 'Test Generation Specialist',
        inputs: ['code', 'requirements', 'testContext'],
        outputs: ['message'],
      },
    ]);
    getAncestorStepsSpy = vi.fn(() => []);

    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null,
              },
            },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: vi.fn(),
          },
        },
        {
          provide: AgentService,
          useValue: {
            getAgents: () => agentsSignal.asReadonly(),
          },
        },
        {
          provide: SquadBuilderStateService,
          useValue: {
            draft: signal(null).asReadonly(),
            steps: stepsSignal.asReadonly(),
            edges: edgesSignal.asReadonly(),
            selectedStep: selectedStepSignal.asReadonly(),
            getAncestorSteps: getAncestorStepsSpy,
            updateSelectedStepInputRef: vi.fn(),
            updateSelectedStep: vi.fn(),
            addSelectedStepInputRef: vi.fn(),
            addSelectedStepManualInputRef: vi.fn(),
            addStep: vi.fn(),
            deleteSelectedStep: vi.fn(),
            removeSelectedStepInputRef: vi.fn(),
            addEdge: vi.fn(),
            removeEdge: vi.fn(),
            updateStepPosition: vi.fn(),
            selectStep: vi.fn(),
            buildSavePayload: vi.fn(() => null),
            resetDraft: vi.fn(),
            loadDraftFromApi: vi.fn(),
          },
        },
        {
          provide: SquadService,
          useValue: {},
        },
      ],
    });
  });

  it('filters out target inputs already used by other mappings', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: 'test-weaver',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [
        { targetInput: 'code', fromStepId: 'step-1', key: 'message' },
        { targetInput: 'requirements', fromStepId: 'step-1', key: 'summary' },
      ],
    });

    expect(component.getAvailableTargetInputsForInputRef(0)).toEqual(['code', 'testContext']);
  });

  it('disables adding input refs when there is no assigned agent', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: null,
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([
      {
        id: 'step-1',
        name: 'Step 1',
        assignedAgentId: 'code-sentinel',
        parameters: {},
        position: { x: 0, y: 0 },
        inputRefs: [],
      },
    ]);

    expect(component.canAddSelectedStepInputRef()).toBe(false);
  });

  it('disables adding input refs when there are no ancestor steps', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: 'test-weaver',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([]);

    expect(component.canAddSelectedStepInputRef()).toBe(false);
  });

  it('disables adding input refs when every agent input is already mapped', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: 'test-weaver',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [
        { targetInput: 'code', fromStepId: 'step-1', key: 'message' },
        { targetInput: 'requirements', fromStepId: 'step-1', key: 'summary' },
        { targetInput: 'testContext', fromStepId: 'step-1', key: 'message' },
      ],
    });
    getAncestorStepsSpy.mockReturnValue([
      {
        id: 'step-1',
        name: 'Step 1',
        assignedAgentId: 'code-sentinel',
        parameters: {},
        position: { x: 0, y: 0 },
        inputRefs: [],
      },
    ]);

    expect(component.canAddSelectedStepInputRef()).toBe(false);
  });

  it('identifies selected step as root when it has no ancestors', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: 'code-sentinel',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([]);

    expect(component.isSelectedStepRoot()).toBe(true);
  });

  it('identifies selected step as non-root when it has ancestors', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: 'test-weaver',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([
      {
        id: 'step-1',
        name: 'Step 1',
        assignedAgentId: 'code-sentinel',
        parameters: {},
        position: { x: 0, y: 0 },
        inputRefs: [],
      },
    ]);

    expect(component.isSelectedStepRoot()).toBe(false);
  });

  it('enables adding manual input when step is root with agent and unmapped inputs', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: 'code-sentinel',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([]);

    expect(component.canAddSelectedStepManualInputRef()).toBe(true);
  });

  it('disables adding manual input when step is not root', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-2',
      name: 'Step 2',
      assignedAgentId: 'test-weaver',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([
      {
        id: 'step-1',
        name: 'Step 1',
        assignedAgentId: 'code-sentinel',
        parameters: {},
        position: { x: 0, y: 0 },
        inputRefs: [],
      },
    ]);

    expect(component.canAddSelectedStepManualInputRef()).toBe(false);
  });

  it('disables adding manual input when root step has no agent assigned', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: null,
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });
    getAncestorStepsSpy.mockReturnValue([]);

    expect(component.canAddSelectedStepManualInputRef()).toBe(false);
  });

  it('disables adding manual input when all agent inputs are already mapped', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: 'code-sentinel',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [
        { targetInput: 'code', sourceType: 'MANUAL' },
        { targetInput: 'requirements', sourceType: 'MANUAL' },
        { targetInput: 'context', sourceType: 'MANUAL' },
      ],
    });
    getAncestorStepsSpy.mockReturnValue([]);

    expect(component.canAddSelectedStepManualInputRef()).toBe(false);
  });

  function createComponent(): SquadBuilderPage {
    return TestBed.runInInjectionContext(() => new SquadBuilderPage());
  }
});
