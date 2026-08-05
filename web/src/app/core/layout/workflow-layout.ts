export interface WorkflowLayoutEdge {
  sourceStepId: string;
  targetStepId: string;
}

export interface WorkflowLayoutPosition {
  x: number;
  y: number;
}

const ORIGIN_X = 120;
const ORIGIN_Y = 120;
const COLUMN_GAP = 400;
const ROW_GAP = 190;

/** Horizontal offset that centers a decision diamond in the gap after its source step. */
export const CONDITIONAL_OFFSET_X = 230;

/** Vertical offset that aligns the taller decision diamond with the centre of its source step. */
export const CONDITIONAL_OFFSET_Y = -29;

/**
 * Lays steps out in dependency columns so the flow always reads left to right,
 * with parallel branches stacked and vertically centred against the widest column.
 */
export function layoutWorkflowSteps(
  stepIds: string[],
  edges: WorkflowLayoutEdge[],
): Map<string, WorkflowLayoutPosition> {
  const knownStepIds = new Set(stepIds);
  const relevantEdges = edges.filter(
    (edge) => knownStepIds.has(edge.sourceStepId) && knownStepIds.has(edge.targetStepId),
  );

  const depthByStepId = new Map(stepIds.map((stepId) => [stepId, 0]));

  // Relaxation instead of a topological sort so malformed or cyclic graphs still lay out.
  for (let pass = 0; pass < stepIds.length; pass += 1) {
    let changed = false;

    for (const edge of relevantEdges) {
      const candidateDepth = depthByStepId.get(edge.sourceStepId)! + 1;

      if (candidateDepth > depthByStepId.get(edge.targetStepId)!) {
        depthByStepId.set(edge.targetStepId, candidateDepth);
        changed = true;
      }
    }

    if (!changed) {
      break;
    }
  }

  const columns = new Map<number, string[]>();

  for (const stepId of stepIds) {
    const depth = depthByStepId.get(stepId)!;
    columns.set(depth, [...(columns.get(depth) ?? []), stepId]);
  }

  const tallestColumn = Math.max(...[...columns.values()].map((column) => column.length));
  const positions = new Map<string, WorkflowLayoutPosition>();

  for (const [depth, columnStepIds] of columns) {
    const columnOffsetY = ((tallestColumn - columnStepIds.length) * ROW_GAP) / 2;

    columnStepIds.forEach((stepId, row) => {
      positions.set(stepId, {
        x: ORIGIN_X + depth * COLUMN_GAP,
        y: ORIGIN_Y + columnOffsetY + row * ROW_GAP,
      });
    });
  }

  return positions;
}
