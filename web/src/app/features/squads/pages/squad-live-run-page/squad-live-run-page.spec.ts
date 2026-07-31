import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';

import { AgentService } from '../../../../core/services/agent.service';
import { SquadService } from '../../../../core/services/squad.service';
import { SquadLiveRunPage } from './squad-live-run-page';

describe('SquadLiveRunPage', () => {
  it('copies targetInput into configured input refs', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'squad-1',
              },
            },
          },
        },
        {
          provide: Router,
          useValue: {
            navigate: () => Promise.resolve(true),
          },
        },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({
              afterClosed: () => of(false),
              close: () => {},
            }),
          },
        },
        {
          provide: AgentService,
          useValue: {
            getAgents: () =>
              signal([
                {
                  agentKey: 'test-weaver',
                  name: 'Test Weaver',
                },
              ]).asReadonly(),
          },
        },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({
                id: 'squad-1',
                name: 'Workflow',
                description: 'Workflow',
                type: 'hardcoded-flow',
                steps: [],
                edges: [],
              }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    component.squad.set({
      id: 'squad-1',
      name: 'Workflow',
      description: 'Workflow',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-2',
          name: 'Step 2',
          type: 'AI_AGENT',
          agentKey: 'test-weaver',
          inputRefs: [
            {
              sourceType: 'STEP_OUTPUT',
              targetInput: 'requirements',
              fromStepId: 'step-1',
              key: 'message',
            },
          ],
        },
      ],
      edges: [],
    });
    component.selectedStepId.set('step-2');
    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'RUNNING',
      steps: [
        {
          stepId: 'step-2',
          stepName: 'Step 2',
          status: 'RUNNING',
          input: { requirements: 'Value produced by Step 1' },
          output: null,
        },
      ],
    });

    expect(component.selectedStepDetails()?.configuredInputRefs).toEqual([
      {
        targetInput: 'requirements',
        fromStepId: 'step-1',
        key: 'message',
      },
    ]);
  });

  it('displays the final squad result equal to the terminal step output when completed', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'COMPLETED',
      steps: [
        { stepId: 'step-1', stepName: 'Step 1', status: 'COMPLETED', input: {}, output: { change: 'x' } },
        { stepId: 'step-2', stepName: 'Step 2', status: 'COMPLETED', input: {}, output: { message: 'planned' } },
      ],
      finalResult: { message: 'planned' },
    });

    expect(component.finalResult()).toEqual({ message: 'planned' });
    expect(component.finalResultEntries()).toEqual([['message', 'planned']]);
  });

  it('handles an absent final result without throwing', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'COMPLETED',
      steps: [],
    });

    expect(component.finalResult()).toBeNull();
    expect(component.finalResultEntries()).toEqual([]);
    expect(component.failureMessage()).toBeNull();
  });

  it('exposes the failure message from the failed step when the run failed', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'FAILED',
      steps: [
        {
          stepId: 'step-2',
          stepName: 'Step 2',
          status: 'FAILED',
          input: { requirements: 'value' },
          output: { error: 'Boom' },
          message: 'Step "Step 2" failed: Boom',
        },
      ],
    });

    expect(component.failureMessage()).toBe('Step "Step 2" failed: Boom');
    expect(component.finalResult()).toBeNull();
  });

  it('identifies root step with MANUAL input refs and builds dialog prompt', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        {
          provide: AgentService,
          useValue: {
            getAgents: () =>
              signal([
                {
                  agentKey: 'change-classifier',
                  name: 'Change Classifier',
                  inputs: ['change'],
                },
              ]).asReadonly(),
            getAgentByKey: () => ({ name: 'Change Classifier', inputs: ['change'] }),
          },
        },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // Set up a squad with root step having MANUAL input ref
    component.squad.set({
      id: 'squad-1',
      name: 'Workflow',
      description: 'Workflow',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Change Classifier',
          type: 'AI_AGENT',
          agentKey: 'change-classifier',
          inputRefs: [
            {
              targetInput: 'change',
              sourceType: 'MANUAL',
            },
          ],
        },
        {
          id: 'step-2',
          name: 'Step 2',
          type: 'AI_AGENT',
          agentKey: 'test-weaver',
          inputRefs: [],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
        },
      ],
    });

    const prompt = component['buildRootStepInputPrompt']();

    expect(prompt).not.toBeNull();
    expect(prompt?.stepName).toBe('Change Classifier');
    expect(prompt?.agentName).toBe('Change Classifier');
    expect(prompt?.inputKeys).toEqual(['change']);
  });

  it('returns null for root step input prompt when no MANUAL refs exist', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // Set up a squad without MANUAL input refs
    component.squad.set({
      id: 'squad-1',
      name: 'Workflow',
      description: 'Workflow',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-1',
          name: 'Step 1',
          type: 'AI_AGENT',
          agentKey: 'test-weaver',
          inputRefs: [],
        },
      ],
      edges: [],
    });

    const prompt = component['buildRootStepInputPrompt']();

    expect(prompt).toBeNull();
  });

  it('does not display STEP_OUTPUT refs in the run input dialog', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        {
          provide: AgentService,
          useValue: {
            getAgents: () =>
              signal([
                {
                  agentKey: 'test-agent',
                  name: 'Test Agent',
                  inputs: ['input1'],
                },
              ]).asReadonly(),
            getAgentByKey: () => ({ name: 'Test Agent', inputs: ['input1'] }),
          },
        },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // Set up a squad with STEP_OUTPUT ref (should not appear in dialog)
    component.squad.set({
      id: 'squad-1',
      name: 'Workflow',
      description: 'Workflow',
      type: 'hardcoded-flow',
      steps: [
        {
          id: 'step-0',
          name: 'Step 0',
          type: 'AI_AGENT',
          agentKey: 'test-agent',
          inputRefs: [],
        },
        {
          id: 'step-1',
          name: 'Step 1',
          type: 'AI_AGENT',
          agentKey: 'test-agent',
          inputRefs: [
            {
              targetInput: 'input1',
              sourceType: 'STEP_OUTPUT',
              fromStepId: 'step-0',
              key: 'output-key',
            },
          ],
        },
      ],
      edges: [
        {
          sourceStepId: 'step-0',
          targetStepId: 'step-1',
        },
      ],
    });

    // This root step has STEP_OUTPUT ref, not MANUAL, so prompt should be null
    const prompt = component['buildRootStepInputPrompt']();

    // The root step is step-0 which has no input refs, so prompt is null
    expect(prompt).toBeNull();
  });

  it('preserves MANUAL refs when squad is reloaded from API', () => {
    const mockSquadService = {
      getSquadByIdFromApi: () =>
        of({
          id: 'squad-1',
          name: 'Workflow',
          description: 'Workflow',
          type: 'hardcoded-flow',
          steps: [
            {
              id: 'step-1',
              name: 'Change Classifier',
              type: 'AI_AGENT',
              agentKey: 'change-classifier',
              inputRefs: [
                {
                  targetInput: 'change',
                  sourceType: 'MANUAL',
                },
              ],
            },
          ],
          edges: [],
        }),
    };

    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        {
          provide: AgentService,
          useValue: {
            getAgents: () =>
              signal([
                {
                  agentKey: 'change-classifier',
                  name: 'Change Classifier',
                  inputs: ['change'],
                },
              ]).asReadonly(),
            getAgentByKey: () => ({ name: 'Change Classifier', inputs: ['change'] }),
          },
        },
        {
          provide: SquadService,
          useValue: mockSquadService,
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // When squad is loaded from API
    component.ngOnInit();

    // Verify MANUAL ref is present in loaded squad
    const squad = component.squad();
    expect(squad).not.toBeNull();
    expect(squad?.steps[0]?.inputRefs).toBeDefined();
    expect(squad?.steps[0]?.inputRefs?.[0]?.targetInput).toBe('change');
    expect(squad?.steps[0]?.inputRefs?.[0]?.sourceType).toBe('MANUAL');
  });

  it('orders final result fields in the specified workflow order', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // Set completion status with all four known fields in random order in the data
    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'COMPLETED',
      steps: [],
      finalResult: {
        nextAction: 'Deploy to staging',
        change: 'Add retry logic',
        test: 'Verify payment gateway',
        changeType: 'ENHANCEMENT',
      },
    });

    const ordered = component.orderedFinalResultFields();
    const fieldKeys = ordered.map((entry) => entry[0]);

    // Should be in exact order: change, changeType, test, nextAction
    expect(fieldKeys).toEqual(['change', 'changeType', 'test', 'nextAction']);
  });

  it('appends unknown fields after known fields', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'COMPLETED',
      steps: [],
      finalResult: {
        nextAction: 'Deploy',
        change: 'Add logic',
        extraField: 'Unknown value',
        test: 'Verify test',
        changeType: 'ENHANCEMENT',
        anotherField: 'Another value',
      },
    });

    const ordered = component.orderedFinalResultFields();
    const fieldKeys = ordered.map((entry) => entry[0]);

    // Known fields first in order, then unknown fields
    expect(fieldKeys[0]).toBe('change');
    expect(fieldKeys[1]).toBe('changeType');
    expect(fieldKeys[2]).toBe('test');
    expect(fieldKeys[3]).toBe('nextAction');
    expect(fieldKeys.slice(4)).toContain('extraField');
    expect(fieldKeys.slice(4)).toContain('anotherField');
  });

  it('handles missing known fields without empty sections', () => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'squad-1' },
              queryParamMap: { get: () => null },
            },
          },
        },
        { provide: Router, useValue: { navigate: () => Promise.resolve(true) } },
        {
          provide: MatDialog,
          useValue: { open: () => ({ afterClosed: () => of(false), close: () => {} }) },
        },
        { provide: AgentService, useValue: { getAgents: () => signal([]).asReadonly() } },
        {
          provide: SquadService,
          useValue: {
            getSquadByIdFromApi: () =>
              of({ id: 'squad-1', name: 'Workflow', description: '', type: 'hardcoded-flow', steps: [], edges: [] }),
          },
        },
      ],
    });

    const component = TestBed.runInInjectionContext(() => new SquadLiveRunPage());

    // Only provide change and nextAction (missing changeType and test)
    component.executionStatus.set({
      squadId: 'squad-1',
      overallStatus: 'COMPLETED',
      steps: [],
      finalResult: {
        change: 'Add logic',
        nextAction: 'Deploy',
      },
    });

    const ordered = component.orderedFinalResultFields();
    const fieldKeys = ordered.map((entry) => entry[0]);

    // Should only have the provided fields, not all four
    expect(fieldKeys).toEqual(['change', 'nextAction']);
    expect(fieldKeys.length).toBe(2);
  });
});
