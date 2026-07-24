import {
  validateSquadWorkflow,
  type SquadWorkflowAgent,
} from './squad-workflow-validation';
import {
  SquadBuilderDraft,
  SquadBuilderEdge,
  SquadBuilderInputRef,
  SquadBuilderStep,
} from '../models/squad-builder.model';
import { describe, expect, it } from 'vitest';

describe('validateSquadWorkflow', () => {
  const agents: SquadWorkflowAgent[] = [
    { agentKey: 'code-sentinel', name: 'Code Sentinel', outputs: ['message', 'summary'] },
    { agentKey: 'test-weaver', name: 'Test Weaver', outputs: ['message'] },
    { agentKey: 'flow-architect', name: 'Flow Architect', outputs: ['message'] },
  ];

  it('validates a linear DAG with an upstream inputRef', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('validates branching and convergence', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message')),
      step('step-3', 'Step 3', 'flow-architect', ref('step-1', 'message')),
      step('step-4', 'Step 4', 'code-sentinel', ref('step-2', 'message'), ref('step-3', 'message')),
      edge('step-1', 'step-2'),
      edge('step-1', 'step-3'),
      edge('step-2', 'step-4'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('rejects a disconnected workflow component', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      edge('step-1', 'step-2'),
      edge('step-3', 'step-4'),
      edge('step-4', 'step-3'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain("Step 'Step 3' is disconnected from the workflow.");
  });

  it('rejects directed cycles', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      edge('step-1', 'step-2'),
      edge('step-2', 'step-3'),
      edge('step-3', 'step-2'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain('The workflow contains a directed cycle.');
  });

  it('rejects invalid downstream inputRefs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-3', 'message')),
      step('step-3', 'Step 3', 'flow-architect'),
      edge('step-1', 'step-2'),
      edge('step-2', 'step-3'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 2' inputRef from Step 'Step 3' must reference an upstream ancestor.",
    );
  });

  it('rejects duplicate inputRefs and undeclared output keys', () => {
    const duplicateDraft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message'), ref('step-1', 'message')),
      edge('step-1', 'step-2'),
    );
    const keyDraft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'missing')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(duplicateDraft, agents)).toContain(
      "Step 'Step 2' has a duplicate inputRef from Step 'Step 1' using output key 'message'.",
    );
    expect(validateSquadWorkflow(keyDraft, agents)).toContain(
      "Step 'Step 2' inputRef from Step 'Step 1' references undeclared output key 'missing'.",
    );
  });

  it('rejects duplicate edges', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from Step 'Step 1' to Step 'Step 2' is duplicated.",
    );
  });
});

function workflow(...items: Array<SquadBuilderStep | SquadBuilderEdge>): SquadBuilderDraft {
  const steps = items.filter(isStep);
  const edges = items.filter(isEdge);

  return {
    id: 'draft-1',
    name: 'Workflow',
    description: 'Workflow validation test',
    type: 'hardcoded-flow',
    steps,
    edges,
  };
}

function step(
  id: string,
  name: string,
  assignedAgentId: string | null,
  ...inputRefs: SquadBuilderInputRef[]
): SquadBuilderStep {
  return {
    id,
    name,
    assignedAgentId,
    parameters: {},
    position: { x: 0, y: 0 },
    inputRefs,
  };
}

function edge(sourceStepId: string, targetStepId: string): SquadBuilderEdge {
  return {
    id: `${sourceStepId}-${targetStepId}`,
    sourceStepId,
    targetStepId,
  };
}

function ref(fromStepId: string, key: string): SquadBuilderInputRef {
  return { fromStepId, key };
}

function isStep(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderStep {
  return Object.prototype.hasOwnProperty.call(item, 'parameters');
}

function isEdge(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderEdge {
  return Object.prototype.hasOwnProperty.call(item, 'sourceStepId');
}
