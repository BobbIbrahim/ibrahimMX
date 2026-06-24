export type SquadBuilderType = 'hardcoded-flow' | 'prompt-squad';

export type SquadBuilderObjectType = 'JIRA' | 'PEGA';

export type SquadBuilderStepTrigger = SquadBuilderObjectType | 'ANY';

export interface SquadBuilderPosition {
  x: number;
  y: number;
}

export interface SquadBuilderStep {
  id: string;
  name: string;
  description: string;
  assignedAgentId: string | null;
  triggerObjectType: SquadBuilderStepTrigger;
  parameters: Record<string, string>;
  position: SquadBuilderPosition;
}

export interface SquadBuilderEdge {
  id: string;
  sourceStepId: string;
  targetStepId: string;
}

export interface SquadBuilderObject {
  id: string;
  type: SquadBuilderObjectType;
  name: string;
  position: SquadBuilderPosition;
}

export interface SquadBuilderDraft {
  id: string;
  name: string;
  description: string;
  type: SquadBuilderType;
  projectKey: string;
  objectTypes: SquadBuilderObjectType[];
  steps: SquadBuilderStep[];
  edges: SquadBuilderEdge[];
  objects: SquadBuilderObject[];
}