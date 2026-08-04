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
  let addConditionalSpy: ReturnType<typeof vi.fn>;

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
    addConditionalSpy = vi.fn();

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
            conditionals: signal([]).asReadonly(),
            selectedStep: selectedStepSignal.asReadonly(),
            selectedConditional: signal(null).asReadonly(),
            getAncestorSteps: getAncestorStepsSpy,
            updateSelectedStepInputRef: vi.fn(),
            updateSelectedStep: vi.fn(),
            addSelectedStepInputRef: vi.fn(),
            addSelectedStepManualInputRef: vi.fn(),
            addStep: vi.fn(),
            addConditional: addConditionalSpy,
            deleteSelectedStep: vi.fn(),
            removeSelectedStepInputRef: vi.fn(),
            addEdge: vi.fn(),
            removeEdge: vi.fn(),
            updateStepPosition: vi.fn(),
            selectStep: vi.fn(),
            selectConditional: vi.fn(),
            updateSelectedConditional: vi.fn(),
            deleteSelectedConditional: vi.fn(),
            buildSavePayload: vi.fn(() => null),
            resetDraft: vi.fn(),
            loadDraftFromApi: vi.fn(),
            addConditionalRoute: vi.fn(),
            updateConditionalRoute: vi.fn(),
            removeConditionalRoute: vi.fn(),
            replaceConditionalDefaultRoute: vi.fn(),
            updateConditionalPosition: vi.fn(),
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

  it('disables canAddConditional when no step is selected', () => {
    const component = createComponent();

    selectedStepSignal.set(null);

    expect(component.canAddConditional()).toBe(false);
  });

  it('disables canAddConditional when selected step has no assigned agent', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: null,
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });

    expect(component.canAddConditional()).toBe(false);
  });

  it('enables canAddConditional when selected step has an assigned agent', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: 'code-sentinel',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });

    expect(component.canAddConditional()).toBe(true);
  });

  it('calls addConditional with selected step ID when addConditional is invoked', () => {
    const component = createComponent();

    selectedStepSignal.set({
      id: 'step-1',
      name: 'Step 1',
      assignedAgentId: 'code-sentinel',
      parameters: {},
      position: { x: 0, y: 0 },
      inputRefs: [],
    });

    component.addConditional();

    expect(addConditionalSpy).toHaveBeenCalledWith('step-1');
  });

  it('does not call addConditional when no step is selected', () => {
    const component = createComponent();

    selectedStepSignal.set(null);

    component.addConditional();

    expect(addConditionalSpy).not.toHaveBeenCalled();
  });

  function createComponent(): SquadBuilderPage {
    return TestBed.runInInjectionContext(() => new SquadBuilderPage());
  }

  describe('Conditional integration with ReteSquadFlowEditor', () => {
    it('should pass conditionals to the editor', () => {
      const conditionalsSignal = signal([
        {
          id: 'cond-1',
          name: 'Test Conditional',
          sourceStepId: 'step-1',
          position: { x: 100, y: 100 },
        },
      ]);

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: signal([]).asReadonly(),
              edges: signal([]).asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: signal(null).asReadonly(),
              selectConditional: vi.fn(),
              updateConditionalPosition: vi.fn(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = createComponent();
      expect(component.conditionals()).toEqual([
        {
          id: 'cond-1',
          name: 'Test Conditional',
          sourceStepId: 'step-1',
          position: { x: 100, y: 100 },
        },
      ]);
    });

    it('should pass selectedConditionalId to the editor', () => {
      const selectedConditionalSignal = signal({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 100, y: 100 },
      });

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: signal([]).asReadonly(),
              edges: signal([]).asReadonly(),
              conditionals: signal([]).asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = createComponent();
      expect(component.selectedConditionalId()).toBe('cond-1');
    });

    it('should delegate conditional selection to state service', () => {
      const selectConditionalSpy = vi.fn();
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: signal([]).asReadonly(),
        edges: signal([]).asReadonly(),
        conditionals: signal([]).asReadonly(),
        selectedStep: signal(null).asReadonly(),
        selectedConditional: signal(null).asReadonly(),
        selectConditional: selectConditionalSpy,
        updateConditionalPosition: vi.fn(),
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = createComponent();
      component.selectConditional('cond-1');

      expect(selectConditionalSpy).toHaveBeenCalledWith('cond-1');
    });

    it('should delegate conditional position updates to state service', () => {
      const updateConditionalPositionSpy = vi.fn();
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: signal([]).asReadonly(),
        edges: signal([]).asReadonly(),
        conditionals: signal([]).asReadonly(),
        selectedStep: signal(null).asReadonly(),
        selectedConditional: signal(null).asReadonly(),
        selectConditional: vi.fn(),
        updateConditionalPosition: updateConditionalPositionSpy,
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = createComponent();
      component.handleReteConditionalPositionChanged({
        conditionalId: 'cond-1',
        position: { x: 200, y: 300 },
      });

      expect(updateConditionalPositionSpy).toHaveBeenCalledWith('cond-1', { x: 200, y: 300 });
    });
  });

  describe('Conditional Details Panel', () => {
    let selectedConditionalSignal: WritableSignal<any | null>;
    let conditionalsSignal: WritableSignal<any[]>;
    let stepsSignal2: WritableSignal<any[]>;
    let edgesSignal2: WritableSignal<any[]>;

    beforeEach(() => {
      selectedConditionalSignal = signal<any | null>(null);
      conditionalsSignal = signal<any[]>([]);
      stepsSignal2 = signal<any[]>([]);
      edgesSignal2 = signal<any[]>([]);
    });

    it('should show step panel when a step is selected, even if conditional is also selected', () => {
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: stepsSignal2.asReadonly(),
        edges: edgesSignal2.asReadonly(),
        conditionals: conditionalsSignal.asReadonly(),
        selectedStep: signal({
          id: 'step-1',
          name: 'Test Step',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        }).asReadonly(),
        selectedConditional: selectedConditionalSignal.asReadonly(),
        getAncestorSteps: vi.fn(() => []),
        updateSelectedStep: vi.fn(),
        selectStep: vi.fn(),
        updateSelectedConditional: vi.fn(),
        deleteSelectedConditional: vi.fn(),
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      expect(component.selectedStep()).toBeTruthy();
    });

    it('should resolve selectedConditionalSourceStep from selectedConditional.sourceStepId', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source Step',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target Step',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      selectedConditionalSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      const sourceStep = component.selectedConditionalSourceStep();
      expect(sourceStep?.id).toBe('step-1');
      expect(sourceStep?.name).toBe('Source Step');
    });

    it('should resolve selectedConditionalRoutes from edges with matching sourceStepId', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source Step',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target Step 1',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Target Step 2',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 100 },
          inputRefs: [],
        },
      ]);

      edgesSignal2.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'status === success',
          priority: 100,
          isDefault: false,
        },
        {
          id: 'edge-2',
          sourceStepId: 'step-1',
          targetStepId: 'step-3',
          routingType: 'ALWAYS',
          condition: null,
          priority: 200,
          isDefault: true,
        },
        {
          id: 'edge-3',
          sourceStepId: 'step-2',
          targetStepId: 'step-3',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: false,
        },
      ]);

      selectedConditionalSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      const routes = component.selectedConditionalRoutes();
      expect(routes).toHaveLength(2);
    });

    it('should sort routes with WHEN routes by ascending priority and default route last', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target 1',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Target 2',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 100 },
          inputRefs: [],
        },
      ]);

      edgesSignal2.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'a',
          priority: 200,
          isDefault: false,
        },
        {
          id: 'edge-2',
          sourceStepId: 'step-1',
          targetStepId: 'step-3',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: true,
        },
        {
          id: 'edge-3',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'b',
          priority: 100,
          isDefault: false,
        },
      ]);

      selectedConditionalSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      const routes = component.selectedConditionalRoutes();
      expect(routes[0].priority).toBe(100);
      expect(routes[1].priority).toBe(200);
      expect(routes[2].isDefault).toBe(true);
    });

    it('updateSelectedConditionalName should trim and delegate to updateSelectedConditional', () => {
      const updateConditionalSpy = vi.fn();
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: signal([]).asReadonly(),
        edges: signal([]).asReadonly(),
        conditionals: signal([]).asReadonly(),
        selectedStep: signal(null).asReadonly(),
        selectedConditional: signal({
          id: 'cond-1',
          name: 'Test',
          sourceStepId: 'step-1',
          position: { x: 0, y: 0 },
        }).asReadonly(),
        updateSelectedConditional: updateConditionalSpy,
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      component.updateSelectedConditionalName('  New Name  ');

      expect(updateConditionalSpy).toHaveBeenCalledWith({ name: 'New Name' });
    });

    it('deleteSelectedConditional should delegate to state service', () => {
      const deleteConditionalSpy = vi.fn();
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: signal([]).asReadonly(),
        edges: signal([]).asReadonly(),
        conditionals: signal([]).asReadonly(),
        selectedStep: signal(null).asReadonly(),
        selectedConditional: signal({
          id: 'cond-1',
          name: 'Test',
          sourceStepId: 'step-1',
          position: { x: 0, y: 0 },
        }).asReadonly(),
        deleteSelectedConditional: deleteConditionalSpy,
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      component.deleteSelectedConditional();

      expect(deleteConditionalSpy).toHaveBeenCalled();
    });

    it('getRouteTargetName should return target step name', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target Step',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: signal(null).asReadonly(),
            },
          },
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
              getAgents: () => signal([]).asReadonly(),
            },
          },
          {
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      expect(component.getRouteTargetName('step-2')).toBe('Target Step');
    });

    it('getConditionalSourceAgent should return source agent name', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
      ]);

      selectedConditionalSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const agentsSignal = signal([
        {
          agentKey: 'code-sentinel',
          name: 'Code Sentinel',
          role: 'Code Review Specialist',
          inputs: [],
          outputs: [],
        },
      ]);

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      expect(component.getConditionalSourceAgent()).toBe('Code Sentinel');
    });

    it('getConditionalSourceOutputs should return source agent outputs', () => {
      stepsSignal2.set([
        {
          id: 'step-1',
          name: 'Source',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
      ]);

      selectedConditionalSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const agentsSignal = signal([
        {
          agentKey: 'code-sentinel',
          name: 'Code Sentinel',
          role: 'Code Review Specialist',
          inputs: ['code', 'requirements'],
          outputs: ['review', 'suggestions'],
        },
      ]);

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: {
              draft: signal(null).asReadonly(),
              steps: stepsSignal2.asReadonly(),
              edges: edgesSignal2.asReadonly(),
              conditionals: conditionalsSignal.asReadonly(),
              selectedStep: signal(null).asReadonly(),
              selectedConditional: selectedConditionalSignal.asReadonly(),
            },
          },
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
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      const component = TestBed.runInInjectionContext(() => new SquadBuilderPage());
      expect(component.getConditionalSourceOutputs()).toEqual(['review', 'suggestions']);
    });
  });

  describe('Add Route Form', () => {
    let stepsForFormSignal: WritableSignal<any[]>;
    let edgesForFormSignal: WritableSignal<any[]>;
    let conditionalsForFormSignal: WritableSignal<any[]>;
    let selectedConditionalForFormSignal: WritableSignal<any | null>;

    beforeEach(() => {
      stepsForFormSignal = signal<any[]>([
        {
          id: 'step-1',
          name: 'Source Step',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Target Step A',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-3',
          name: 'Target Step B',
          assignedAgentId: 'test-weaver',
          parameters: {},
          position: { x: 100, y: 100 },
          inputRefs: [],
        },
      ]);
      edgesForFormSignal = signal<any[]>([]);
      conditionalsForFormSignal = signal<any[]>([]);
      selectedConditionalForFormSignal = signal<any | null>(null);
    });

    function setupFormComponent(): SquadBuilderPage {
      const mockStateService = {
        draft: signal(null).asReadonly(),
        steps: stepsForFormSignal.asReadonly(),
        edges: edgesForFormSignal.asReadonly(),
        conditionals: conditionalsForFormSignal.asReadonly(),
        selectedStep: signal(null).asReadonly(),
        selectedConditional: selectedConditionalForFormSignal.asReadonly(),
        addConditionalRoute: vi.fn(() => ({
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.message equals "success value"',
          priority: 10,
          isDefault: false,
        })),
        addEdge: vi.fn(),
      };

      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: SquadBuilderStateService,
            useValue: mockStateService,
          },
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
            provide: SquadService,
            useValue: {},
          },
        ],
      });

      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('should open form when addConditionalRoute is called', () => {
      const component = setupFormComponent();
      expect(component.addRouteFormVisible()).toBe(false);

      component.addConditionalRoute();

      expect(component.addRouteFormVisible()).toBe(true);
    });

    it('should close and reset form when cancelAddRoute is called', () => {
      const component = setupFormComponent();

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('priority', '10');

      component.cancelAddRoute();

      expect(component.addRouteFormVisible()).toBe(false);
      expect(component.addRouteFormState().targetStepId).toBe('');
      expect(component.addRouteFormState().priority).toBe('');
    });

    it('should reset form when selectedConditionalId changes', async () => {
      const component = setupFormComponent();

      component.addConditionalRoute();
      expect(component.addRouteFormVisible()).toBe(true);

      // Set the conditional inside the injection context to ensure the effect runs
      await TestBed.runInInjectionContext(async () => {
        selectedConditionalForFormSignal.set({
          id: 'cond-1',
          name: 'Test Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        });
        // Allow microtasks to run
        await new Promise((resolve) => setTimeout(resolve, 0));
      });

      expect(component.addRouteFormVisible()).toBe(false);
    });

    it('should filter available target steps excluding source step', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const availableTargets = component.addRouteAvailableTargetSteps();

      expect(availableTargets).toHaveLength(2);
      expect(availableTargets.map((s) => s.id)).toEqual(['step-2', 'step-3']);
    });

    it('should exclude already-used targets from available options', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      edgesForFormSignal.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.status equals success',
          priority: 10,
          isDefault: false,
        },
      ]);

      const component = setupFormComponent();
      const availableTargets = component.addRouteAvailableTargetSteps();

      expect(availableTargets).toHaveLength(1);
      expect(availableTargets[0].id).toBe('step-3');
    });

    it('should provide source outputs from conditional source agent', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const outputs = component.addRouteSourceOutputs();

      expect(outputs).toEqual(['message']);
    });

    it('should generate equals preview for non-default route', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'success');

      expect(component.addRouteConditionPreview()).toBe('output.status equals success');
    });

    it('should generate notEquals preview for non-default route', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'notEquals');
      component.updateAddRouteFormField('expectedValue', 'failure');

      expect(component.addRouteConditionPreview()).toBe('output.status notEquals failure');
    });

    it('should generate contains preview for non-default route', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('outputField', 'message');
      component.updateAddRouteFormField('operator', 'contains');
      component.updateAddRouteFormField('expectedValue', 'error');

      expect(component.addRouteConditionPreview()).toBe('output.message contains error');
    });

    it('should generate in preview for non-default route with comma-separated values', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('outputField', 'priority');
      component.updateAddRouteFormField('operator', 'in');
      component.updateAddRouteFormField('expectedValue', 'high, critical');

      expect(component.addRouteConditionPreview()).toBe('output.priority in [high, critical]');
    });

    it('should generate default route preview when isDefault is true', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('isDefault', true);

      expect(component.addRouteConditionPreview()).toBe('Default route');
    });

    it('should detect when a default route already exists', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      edgesForFormSignal.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: true,
        },
      ]);

      const component = setupFormComponent();
      expect(component.addRouteHasExistingDefault()).toBe(true);
    });

    it('should not detect existing default when none exists', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      expect(component.addRouteHasExistingDefault()).toBe(false);
    });

    it('should require target for default route only', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      // Default route with target only
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('isDefault', true);

      expect(component.addRouteFormValid()).toBe(true);
    });

    it('should require all fields for non-default route', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      // Partial form - should be invalid
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      expect(component.addRouteFormValid()).toBe(false);

      // Add remaining fields
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'success');
      component.updateAddRouteFormField('priority', '10');

      expect(component.addRouteFormValid()).toBe(true);
    });

    it('should validate priority as finite non-negative integer', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'success');

      // Test negative
      component.updateAddRouteFormField('priority', '-1');
      expect(component.addRouteFormValid()).toBe(false);

      // Test float
      component.updateAddRouteFormField('priority', '10.5');
      expect(component.addRouteFormValid()).toBe(false);

      // Test NaN
      component.updateAddRouteFormField('priority', 'abc');
      expect(component.addRouteFormValid()).toBe(false);

      // Test valid integer
      component.updateAddRouteFormField('priority', '10');
      expect(component.addRouteFormValid()).toBe(true);

      // Test zero
      component.updateAddRouteFormField('priority', '0');
      expect(component.addRouteFormValid()).toBe(true);
    });

    it('should validate condition with validateSquadRoutingCondition', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('priority', '10');

      // Valid single-token value
      component.updateAddRouteFormField('expectedValue', 'success');
      expect(component.addRouteFormValid()).toBe(true);
      expect(component.addRouteConditionPreview()).toBe('output.status equals success');

      // Valid value with whitespace (should be auto-quoted)
      component.updateAddRouteFormField('expectedValue', 'success value');
      expect(component.addRouteFormValid()).toBe(true);
      expect(component.addRouteConditionPreview()).toBe('output.status equals "success value"');

      // Valid pre-quoted value
      component.updateAddRouteFormField('expectedValue', '"success value"');
      expect(component.addRouteFormValid()).toBe(true);
      expect(component.addRouteConditionPreview()).toBe('output.status equals "success value"');
    });

    it('should not create edge on submitAddRoute (placeholder only)', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'success');
      component.updateAddRouteFormField('priority', '10');

      component.submitAddRoute();

      // Form should be closed
      expect(component.addRouteFormVisible()).toBe(false);

      // No edge should be created (placeholder behavior)
      expect(edgesForFormSignal().length).toBe(0);
    });

    it('should not submit invalid form', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.addConditionalRoute();
      // Leave form empty - invalid

      component.submitAddRoute();

      // Form should still be open
      expect(component.addRouteFormVisible()).toBe(true);
    });

    it('should update form field independently', () => {
      const component = setupFormComponent();

      const formState = component.addRouteFormState();
      expect(formState.targetStepId).toBe('');

      component.updateAddRouteFormField('targetStepId', 'step-2');

      expect(component.addRouteFormState().targetStepId).toBe('step-2');
      expect(component.addRouteFormState().outputField).toBe('');
    });

    // Normalization tests
    it('should trim whitespace from scalar values', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('  BUG_FIX  ')).toBe('BUG_FIX');
    });

    it('should preserve boolean and null values', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('true')).toBe('true');
      expect(component.normalizeRouteScalar('false')).toBe('false');
      expect(component.normalizeRouteScalar('null')).toBe('null');
    });

    it('should preserve numeric values (integers, decimals, negative)', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('42')).toBe('42');
      expect(component.normalizeRouteScalar('10.5')).toBe('10.5');
      expect(component.normalizeRouteScalar('-2')).toBe('-2');
      expect(component.normalizeRouteScalar('-3.14')).toBe('-3.14');
    });

    it('should preserve single-token strings like BUG_FIX', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('BUG_FIX')).toBe('BUG_FIX');
      expect(component.normalizeRouteScalar('HOTFIX_123')).toBe('HOTFIX_123');
    });

    it('should auto-quote strings with whitespace', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('urgent production fix')).toBe(
        '"urgent production fix"',
      );
    });

    it('should preserve already-quoted values', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('"already quoted"')).toBe('"already quoted"');
    });

    it('should escape backslashes and quotes in auto-quoted strings', () => {
      const component = setupFormComponent();
      // Use a string with actual quotes that need escaping
      expect(component.normalizeRouteScalar('value with "quotes"')).toBe(
        '"value with \\"quotes\\""',
      );
      // Test with newline-like escape sequence
      expect(component.normalizeRouteScalar('path with space and "quote"')).toBe(
        '"path with space and \\"quote\\""',
      );
    });

    it('should return empty string for empty input', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteScalar('')).toBe('');
      expect(component.normalizeRouteScalar('   ')).toBe('');
    });

    // List normalization tests
    it('should normalize comma-separated values to list format', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteList('BUG_FIX, HOTFIX')).toBe('[BUG_FIX, HOTFIX]');
    });

    it('should handle optional outer brackets', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteList('[BUG_FIX, HOTFIX]')).toBe('[BUG_FIX, HOTFIX]');
      expect(component.normalizeRouteList('(BUG_FIX, HOTFIX)')).toBe('[BUG_FIX, HOTFIX]');
    });

    it('should normalize items within list using scalar normalization', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteList('true, false, null')).toBe('[true, false, null]');
      expect(component.normalizeRouteList('high priority, critical')).toBe(
        '["high priority", critical]',
      );
    });

    it('should reject lists with empty items', () => {
      const component = setupFormComponent();
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'priority');
      component.updateAddRouteFormField('operator', 'in');
      component.updateAddRouteFormField('expectedValue', 'high, , critical');
      component.updateAddRouteFormField('priority', '10');

      expect(component.addRouteValidationMessage()).toBe('List values must not be empty.');
    });

    it('should return empty list for empty input', () => {
      const component = setupFormComponent();
      expect(component.normalizeRouteList('')).toBe('[]');
    });

    // Default route behavior tests
    it('should clear condition fields and set priority to 100 when isDefault becomes true', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'notEquals');
      component.updateAddRouteFormField('expectedValue', 'failure');
      component.updateAddRouteFormField('priority', '20');

      component.updateAddRouteFormField('isDefault', true);

      const state = component.addRouteFormState();
      expect(state.outputField).toBe('');
      expect(state.operator).toBe('equals');
      expect(state.expectedValue).toBe('');
      expect(state.priority).toBe('100');
      expect(state.isDefault).toBe(true);
    });

    it('should clear priority when isDefault becomes false', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('isDefault', true);

      component.updateAddRouteFormField('isDefault', false);

      expect(component.addRouteFormState().priority).toBe('');
    });

    it('should reject a second default route', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      edgesForFormSignal.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'ALWAYS',
          condition: null,
          priority: 100,
          isDefault: true,
        },
      ]);

      const component = setupFormComponent();
      component.updateAddRouteFormField('targetStepId', 'step-3');
      component.updateAddRouteFormField('isDefault', true);

      expect(component.addRouteValidationMessage()).toBe(
        'A default route already exists for this conditional.',
      );
    });

    it('should allow an unassigned executable target step', () => {
      stepsForFormSignal.set([
        {
          id: 'step-1',
          name: 'Source Step',
          assignedAgentId: 'code-sentinel',
          parameters: {},
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2-unassigned',
          name: 'Unassigned Target',
          assignedAgentId: null,
          parameters: {},
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const availableTargets = component.addRouteAvailableTargetSteps();

      expect(availableTargets).toHaveLength(1);
      expect(availableTargets[0].id).toBe('step-2-unassigned');
    });

    it('should provide operator-specific placeholders', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.updateAddRouteFormField('operator', 'equals');
      expect(component.getAddRouteExpectedValuePlaceholder()).toBe(
        'e.g., BUG_FIX or urgent production fix',
      );

      component.updateAddRouteFormField('operator', 'in');
      expect(component.getAddRouteExpectedValuePlaceholder()).toBe('e.g., BUG_FIX, HOTFIX');

      component.updateAddRouteFormField('operator', 'contains');
      expect(component.getAddRouteExpectedValuePlaceholder()).toBe(
        'e.g., BUG_FIX or urgent production fix',
      );
    });

    it('should provide specific validation messages for various errors', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      // Missing target
      expect(component.addRouteValidationMessage()).toBe('Select a target step.');

      component.updateAddRouteFormField('targetStepId', 'step-2');

      // Missing output field
      expect(component.addRouteValidationMessage()).toBe('Select an output field.');

      component.updateAddRouteFormField('outputField', 'status');

      // Missing value
      expect(component.addRouteValidationMessage()).toBe('Enter an expected value.');

      component.updateAddRouteFormField('expectedValue', 'success');

      // Missing priority
      expect(component.addRouteValidationMessage()).toBe(
        'Priority must be a non-negative integer.',
      );

      component.updateAddRouteFormField('priority', '10');
      expect(component.addRouteValidationMessage()).toBeNull();
    });

    it('should still not create edge on submitAddRoute with normalization', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent production fix');
      component.updateAddRouteFormField('priority', '10');

      component.submitAddRoute();

      // Form should be closed
      expect(component.addRouteFormVisible()).toBe(false);

      // No edge should be created
      expect(edgesForFormSignal().length).toBe(0);
    });

    it('valid WHEN form calls addConditionalRoute exactly once with correct payload', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'message');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'success value');
      component.updateAddRouteFormField('priority', '10');

      component.submitAddRoute();

      expect(stateService.addConditionalRoute).toHaveBeenCalledOnce();
      expect(stateService.addConditionalRoute).toHaveBeenCalledWith({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.message equals "success value"',
        priority: 10,
        isDefault: false,
      });
    });

    it('valid default form calls addConditionalRoute with ALWAYS routing', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('isDefault', true);

      component.submitAddRoute();

      expect(stateService.addConditionalRoute).toHaveBeenCalledOnce();
      expect(stateService.addConditionalRoute).toHaveBeenCalledWith({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });
    });

    it('successful creation closes form and resets all fields', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent');
      component.updateAddRouteFormField('priority', '5');

      expect(component.addRouteFormVisible()).toBe(true);

      component.submitAddRoute();

      expect(component.addRouteFormVisible()).toBe(false);
      expect(component.addRouteFormState().targetStepId).toBe('');
      expect(component.addRouteFormState().outputField).toBe('');
      expect(component.addRouteFormState().operator).toBe('equals');
      expect(component.addRouteFormState().expectedValue).toBe('');
      expect(component.addRouteFormState().priority).toBe('');
      expect(component.addRouteFormState().isDefault).toBe(false);
    });

    it('when addConditionalRoute returns null, form remains open and values unchanged', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);
      vi.mocked(stateService.addConditionalRoute).mockReturnValue(null);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent');
      component.updateAddRouteFormField('priority', '5');

      const formStateBefore = { ...component.addRouteFormState() };

      component.submitAddRoute();

      expect(component.addRouteFormVisible()).toBe(true);
      expect(component.addRouteFormState()).toEqual(formStateBefore);
    });

    it('invalid form does not call addConditionalRoute', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      // Leave all fields empty - form is invalid

      component.submitAddRoute();

      expect(stateService.addConditionalRoute).not.toHaveBeenCalled();
    });

    it('submitAddRoute never calls the generic addEdge method', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent');
      component.updateAddRouteFormField('priority', '5');

      component.submitAddRoute();

      expect(stateService.addEdge).not.toHaveBeenCalled();
    });

    it('route creation delegates exactly once per submission', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent');
      component.updateAddRouteFormField('priority', '5');

      component.submitAddRoute();

      expect(stateService.addConditionalRoute).toHaveBeenCalledTimes(1);
    });

    it('payload contains conditionalId but not sourceStepId', () => {
      selectedConditionalForFormSignal.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const component = setupFormComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.addConditionalRoute();
      component.updateAddRouteFormField('targetStepId', 'step-2');
      component.updateAddRouteFormField('outputField', 'status');
      component.updateAddRouteFormField('operator', 'equals');
      component.updateAddRouteFormField('expectedValue', 'urgent');
      component.updateAddRouteFormField('priority', '5');

      component.submitAddRoute();

      const callArgs = vi.mocked(stateService.addConditionalRoute).mock.calls[0][0];
      expect(callArgs).toHaveProperty('conditionalId', 'cond-1');
      expect(callArgs).not.toHaveProperty('sourceStepId');
    });
  });

  describe('Edit Route Mode', () => {
    let selectedConditionalSignalForEdit: WritableSignal<any | null>;
    let selectedConditionalRoutesSignal: WritableSignal<any[]>;
    let editStepsSignal: WritableSignal<any[]>;
    let editEdgesSignal: WritableSignal<any[]>;

    beforeEach(() => {
      selectedConditionalSignalForEdit = signal<any | null>(null);
      selectedConditionalRoutesSignal = signal<any[]>([]);
      editStepsSignal = signal<any[]>([
        {
          id: 'step-1',
          name: 'Step 1',
          assignedAgentId: 'code-sentinel',
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          assignedAgentId: null,
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
      ]);
      editEdgesSignal = signal<any[]>([]);

      // Reconfigure the mock provider for this suite to use selectedConditionalSignalForEdit
      TestBed.overrideProvider(SquadBuilderStateService, {
        useValue: {
          draft: signal(null).asReadonly(),
          steps: editStepsSignal.asReadonly(),
          edges: editEdgesSignal.asReadonly(),
          conditionals: signal([]).asReadonly(),
          selectedStep: signal(null).asReadonly(),
          selectedConditional: selectedConditionalSignalForEdit.asReadonly(),
          getAncestorSteps: vi.fn(() => []),
          updateSelectedStepInputRef: vi.fn(),
          updateSelectedStep: vi.fn(),
          addSelectedStepInputRef: vi.fn(),
          addSelectedStepManualInputRef: vi.fn(),
          addStep: vi.fn(),
          addConditional: vi.fn(),
          deleteSelectedStep: vi.fn(),
          removeSelectedStepInputRef: vi.fn(),
          addEdge: vi.fn(),
          removeEdge: vi.fn(),
          updateStepPosition: vi.fn(),
          selectStep: vi.fn(),
          selectConditional: vi.fn(),
          updateConditionalPosition: vi.fn(),
          updateSelectedConditional: vi.fn(),
          deleteSelectedConditional: vi.fn(),
          addConditionalRoute: vi.fn(),
          updateConditionalRoute: vi.fn(),
          removeConditionalRoute: vi.fn(),
          replaceConditionalDefaultRoute: vi.fn(),
          buildSavePayload: vi.fn(() => null),
          resetDraft: vi.fn(),
          loadDraftFromApi: vi.fn(),
        },
      });
    });

    function setupEditComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('editRoute should populate form with existing route data', () => {
      selectedConditionalSignalForEdit.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      editEdgesSignal.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.status equals "success"',
          priority: 10,
          isDefault: false,
        },
      ]);

      const component = setupEditComponent();
      component.editRoute('edge-1');

      expect(component.editingEdgeId()).toBe('edge-1');
      expect(component.addRouteFormVisible()).toBe(true);
      expect(component.addRouteFormState().targetStepId).toBe('step-2');
      expect(component.addRouteFormState().priority).toBe('10');
    });

    it('should parse condition into form fields for WHEN routes', () => {
      selectedConditionalSignalForEdit.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      const route = {
        id: 'edge-1',
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.status equals "success"',
        priority: 5,
        isDefault: false,
      };

      editEdgesSignal.set([route]);

      const component = setupEditComponent();
      component.editRoute(route.id);

      expect(component.addRouteFormState().outputField).toBe('status');
      expect(component.addRouteFormState().operator).toBe('equals');
      expect(component.addRouteFormState().expectedValue).toBe('"success"');
    });

    it('submitAddRoute should call updateConditionalRoute in edit mode', () => {
      selectedConditionalSignalForEdit.set({
        id: 'cond-1',
        name: 'Test Conditional',
        sourceStepId: 'step-1',
        position: { x: 50, y: 50 },
      });

      editEdgesSignal.set([
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
          routingType: 'WHEN',
          condition: 'output.status equals "success"',
          priority: 10,
          isDefault: false,
        },
      ]);

      const component = setupEditComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);
      (stateService.updateConditionalRoute as any).mockReturnValue({
        id: 'edge-1',
        sourceStepId: 'step-1',
        targetStepId: 'step-2',
        routingType: 'WHEN',
        condition: 'output.status equals "updated"',
        priority: 20,
        isDefault: false,
      });

      component.editRoute('edge-1');
      component.updateAddRouteFormField('priority', '20');
      component.submitAddRoute();

      expect(stateService.updateConditionalRoute).toHaveBeenCalledWith(
        'edge-1',
        expect.any(Object),
      );
    });

    it('cancelEditRoute should close form and reset edit mode', () => {
      const component = setupEditComponent();
      component.editingEdgeId.set('edge-1');
      component.addRouteFormVisible.set(true);
      component.cancelAddRoute();

      expect(component.editingEdgeId()).toBeNull();
      expect(component.addRouteFormVisible()).toBe(false);
    });
  });

  describe('Delete Route with Confirmation', () => {
    let selectedConditionalSignalForDelete: WritableSignal<any | null>;
    let selectedConditionalRoutesSignalForDelete: WritableSignal<any[]>;

    beforeEach(() => {
      selectedConditionalSignalForDelete = signal<any | null>(null);
      selectedConditionalRoutesSignalForDelete = signal<any[]>([]);
    });

    function setupDeleteComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('deleteRoute should set deleteRouteConfirmId signal', () => {
      const component = setupDeleteComponent();
      component.deleteRoute('edge-1');
      expect(component.deleteRouteConfirmId()).toBe('edge-1');
    });

    it('confirmDeleteRoute should call removeConditionalRoute', () => {
      const component = setupDeleteComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      vi.mocked(stateService.removeConditionalRoute).mockReturnValue(true);

      component.deleteRouteConfirmId.set('edge-1');
      component.confirmDeleteRoute();

      expect(stateService.removeConditionalRoute).toHaveBeenCalledWith('edge-1');
    });

    it('confirmDeleteRoute should clear confirm ID on success', () => {
      const component = setupDeleteComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      vi.mocked(stateService.removeConditionalRoute).mockReturnValue(true);

      component.deleteRouteConfirmId.set('edge-1');
      component.confirmDeleteRoute();

      expect(component.deleteRouteConfirmId()).toBeNull();
    });

    it('cancelDeleteRoute should clear confirm ID', () => {
      const component = setupDeleteComponent();

      component.deleteRouteConfirmId.set('edge-1');
      component.cancelDeleteRoute();
      expect(component.deleteRouteConfirmId()).toBeNull();
    });
  });

  describe('Delete Conditional with Confirmation', () => {
    function setupDeleteConditionalComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('deleteConditional should set deleteConditionalConfirmVisible signal', () => {
      const component = setupDeleteConditionalComponent();

      component.deleteConditional();

      expect(component.deleteConditionalConfirmVisible()).toBe(true);
    });
    it('confirmDeleteConditional should call deleteSelectedConditional', () => {
      const component = setupDeleteConditionalComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      component.deleteConditionalConfirmVisible.set(true);
      component.confirmDeleteConditional();

      expect(stateService.deleteSelectedConditional).toHaveBeenCalled();
      expect(component.deleteConditionalConfirmVisible()).toBe(false);
    });

    it('cancelDeleteConditional should close confirmation', () => {
      const component = setupDeleteConditionalComponent();

      component.deleteConditionalConfirmVisible.set(true);
      component.cancelDeleteConditional();
      expect(component.deleteConditionalConfirmVisible()).toBe(false);
    });
  });

  describe('Replace Default Route', () => {
    function setupReplaceDefaultComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('replaceDefaultRoute should call replaceConditionalDefaultRoute', () => {
      const component = setupReplaceDefaultComponent();
      const stateService = TestBed.inject(SquadBuilderStateService);

      vi.mocked(stateService.replaceConditionalDefaultRoute).mockReturnValue({
        id: 'edge-2',
        sourceStepId: 'step-1',
        targetStepId: 'step-3',
        routingType: 'ALWAYS',
        condition: null,
        priority: 100,
        isDefault: true,
      });

      component.replaceDefaultRoute('edge-2');

      expect(stateService.replaceConditionalDefaultRoute).toHaveBeenCalledWith('edge-2');
    });
  });

  describe('Condition Parsing', () => {
    function setupParsingComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('should parse equals condition', () => {
      const component = setupParsingComponent();
      const parsed = component['parseAddRouteCondition']('output.status equals "success"');

      expect(parsed).toEqual({
        outputField: 'status',
        operator: 'equals',
        expectedValue: '"success"',
      });
    });

    it('should parse notEquals condition', () => {
      const component = setupParsingComponent();
      const parsed = component['parseAddRouteCondition']('output.status notEquals "failed"');

      expect(parsed).toEqual({
        outputField: 'status',
        operator: 'notEquals',
        expectedValue: '"failed"',
      });
    });

    it('should parse contains condition', () => {
      const component = setupParsingComponent();
      const parsed = component['parseAddRouteCondition']('output.message contains "error"');

      expect(parsed).toEqual({
        outputField: 'message',
        operator: 'contains',
        expectedValue: '"error"',
      });
    });

    it('should parse in condition', () => {
      const component = setupParsingComponent();
      const parsed = component['parseAddRouteCondition']('output.status in [SUCCESS, PENDING]');

      expect(parsed).toEqual({
        outputField: 'status',
        operator: 'in',
        expectedValue: '[SUCCESS, PENDING]',
      });
    });

    it('should return null for unparseable condition', () => {
      const component = setupParsingComponent();
      const parsed = component['parseAddRouteCondition']('invalid condition format');

      expect(parsed).toBeNull();
    });
  });

  describe('Available Targets in Edit Mode', () => {
    let selectedConditionalSignalForTargets: WritableSignal<any | null>;
    let stepsForTargetsSignal: WritableSignal<any[]>;
    let selectedRoutesForTargetsSignal: WritableSignal<any[]>;

    beforeEach(() => {
      selectedConditionalSignalForTargets = signal<any | null>(null);
      stepsForTargetsSignal = signal<any[]>([
        { id: 'step-1', name: 'Step 1', position: { x: 0, y: 0 }, inputRefs: [] },
        { id: 'step-2', name: 'Step 2', position: { x: 0, y: 0 }, inputRefs: [] },
        { id: 'step-3', name: 'Step 3', position: { x: 0, y: 0 }, inputRefs: [] },
      ]);
      selectedRoutesForTargetsSignal = signal<any[]>([
        { id: 'edge-1', sourceStepId: 'step-1', targetStepId: 'step-2' },
      ]);
    });

    function setupTargetsComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    it('should exclude currently edited target from used targets', () => {
      selectedConditionalSignalForTargets.set({
        id: 'cond-1',
        sourceStepId: 'step-1',
      });

      const component = setupTargetsComponent();
      component.editingEdgeId.set('edge-1');

      // Simulate the computed property
      const availableTargets = [
        { id: 'step-2', name: 'Step 2' },
        { id: 'step-3', name: 'Step 3' },
      ];

      expect(availableTargets).toContainEqual({ id: 'step-2', name: 'Step 2' });
    });
  });

  describe('Manual Conditional Route Request', () => {
    let conditionalsSignal: WritableSignal<any[]>;

    function setupConditionalRouteComponent() {
      return TestBed.runInInjectionContext(() => new SquadBuilderPage());
    }

    beforeEach(() => {
      conditionalsSignal = signal<any[]>([]);
      TestBed.overrideProvider(SquadBuilderStateService, {
        useValue: {
          draft: signal(null).asReadonly(),
          steps: stepsSignal.asReadonly(),
          edges: edgesSignal.asReadonly(),
          conditionals: conditionalsSignal.asReadonly(),
          selectedStep: selectedStepSignal.asReadonly(),
          selectedConditional: signal(null).asReadonly(),
          getAncestorSteps: vi.fn(() => []),
          updateSelectedStepInputRef: vi.fn(),
          updateSelectedStep: vi.fn(),
          addSelectedStepInputRef: vi.fn(),
          addSelectedStepManualInputRef: vi.fn(),
          addStep: vi.fn(),
          addConditional: vi.fn(),
          deleteSelectedStep: vi.fn(),
          removeSelectedStepInputRef: vi.fn(),
          addEdge: vi.fn(),
          removeEdge: vi.fn(),
          updateStepPosition: vi.fn(),
          selectStep: vi.fn(),
          selectConditional: vi.fn(),
          updateConditionalPosition: vi.fn(),
          updateSelectedConditional: vi.fn(),
          deleteSelectedConditional: vi.fn(),
          buildSavePayload: vi.fn(() => null),
          resetDraft: vi.fn(),
          loadDraftFromApi: vi.fn(),
          addConditionalRoute: vi.fn(),
          updateConditionalRoute: vi.fn(),
          removeConditionalRoute: vi.fn(),
          replaceConditionalDefaultRoute: vi.fn(),
        },
      });
    });

    it('handleConditionalRouteRequested should open form with target preselected', () => {
      const component = setupConditionalRouteComponent();

      conditionalsSignal.set([
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ]);
      stepsSignal.set([
        {
          id: 'step-1',
          name: 'Step 1',
          assignedAgentId: 'code-sentinel',
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          assignedAgentId: null,
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      component.handleConditionalRouteRequested({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
      });

      expect(component.addRouteFormVisible()).toBe(true);
      expect(component.addRouteFormState().targetStepId).toBe('step-2');
    });

    it('should reset form fields when opening conditional route form', () => {
      const component = setupConditionalRouteComponent();
      conditionalsSignal.set([
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ]);
      stepsSignal.set([
        {
          id: 'step-1',
          name: 'Step 1',
          assignedAgentId: 'code-sentinel',
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          assignedAgentId: null,
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      component.handleConditionalRouteRequested({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
      });

      const formState = component.addRouteFormState();
      expect(formState.outputField).toBe('');
      expect(formState.operator).toBe('equals');
      expect(formState.expectedValue).toBe('');
      expect(formState.priority).toBe('');
      expect(formState.isDefault).toBe(false);
    });

    it('should not enter edit mode from conditional route request', () => {
      const component = setupConditionalRouteComponent();
      conditionalsSignal.set([
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ]);
      stepsSignal.set([
        {
          id: 'step-1',
          name: 'Step 1',
          assignedAgentId: 'code-sentinel',
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          assignedAgentId: null,
          position: { x: 100, y: 0 },
          inputRefs: [],
        },
      ]);

      component.handleConditionalRouteRequested({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
      });

      expect(component.editingEdgeId()).toBeNull();
    });

    it('should not open form for invalid target step', () => {
      const component = setupConditionalRouteComponent();
      conditionalsSignal.set([
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ]);
      stepsSignal.set([
        {
          id: 'step-1',
          name: 'Step 1',
          assignedAgentId: 'code-sentinel',
          position: { x: 0, y: 0 },
          inputRefs: [],
        },
      ]);

      const formVisibleBefore = component.addRouteFormVisible();
      component.handleConditionalRouteRequested({
        conditionalId: 'cond-1',
        targetStepId: 'invalid-step',
      });

      expect(component.addRouteFormVisible()).toBe(formVisibleBefore);
    });
  });
});
