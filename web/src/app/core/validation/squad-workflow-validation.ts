import { SquadBuilderDraft, SquadBuilderStep } from '../models/squad-builder.model';

export type SquadWorkflowAgent = {
  agentKey: string;
  name: string;
  inputs: string[];
  outputs: string[];
};

export function validateSquadWorkflow(
  draft: SquadBuilderDraft | null,
  agents: SquadWorkflowAgent[],
): string[] {
  if (!draft) {
    return [];
  }

  const steps = draft.steps ?? [];
  const edges = draft.edges ?? [];

  const validationErrors: string[] = [];

  if (steps.length < 2) {
    validationErrors.push('A workflow must contain at least two steps.');
    return validationErrors;
  }

  const stepMap = new Map<string, SquadBuilderStep>();
  const agentByKey = new Map(agents.map((agent) => [agent.agentKey, agent]));
  const outgoingEdges = new Map<string, Set<string>>();
  const incomingEdges = new Map<string, Set<string>>();
  const undirectedEdges = new Map<string, Set<string>>();
  const reverseEdges = new Map<string, Set<string>>();
  const uniqueEdgeKeys = new Set<string>();

  for (const step of steps) {
    if (!step?.id) {
      validationErrors.push('A workflow step must have a nonblank id.');
      return validationErrors;
    }

    if (!step.name || !step.name.trim()) {
      validationErrors.push(stepLabel(step) + ' must have a nonblank name.');
    }

    const agentKey = step.assignedAgentId;
    if (!agentKey || !agentKey.trim()) {
      validationErrors.push(stepLabel(step) + ' must have an assigned agent.');
    } else if (!agentByKey.has(agentKey)) {
      validationErrors.push(stepLabel(step) + " references unknown agent '" + agentKey + "'.");
    }

    if (stepMap.has(step.id)) {
      validationErrors.push("The workflow contains duplicate step id '" + step.id + "'.");
    }

    stepMap.set(step.id, step);
  }

  for (const edge of edges) {
    if (!edge) {
      validationErrors.push('The workflow contains an invalid edge.');
      continue;
    }

    const { sourceStepId, targetStepId } = edge;

    if (!stepMap.has(sourceStepId)) {
      validationErrors.push(
        'Connection from ' + stepReferenceLabel(sourceStepId, stepMap) + ' references an unknown source step.',
      );
      continue;
    }

    if (!stepMap.has(targetStepId)) {
      validationErrors.push(
        'Connection to ' + stepReferenceLabel(targetStepId, stepMap) + ' references an unknown target step.',
      );
      continue;
    }

    if (sourceStepId === targetStepId) {
      validationErrors.push(
        'Connection from ' +
          stepReferenceLabel(sourceStepId, stepMap) +
          ' to ' +
          stepReferenceLabel(targetStepId, stepMap) +
          ' must connect two different steps.',
      );
      continue;
    }

    const edgeKey = sourceStepId + '\u0000' + targetStepId;
    if (uniqueEdgeKeys.has(edgeKey)) {
      validationErrors.push(
        "Connection from " +
          stepReferenceLabel(sourceStepId, stepMap) +
          ' to ' +
          stepReferenceLabel(targetStepId, stepMap) +
          ' is duplicated.',
      );
      continue;
    }
    uniqueEdgeKeys.add(edgeKey);

    addGraphEdge(outgoingEdges, sourceStepId, targetStepId);
    addGraphEdge(incomingEdges, targetStepId, sourceStepId);
    addGraphEdge(undirectedEdges, sourceStepId, targetStepId);
    addGraphEdge(undirectedEdges, targetStepId, sourceStepId);
    addGraphEdge(reverseEdges, targetStepId, sourceStepId);
  }

  const roots = steps.filter((step) => (incomingEdges.get(step.id)?.size ?? 0) === 0);
  const terminals = steps.filter((step) => (outgoingEdges.get(step.id)?.size ?? 0) === 0);

  if (roots.length !== 1) {
    validationErrors.push('The workflow must contain exactly one root step.');
  } else if ((outgoingEdges.get(roots[0].id)?.size ?? 0) === 0) {
    validationErrors.push('The workflow must contain exactly one root step.');
  }

  if (terminals.length !== 1) {
    validationErrors.push('The workflow must contain exactly one terminal step.');
  } else if ((incomingEdges.get(terminals[0].id)?.size ?? 0) === 0) {
    validationErrors.push('The workflow must contain exactly one terminal step.');
  }

  if (validationErrors.length > 0) {
    return validationErrors;
  }

  const visited = new Set<string>();
  const queue: string[] = [steps[0].id];
  visited.add(steps[0].id);

  while (queue.length > 0) {
    const currentStepId = queue.shift();
    if (!currentStepId) {
      continue;
    }

    for (const neighborStepId of undirectedEdges.get(currentStepId) ?? []) {
      if (!visited.has(neighborStepId)) {
        visited.add(neighborStepId);
        queue.push(neighborStepId);
      }
    }
  }

  for (const step of steps) {
    if (!visited.has(step.id)) {
      validationErrors.push(stepLabel(step) + ' is disconnected from the workflow.');
    }
  }

  if (validationErrors.length > 0) {
    return validationErrors;
  }

  const incomingCounts = new Map<string, number>();
  const readySteps: string[] = [];

  for (const step of steps) {
    const incomingCount = incomingEdges.get(step.id)?.size ?? 0;
    incomingCounts.set(step.id, incomingCount);
    if (incomingCount === 0) {
      readySteps.push(step.id);
    }
  }

  let visitedCount = 0;
  while (readySteps.length > 0) {
    const currentStepId = readySteps.shift();
    if (!currentStepId) {
      continue;
    }

    visitedCount++;

    for (const targetStepId of outgoingEdges.get(currentStepId) ?? []) {
      const nextCount = (incomingCounts.get(targetStepId) ?? 0) - 1;
      incomingCounts.set(targetStepId, nextCount);

      if (nextCount === 0) {
        readySteps.push(targetStepId);
      }
    }
  }

  if (visitedCount !== steps.length) {
    validationErrors.push('The workflow contains a directed cycle.');
    return validationErrors;
  }

  for (const step of steps) {
    const hasIncoming = (incomingEdges.get(step.id)?.size ?? 0) > 0;
    const hasOutgoing = (outgoingEdges.get(step.id)?.size ?? 0) > 0;

    if (!hasIncoming && !hasOutgoing) {
      validationErrors.push(stepLabel(step) + ' is disconnected from the workflow.');
    }
  }

  if (validationErrors.length > 0) {
    return validationErrors;
  }

  for (const step of steps) {
    const ancestorIds = computeAncestors(step.id, reverseEdges);
    const seenInputRefs = new Set<string>();
    const stepAgentKey = step.assignedAgentId;
    const targetInputs = new Set(
      (stepAgentKey ? agentByKey.get(stepAgentKey)?.inputs ?? [] : []).filter((input) => Boolean(input?.trim())),
    );
    const seenTargetInputs = new Set<string>();

    for (const inputRef of step.inputRefs ?? []) {
      // MANUAL input refs only need targetInput
      if (inputRef.sourceType === 'MANUAL') {
        if (!inputRef.targetInput || !inputRef.targetInput.trim()) {
          validationErrors.push(stepLabel(step) + ' has an incomplete inputRef.');
          continue;
        }

        if (!targetInputs.has(inputRef.targetInput)) {
          validationErrors.push(
            stepLabel(step) +
              " inputRef target input '" +
              inputRef.targetInput +
              "' is not declared by agent '" +
              (stepAgentKey ? agentByKey.get(stepAgentKey)?.name ?? stepAgentKey : 'unknown') +
              "'.",
          );
          continue;
        }

        if (seenTargetInputs.has(inputRef.targetInput)) {
          validationErrors.push(
            stepLabel(step) + " has a duplicate inputRef target input '" + inputRef.targetInput + "'.",
          );
          continue;
        }
        seenTargetInputs.add(inputRef.targetInput);

        continue;
      }

      // STEP_OUTPUT input refs need fromStepId, key, and targetInput
      if (
        !inputRef?.fromStepId ||
        !inputRef.key ||
        !inputRef.key.trim() ||
        !inputRef.targetInput ||
        !inputRef.targetInput.trim()
      ) {
        validationErrors.push(stepLabel(step) + ' has an incomplete inputRef.');
        continue;
      }

      if (!targetInputs.has(inputRef.targetInput)) {
        validationErrors.push(
          stepLabel(step) +
            " inputRef target input '" +
            inputRef.targetInput +
            "' is not declared by agent '" +
            (stepAgentKey ? agentByKey.get(stepAgentKey)?.name ?? stepAgentKey : 'unknown') +
            "'.",
        );
        continue;
      }

      if (!stepMap.has(inputRef.fromStepId)) {
        validationErrors.push(
          stepLabel(step) +
            ' references unknown source step ' +
            stepReferenceLabel(inputRef.fromStepId, stepMap) +
            ' in an inputRef.',
        );
        continue;
      }

      if (inputRef.fromStepId === step.id) {
        validationErrors.push(stepLabel(step) + ' cannot reference itself in an inputRef.');
        continue;
      }

      if (!ancestorIds.has(inputRef.fromStepId)) {
        validationErrors.push(
          stepLabel(step) +
            ' inputRef from ' +
            stepReferenceLabel(inputRef.fromStepId, stepMap) +
            ' must reference an upstream ancestor.',
        );
        continue;
      }
      const duplicateKey = inputRef.fromStepId + '\u0000' + inputRef.key;
      if (seenInputRefs.has(duplicateKey)) {
        validationErrors.push(
          stepLabel(step) +
            ' has a duplicate inputRef from ' +
            stepReferenceLabel(inputRef.fromStepId, stepMap) +
            " using output key '" +
            inputRef.key +
            "'.",
        );
        continue;
      }
      seenInputRefs.add(duplicateKey);

      if (seenTargetInputs.has(inputRef.targetInput)) {
        validationErrors.push(
          stepLabel(step) + " has a duplicate inputRef target input '" + inputRef.targetInput + "'.",
        );
        continue;
      }
      seenTargetInputs.add(inputRef.targetInput);

      const sourceStep = stepMap.get(inputRef.fromStepId);
      const sourceAgentKey = sourceStep?.assignedAgentId;
      const outputs = sourceAgentKey ? agentByKey.get(sourceAgentKey)?.outputs : undefined;

      if (!outputs) {
        validationErrors.push(
          stepLabel(step) +
            ' inputRef from ' +
            stepReferenceLabel(inputRef.fromStepId, stepMap) +
            " references unknown agent '" +
            (sourceAgentKey ?? 'unknown') +
            "'.",
        );
        continue;
      }

      if (!outputs.includes(inputRef.key)) {
        validationErrors.push(
          stepLabel(step) +
            ' inputRef from ' +
            stepReferenceLabel(inputRef.fromStepId, stepMap) +
            " references undeclared output key '" +
            inputRef.key +
            "'.",
        );
      }
    }
  }

  return validationErrors;
}

function stepLabel(step: SquadBuilderStep): string {
  if (step.name && step.name.trim()) {
    return "Step '" + step.name.trim() + "'";
  }

  return "Step with id '" + step.id + "'";
}

function stepReferenceLabel(stepId: string, stepMap: Map<string, SquadBuilderStep>): string {
  const step = stepMap.get(stepId);
  if (!step) {
    return "Step with id '" + stepId + "'";
  }

  return stepLabel(step);
}

function addGraphEdge(map: Map<string, Set<string>>, sourceStepId: string, targetStepId: string): void {
  const stepIds = map.get(sourceStepId) ?? new Set<string>();
  stepIds.add(targetStepId);
  map.set(sourceStepId, stepIds);
}

function computeAncestors(stepId: string, reverseEdges: Map<string, Set<string>>): Set<string> {
  const ancestors = new Set<string>();
  const queue = [stepId];

  while (queue.length > 0) {
    const currentStepId = queue.shift();
    if (!currentStepId) {
      continue;
    }

    for (const parentStepId of reverseEdges.get(currentStepId) ?? []) {
      if (!ancestors.has(parentStepId)) {
        ancestors.add(parentStepId);
        queue.push(parentStepId);
      }
    }
  }

  return ancestors;
}
