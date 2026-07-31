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
    {
      agentKey: 'code-sentinel',
      name: 'Code Sentinel',
      inputs: ['code', 'requirements', 'context'],
      outputs: ['message', 'summary'],
    },
    {
      agentKey: 'test-weaver',
      name: 'Test Weaver',
      inputs: ['code', 'requirements', 'testContext'],
      outputs: ['message'],
    },
    {
      agentKey: 'flow-architect',
      name: 'Flow Architect',
      inputs: ['requirement', 'context', 'constraints'],
      outputs: ['message'],
    },
  ];

  it('validates a linear DAG with a target input', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', 'requirements')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('allows empty inputRefs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('validates branching and convergence', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', 'requirements')),
      step('step-3', 'Step 3', 'flow-architect', ref('step-1', 'message', 'requirement')),
      step('step-4', 'Step 4', 'code-sentinel', ref('step-2', 'message', 'code'), ref('step-3', 'message', 'context')),
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
      step('step-2', 'Step 2', 'test-weaver', ref('step-3', 'message', 'requirements')),
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
      step(
        'step-2',
        'Step 2',
        'test-weaver',
        ref('step-1', 'message', 'requirements'),
        ref('step-1', 'message', 'testContext'),
      ),
      edge('step-1', 'step-2'),
    );
    const keyDraft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'missing', 'requirements')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(duplicateDraft, agents)).toContain(
      "Step 'Step 2' has a duplicate inputRef from Step 'Step 1' using output key 'message'.",
    );
    expect(validateSquadWorkflow(keyDraft, agents)).toContain(
      "Step 'Step 2' inputRef from Step 'Step 1' references undeclared output key 'missing'.",
    );
  });

  it('rejects blank target inputs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', ' ')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain("Step 'Step 2' has an incomplete inputRef.");
  });

  it('rejects unknown target inputs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', 'unknown')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 2' inputRef target input 'unknown' is not declared by agent 'Test Weaver'.",
    );
  });

  it('rejects duplicate target inputs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step(
        'step-2',
        'Step 2',
        'test-weaver',
        ref('step-1', 'message', 'requirements'),
        ref('step-1', 'summary', 'requirements'),
      ),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 2' has a duplicate inputRef target input 'requirements'.",
    );
  });

  it('rejects legacy mappings without targetInput', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', legacyRef('step-1', 'message')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain("Step 'Step 2' has an incomplete inputRef.");
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

  it('validates a root step with MANUAL input ref', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel', manualRef('code')),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', 'requirements')),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('rejects MANUAL inputRef with missing targetInput', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel', { sourceType: 'MANUAL', targetInput: '' } as SquadBuilderInputRef),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain("Step 'Step 1' has an incomplete inputRef.");
  });

  it('rejects MANUAL inputRef with undeclared target input', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel', manualRef('unknown')),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 1' inputRef target input 'unknown' is not declared by agent 'Code Sentinel'.",
    );
  });

  it('rejects duplicate MANUAL target inputs', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel', manualRef('code'), manualRef('code')),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 1' has a duplicate inputRef target input 'code'.",
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

function ref(fromStepId: string, key: string, targetInput: string): SquadBuilderInputRef {
  return { sourceType: 'STEP_OUTPUT', fromStepId, key, targetInput };
}

function manualRef(targetInput: string): SquadBuilderInputRef {
  return { sourceType: 'MANUAL', targetInput };
}

function legacyRef(fromStepId: string, key: string): SquadBuilderInputRef {
  return { fromStepId, key } as SquadBuilderInputRef;
}

function isStep(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderStep {
  return Object.prototype.hasOwnProperty.call(item, 'parameters');
}

function isEdge(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderEdge {
  return Object.prototype.hasOwnProperty.call(item, 'sourceStepId');
}
