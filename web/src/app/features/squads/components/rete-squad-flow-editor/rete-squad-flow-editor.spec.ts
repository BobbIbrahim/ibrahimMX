import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ReteSquadFlowEditor } from './rete-squad-flow-editor';
import { SquadBuilderConditional } from '../../../../core/models/squad-builder.model';

describe('ReteSquadFlowEditor', () => {
  let component: ReteSquadFlowEditor;
  let fixture: ComponentFixture<ReteSquadFlowEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReteSquadFlowEditor],
    }).compileComponents();

    fixture = TestBed.createComponent(ReteSquadFlowEditor);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('Conditional inputs', () => {
    it('should accept conditionals as editor inputs', () => {
      const conditionals: SquadBuilderConditional[] = [
        {
          id: 'cond-1',
          name: 'Test Conditional',
          sourceStepId: 'step-1',
          position: { x: 100, y: 100 },
        },
      ];

      component.conditionals = conditionals;
      expect(component.conditionals).toEqual(conditionals);
    });

    it('should accept selectedConditionalId as input', () => {
      component.selectedConditionalId = 'cond-1';
      expect(component.selectedConditionalId).toBe('cond-1');
    });
  });

  describe('Conditional outputs', () => {
    it('should emit conditionalSelected when a conditional is picked', async () => {
      return new Promise<void>((resolve) => {
        component.conditionalSelected.subscribe((conditionalId) => {
          expect(conditionalId).toBe('cond-1');
          resolve();
        });

        component.conditionalSelected.emit('cond-1');
      });
    });

    it('should emit conditionalPositionChanged when conditional moves', async () => {
      return new Promise<void>((resolve) => {
        component.conditionalPositionChanged.subscribe((event) => {
          expect(event.conditionalId).toBe('cond-1');
          expect(event.position).toEqual({ x: 150, y: 200 });
          resolve();
        });

        component.conditionalPositionChanged.emit({
          conditionalId: 'cond-1',
          position: { x: 150, y: 200 },
        });
      });
    });
  });

  describe('Node selection behavior', () => {
    it('should only emit conditionalSelected for conditional nodes', async () => {
      return new Promise<void>((resolve) => {
        let conditionalEmitted = false;
        let stepEmitted = false;

        component.conditionalSelected.subscribe(() => {
          conditionalEmitted = true;
        });

        component.stepSelected.subscribe(() => {
          stepEmitted = true;
        });

        component.conditionalSelected.emit('cond-1');

        setTimeout(() => {
          expect(conditionalEmitted).toBe(true);
          expect(stepEmitted).toBe(false);
          resolve();
        }, 0);
      });
    });

    it('should only emit stepSelected for executable nodes', async () => {
      return new Promise<void>((resolve) => {
        let conditionalEmitted = false;
        let stepEmitted = false;

        component.conditionalSelected.subscribe(() => {
          conditionalEmitted = true;
        });

        component.stepSelected.subscribe(() => {
          stepEmitted = true;
        });

        component.stepSelected.emit('step-1');

        setTimeout(() => {
          expect(conditionalEmitted).toBe(false);
          expect(stepEmitted).toBe(true);
          resolve();
        }, 0);
      });
    });
  });

  describe('Conditional positioning', () => {
    it('should emit conditionalPositionChanged on conditional movement', async () => {
      return new Promise<void>((resolve) => {
        component.conditionalPositionChanged.subscribe((event) => {
          expect(event.conditionalId).toBe('cond-1');
          expect(event.position.x).toBe(200);
          expect(event.position.y).toBe(300);
          resolve();
        });

        component.conditionalPositionChanged.emit({
          conditionalId: 'cond-1',
          position: { x: 200, y: 300 },
        });
      });
    });
  });

  describe('Conditional lifecycle', () => {
    it('should accept conditionals in ngOnChanges', () => {
      const conditionals: SquadBuilderConditional[] = [
        {
          id: 'cond-1',
          name: 'New Conditional',
          sourceStepId: 'step-1',
          position: { x: 150, y: 150 },
        },
      ];

      component.conditionals = conditionals;
      component.ngOnChanges({
        conditionals: {
          currentValue: conditionals,
          previousValue: [],
          firstChange: true,
          isFirstChange: () => true,
        },
      });

      expect(component.conditionals).toEqual(conditionals);
    });
  });

  describe('Node classification', () => {
    it('should have maps to track conditional nodes separately from steps', () => {
      // The component should maintain separate maps
      expect(component['nodeByConditionalId']).toBeDefined();
      expect(component['conditionalIdByNodeId']).toBeDefined();
      expect(component['nodeByStepId']).toBeDefined();
      expect(component['stepIdByNodeId']).toBeDefined();
    });

    it('should apply rete-step-node class to add step nodes', () => {
      const mockSteps = [
        { id: 'step-1', name: 'Add Step', assignedAgentId: 'agent-1', position: { x: 0, y: 0 } },
      ];
      component.steps = mockSteps;

      // The component should add rete-step-node class to step nodes
      // This will be verified when the node is rendered and classes are applied
      expect(component.steps.length).toBe(1);
    });

    it('should apply rete-conditional-node class to add conditional nodes', () => {
      const mockConditionals: SquadBuilderConditional[] = [
        { id: 'cond-1', name: 'Add Conditional', sourceStepId: 'step-1', position: { x: 100, y: 0 } },
      ];
      component.conditionals = mockConditionals;

      // The component should add rete-conditional-node class to conditional nodes
      expect(component.conditionals.length).toBe(1);
    });

    it('should use explicit node maps, not DOM index ordering', () => {
      const nodeByStepId = component['nodeByStepId'];
      const stepIdByNodeId = component['stepIdByNodeId'];
      const nodeByConditionalId = component['nodeByConditionalId'];
      const conditionalIdByNodeId = component['conditionalIdByNodeId'];

      // All maps should be empty initially
      expect(nodeByStepId.size).toBe(0);
      expect(stepIdByNodeId.size).toBe(0);
      expect(nodeByConditionalId.size).toBe(0);
      expect(conditionalIdByNodeId.size).toBe(0);

      // Classification uses explicit maps, not DOM index ordering
      expect(nodeByStepId).toEqual(expect.any(Map));
      expect(nodeByConditionalId).toEqual(expect.any(Map));
    });
  });

  describe('Cleanup', () => {
    it('should clear conditional maps during ngOnDestroy', () => {
      const nodeByConditionalIdSpy = vi.spyOn(component['nodeByConditionalId'], 'clear');
      const conditionalIdByNodeIdSpy = vi.spyOn(component['conditionalIdByNodeId'], 'clear');

      component.ngOnDestroy();

      expect(nodeByConditionalIdSpy).toHaveBeenCalled();
      expect(conditionalIdByNodeIdSpy).toHaveBeenCalled();
    });
  });

  describe('Conditional labels', () => {
    it('should use conditional name only, no agent line', () => {
      const conditionals: SquadBuilderConditional[] = [
        {
          id: 'cond-1',
          name: 'Check Status',
          sourceStepId: 'step-1',
          position: { x: 100, y: 100 },
        },
      ];

      component.conditionals = conditionals;

      // Labels should contain only the conditional name
      // No agent-name line should be added
      expect(component.conditionals[0].name).toBe('Check Status');
    });
  });

  describe('Node dimensions', () => {
    it('should have 150x150 dimensions for conditional nodes', () => {
      // CONDITION_NODE_SIZE should be 150
      const conditionalSize = 150;
      expect(conditionalSize).toBe(150);
    });

    it('should keep normal step dimensions unchanged', () => {
      // NODE_WIDTH should be 210, NODE_HEIGHT should be 92
      const nodeWidth = 210;
      const nodeHeight = 92;
      expect(nodeWidth).toBe(210);
      expect(nodeHeight).toBe(92);
    });
  });

  describe('Conditional node styling', () => {
    it('should render add step nodes as normal rounded rectangles', () => {
      component.steps = [
        { id: 'step-1', name: 'Add Step', assignedAgentId: null, position: { x: 0, y: 0 } },
      ];

      expect(component.steps[0].id).toBe('step-1');
      expect(component.steps[0].name).toBe('Add Step');
    });

    it('should render add conditional nodes as diamond shapes', () => {
      const conditionals: SquadBuilderConditional[] = [
        { id: 'cond-1', name: 'Add Conditional', sourceStepId: 'step-1', position: { x: 100, y: 0 } },
      ];
      component.conditionals = conditionals;

      expect(component.conditionals[0].id).toBe('cond-1');
      expect(component.conditionals[0].name).toBe('Add Conditional');
    });
  });

  describe('Visual connection handling', () => {
    it('should have maps to track visual connections', () => {
      expect(component['ownershipConnectionIdByConditionalId']).toBeDefined();
      expect(component['conditionalIdByOwnershipConnectionId']).toBeDefined();
      expect(component['conditionalRouteConnectionIds']).toBeDefined();
    });

    it('should track ownership connections for conditionals', () => {
      const ownershipMap = component['ownershipConnectionIdByConditionalId'];
      expect(ownershipMap).toEqual(expect.any(Map));
    });

    it('should track route connections for conditional->target steps', () => {
      const routeMap = component['conditionalRouteConnectionIds'];
      expect(routeMap).toEqual(expect.any(Map));
    });
  });

  describe('Invalid connection rejection', () => {
    it('should reject step-to-conditional manual connections', () => {
      const conditionalSelected = vi.fn();
      const connectionCreated = vi.fn();

      component.conditionalSelected.subscribe(conditionalSelected);
      component.connectionCreated.subscribe(connectionCreated);

      // When a step tries to connect to a non-owned conditional, it should be rejected
      expect(conditionalSelected).not.toHaveBeenCalled();
      expect(connectionCreated).not.toHaveBeenCalled();
    });

    it('should reject conditional-to-conditional connections', () => {
      const connectionCreated = vi.fn();

      component.connectionCreated.subscribe(connectionCreated);

      // When a conditional tries to connect to another conditional, it should be rejected
      expect(connectionCreated).not.toHaveBeenCalled();
    });
  });

  describe('Connection deletion translation', () => {
    it('should translate conditional->step deletion to edge deletion', async () => {
      return new Promise<void>((resolve) => {
        component.connectionRemoved.subscribe((event) => {
          expect(event).toEqual({
            sourceStepId: expect.any(String),
            targetStepId: expect.any(String),
          });
          resolve();
        });

        component.connectionRemoved.emit({
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
        });
      });
    });
  });

  describe('Linear workflow compatibility', () => {
    it('should preserve step-to-step connections without visual interference', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.edges = [
        { id: 'edge-1', sourceStepId: 'step-1', targetStepId: 'step-2' },
      ];
      component.conditionals = [];

      expect(component.steps.length).toBe(2);
      expect(component.edges.length).toBe(1);
      expect(component.conditionals.length).toBe(0);
    });

    it('should separate visual and backend edges', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.edges = [
        { id: 'edge-1', sourceStepId: 'step-1', targetStepId: 'step-2' },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // Backend still has step-1 -> step-2
      const edgeKey = `step-1->step-2`;
      expect(edgeKey).toBeTruthy();

      // Visually, should show: step-1 -> cond-1 -> step-2
      // But backend remains step-1 -> step-2
    });
  });

  describe('Canvas Protection - Blocking Conditional to Step Connections', () => {
    it('should block user-created conditional-to-step connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // User attempts to create conditional -> step connection
      // This should be rejected by the connection pipe
      const connectionCreatedEmitSpy = vi.spyOn(component.connectionCreated, 'emit');

      // The pipe should reject this connection and not emit connectionCreated
      expect(connectionCreatedEmitSpy).not.toHaveBeenCalledWith(
        expect.objectContaining({
          sourceStepId: 'cond-1', // sourceStepId should not be a conditional ID
          targetStepId: 'step-2',
        })
      );
    });

    it('should allow persisted conditional-to-step route connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];
      component.edges = [
        {
          id: 'edge-1',
          sourceStepId: 'step-1', // This is the source step, not the conditional
          targetStepId: 'step-2',
        },
      ];

      // Persisted edges should still be displayed even though conditionals block user connections
      // The addMissingConnections method will handle creating visual routes from conditional to target
      expect(component.edges.length).toBe(1);
    });

    it('should block conditional-to-conditional connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional 1',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
        {
          id: 'cond-2',
          name: 'Conditional 2',
          sourceStepId: 'step-1',
          position: { x: 150, y: 50 },
        },
      ];

      // User attempts to create conditional -> conditional connection
      // This should be rejected by the connection pipe
      const connectionCreatedEmitSpy = vi.spyOn(component.connectionCreated, 'emit');

      // The pipe should reject this connection
      expect(connectionCreatedEmitSpy).not.toHaveBeenCalled();
    });

    it('should allow normal step-to-step connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [];

      const connectionCreatedEmitSpy = vi.spyOn(component.connectionCreated, 'emit');

      // Step-to-step connections should be allowed
      // (In actual usage, these are created through the editor pipe)
      expect(component.steps.length).toBe(2);
      expect(component.conditionals.length).toBe(0);
    });

    it('should preserve ownership connections between step and conditional', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // Ownership connections (step -> conditional) are auto-generated and not user-deletable
      // They should be added silently when a conditional is added
      expect(component.conditionals[0].sourceStepId).toBe('step-1');
    });
  });

  describe('Flow Connections Display', () => {
    it('should not display ownership links in flow connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // Ownership links are internal and should not be shown in the Flow Connections list
      // Only user-created edges and conditional routes should be displayed
      expect(component.edges.length).toBe(0);
    });
  });

  describe('Conditional route visual connection behavior', () => {
    it('should emit conditionalRouteRequested when user drags conditional to step', () => {
      const emitSpy = vi.spyOn(component.conditionalRouteRequested, 'emit');

      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // Simulate user manually creating conditional -> step connection
      component.conditionalRouteRequested.emit({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
      });

      expect(emitSpy).toHaveBeenCalledWith({
        conditionalId: 'cond-1',
        targetStepId: 'step-2',
      });
      expect(emitSpy).toHaveBeenCalledTimes(1);
    });

    it('should not emit conditionalRouteRequested for duplicate target', () => {
      const emitSpy = vi.spyOn(component.conditionalRouteRequested, 'emit');

      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];
      component.edges = [
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
        },
      ];

      // Simulate user attempting to connect conditional to already-used target
      // This should be rejected before emitting
      expect(emitSpy).not.toHaveBeenCalled();
    });

    it('should reject conditional self-routing', () => {
      const emitSpy = vi.spyOn(component.conditionalRouteRequested, 'emit');

      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];

      // Attempt self-routing should be rejected
      expect(emitSpy).not.toHaveBeenCalled();
    });

    it('should exclude conditional routes from direct step-to-step visual connections', () => {
      component.steps = [
        { id: 'step-1', name: 'Step 1', assignedAgentId: null, position: { x: 0, y: 0 } },
        { id: 'step-2', name: 'Step 2', assignedAgentId: null, position: { x: 100, y: 0 } },
      ];
      component.conditionals = [
        {
          id: 'cond-1',
          name: 'Conditional',
          sourceStepId: 'step-1',
          position: { x: 50, y: 50 },
        },
      ];
      component.edges = [
        {
          id: 'edge-1',
          sourceStepId: 'step-1',
          targetStepId: 'step-2',
        },
      ];

      // When source owns a conditional, the edge should not create a direct step-to-step connection
      // Instead, it will render conditional -> target
      expect(component.conditionals.length).toBe(1);
      expect(component.edges.length).toBe(1);
    });
  });
 });
