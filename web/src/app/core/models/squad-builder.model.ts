import { SquadType } from './squad.model';

export type SquadBuilderType = SquadType;

export type SquadEdgeRoutingType = 'ALWAYS' | 'WHEN';

/**
 * Priority forced onto every default (isDefault = true) route. Kept out of
 * the 1-99 non-default range so it can never collide with a conditional
 * route's priority.
 */
export const DEFAULT_ROUTE_PRIORITY = 999;

/** Minimum accepted priority for a non-default route. */
export const MIN_ROUTE_PRIORITY = 1;

/** Maximum accepted priority for a non-default route. */
export const MAX_ROUTE_PRIORITY = 99;

export interface SquadBuilderPosition {
  x: number;
  y: number;
}

export interface SquadBuilderStep {
  id: string;
  name: string;
  assignedAgentId: string | null;
  parameters: Record<string, string>;
  position: SquadBuilderPosition;
  inputRefs: SquadBuilderInputRef[];
}

export interface SquadBuilderConditional {
  id: string;
  name: string;
  sourceStepId: string;
  position: SquadBuilderPosition;
}

export interface SquadBuilderEdge {
  id: string;
  sourceStepId: string;
  targetStepId: string;
  routingType: SquadEdgeRoutingType;
  condition: string | null;
  priority: number;
  isDefault: boolean;
}

export type SquadBuilderInputRefSourceType = 'MANUAL' | 'STEP_OUTPUT';

export interface SquadBuilderInputRef {
  targetInput: string;
  sourceType: SquadBuilderInputRefSourceType;
  fromStepId?: string;
  key?: string;
}

export interface SquadBuilderDraft {
  id: string;
  name: string;
  description: string;
  type: SquadBuilderType;
  steps: SquadBuilderStep[];
  conditionals: SquadBuilderConditional[];
  edges: SquadBuilderEdge[];
}

export interface SquadSaveStepPayload {
  id: string;
  name: string;
  type: string;
  agentKey: string;
  inputRefs: SquadBuilderInputRef[];
}

export interface SquadSaveEdgePayload {
  sourceStepId: string;
  targetStepId: string;
  routingType: SquadEdgeRoutingType;
  condition: string | null;
  priority: number;
  isDefault: boolean;
}

export interface SquadSavePayload {
  name: string;
  description: string;
  /** Wire-level squad type expected by the API, produced by `toSquadWireType`. */
  type: string;
  steps: SquadSaveStepPayload[];
  edges: SquadSaveEdgePayload[];
}
