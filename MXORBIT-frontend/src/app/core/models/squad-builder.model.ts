export type SquadBuilderType = 'hardcoded-flow' | 'prompt-squad';

export interface SquadBuilderPosition {
  x: number;
  y: number;
}

export interface SquadBuilderStep {
  id: string;
  name: string;
  description: string;
  assignedAgentId: string | null;
  parameters: Record<string, string>;
  position: SquadBuilderPosition;
}

export interface SquadBuilderEdge {
  id: string;
  sourceStepId: string;
  targetStepId: string;
}

export interface SquadBuilderDraft {
  id: string;
  name: string;
  description: string;
  type: SquadBuilderType;
  projectKey: string;
  steps: SquadBuilderStep[];
  edges: SquadBuilderEdge[];
}