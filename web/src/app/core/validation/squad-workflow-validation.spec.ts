import { validateSquadWorkflow, type SquadWorkflowAgent } from './squad-workflow-validation';
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

  it('validates conditional branching and convergence', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver', ref('step-1', 'message', 'requirements')),
      step('step-3', 'Step 3', 'flow-architect', ref('step-1', 'message', 'requirement')),
      step(
        'step-4',
        'Step 4',
        'code-sentinel',
        ref('step-2', 'message', 'code'),
        ref('step-3', 'message', 'context'),
      ),
      whenEdge('step-1', 'step-2', 'output.message equals BUG_FIX', 1),
      defaultEdge('step-1', 'step-3'),
      edge('step-2', 'step-4'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('rejects a WHEN edge without a condition', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      whenEdge('step-1', 'step-2', '   ', 1),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from step 'step-1' to step 'step-2' uses routing type WHEN but has no condition.",
    );
  });

  it('rejects a WHEN edge with invalid condition syntax', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      whenEdge('step-1', 'step-2', 'output.changeType startsWith BUG', 1),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from step 'step-1' to step 'step-2' has an invalid routing condition: Invalid routing condition rule: 'output.changeType startsWith BUG'.",
    );
  });

  it('rejects an ALWAYS edge with a condition', () => {
    const invalidAlwaysEdge: SquadBuilderEdge = {
      ...edge('step-1', 'step-2'),
      condition: 'output.changeType equals BUG_FIX',
    };

    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      invalidAlwaysEdge,
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from step 'step-1' to step 'step-2' uses routing type ALWAYS and must not define a condition.",
    );
  });

  it('rejects an edge with a negative priority', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      whenEdge('step-1', 'step-2', 'output.changeType equals BUG_FIX', -1),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from step 'step-1' to step 'step-2' must have a nonnegative priority.",
    );
  });

  it('rejects a default edge using routing type WHEN', () => {
    const invalidDefaultEdge: SquadBuilderEdge = {
      ...whenEdge('step-1', 'step-2', 'output.changeType equals BUG_FIX', 1),
      isDefault: true,
    };

    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      invalidDefaultEdge,
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Connection from step 'step-1' to step 'step-2' is a default edge and must use routing type ALWAYS.",
    );
  });

  it('rejects more than one default outgoing edge from the same source step', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      defaultEdge('step-1', 'step-2'),
      defaultEdge('step-1', 'step-3'),
      edge('step-2', 'step-4'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Source step 'step-1' has more than one default outgoing edge.",
    );
  });

  it('rejects a non-default ALWAYS edge together with other outgoing edges', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      edge('step-1', 'step-2'),
      defaultEdge('step-1', 'step-3'),
      edge('step-2', 'step-4'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Source step 'step-1' has a non-default ALWAYS edge together with other outgoing edges.",
    );
  });

  it('rejects duplicate WHEN priorities under the same source step', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      whenEdge('step-1', 'step-2', 'output.changeType equals BUG_FIX', 1),
      whenEdge('step-1', 'step-3', 'output.changeType equals ENHANCEMENT', 1),
      edge('step-2', 'step-4'),
      edge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Source step 'step-1' has more than one WHEN edge with priority 1.",
    );
  });

  it('allows the same WHEN priority under different source steps', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      step('step-5', 'Step 5', 'test-weaver'),
      step('step-6', 'Step 6', 'flow-architect'),
      whenEdge('step-1', 'step-2', 'output.message equals BUG_FIX', 1),
      defaultEdge('step-1', 'step-3'),
      whenEdge('step-2', 'step-4', 'output.message equals UNIT', 1),
      defaultEdge('step-2', 'step-5'),
      edge('step-3', 'step-4'),
      edge('step-4', 'step-6'),
      edge('step-5', 'step-6'),
    );

    expect(validateSquadWorkflow(draft, agents)).toEqual([]);
  });

  it('allows multiple WHEN routes with unique priorities and one default route', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      step('step-5', 'Step 5', 'test-weaver'),
      whenEdge('step-1', 'step-2', 'output.changeType equals BUG_FIX', 1),
      whenEdge('step-1', 'step-3', 'output.changeType equals ENHANCEMENT', 2),
      defaultEdge('step-1', 'step-4'),
      edge('step-2', 'step-5'),
      edge('step-3', 'step-5'),
      edge('step-4', 'step-5'),
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

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 3' is disconnected from the workflow.",
    );
  });

  it('rejects directed cycles', () => {
    const draft = workflow(
      step('step-1', 'Step 1', 'code-sentinel'),
      step('step-2', 'Step 2', 'test-weaver'),
      step('step-3', 'Step 3', 'flow-architect'),
      step('step-4', 'Step 4', 'code-sentinel'),
      edge('step-1', 'step-2'),
      edge('step-2', 'step-3'),
      whenEdge('step-3', 'step-2', 'output.message equals RETRY', 1),
      defaultEdge('step-3', 'step-4'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      'The workflow contains a directed cycle.',
    );
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

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 2' has an incomplete inputRef.",
    );
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

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 2' has an incomplete inputRef.",
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
      step('step-1', 'Step 1', 'code-sentinel', {
        sourceType: 'MANUAL',
        targetInput: '',
      } as SquadBuilderInputRef),
      step('step-2', 'Step 2', 'test-weaver'),
      edge('step-1', 'step-2'),
    );

    expect(validateSquadWorkflow(draft, agents)).toContain(
      "Step 'Step 1' has an incomplete inputRef.",
    );
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
    conditionals: [],
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
    routingType: 'ALWAYS',
    condition: null,
    priority: 100,
    isDefault: false,
  };
}

function whenEdge(
  sourceStepId: string,
  targetStepId: string,
  condition: string,
  priority: number,
): SquadBuilderEdge {
  return {
    id: `${sourceStepId}-${targetStepId}`,
    sourceStepId,
    targetStepId,
    routingType: 'WHEN',
    condition,
    priority,
    isDefault: false,
  };
}

function defaultEdge(sourceStepId: string, targetStepId: string): SquadBuilderEdge {
  return {
    id: `${sourceStepId}-${targetStepId}`,
    sourceStepId,
    targetStepId,
    routingType: 'ALWAYS',
    condition: null,
    priority: 100,
    isDefault: true,
  };
}

function ref(fromStepId: string, key: string, targetInput: string): SquadBuilderInputRef {
  return {
    sourceType: 'STEP_OUTPUT',
    fromStepId,
    key,
    targetInput,
  };
}

function manualRef(targetInput: string): SquadBuilderInputRef {
  return {
    sourceType: 'MANUAL',
    targetInput,
  };
}

function legacyRef(fromStepId: string, key: string): SquadBuilderInputRef {
  return {
    fromStepId,
    key,
  } as SquadBuilderInputRef;
}

function isStep(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderStep {
  return Object.prototype.hasOwnProperty.call(item, 'parameters');
}

function isEdge(item: SquadBuilderStep | SquadBuilderEdge): item is SquadBuilderEdge {
  return Object.prototype.hasOwnProperty.call(item, 'sourceStepId');
}
