export type SquadBuilderType = 'hardcoded-flow' | 'prompt-squad';

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

export interface SquadBuilderEdge {
  id: string;
  sourceStepId: string;
  targetStepId: string;
}

export interface SquadBuilderInputRef {
  fromStepId: string;
  key: string;
}

export interface SquadBuilderDraft {
  id: string;
  name: string;
  description: string;
  type: SquadBuilderType;
  steps: SquadBuilderStep[];
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
}

export interface SquadSavePayload {
  name: string;
  description: string;
  type: SquadBuilderType;
  steps: SquadSaveStepPayload[];
  edges: SquadSaveEdgePayload[];
}
