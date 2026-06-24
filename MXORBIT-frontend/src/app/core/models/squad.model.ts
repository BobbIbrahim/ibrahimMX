export type SquadType = 'hardcoded-flow' | 'prompt-squad';

export type SquadStatus = 'active' | 'draft' | 'paused';

export interface SquadMetrics {
  steps: number;
  objects: number;
  edges: number;
  members: number;
}

export interface Squad {
  id: string;
  name: string;
  description: string;
  type: SquadType;
  status: SquadStatus;
  projectKey: string;
  tags: string[];
  metrics: SquadMetrics;
}