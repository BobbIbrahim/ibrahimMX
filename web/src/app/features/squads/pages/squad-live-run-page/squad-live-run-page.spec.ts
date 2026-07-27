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
});
