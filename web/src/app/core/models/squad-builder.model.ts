import { SquadType } from './squad.model';

export type SquadBuilderType = SquadType;

export type SquadEdgeRoutingType = 'ALWAYS' | 'WHEN';

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
