export type SquadType = 'hardcoded-flow' | 'prompt-squad';

export type SquadStatus = 'live' | 'draft' | 'paused';

export interface Squad {
  id: string;
  name: string;
  type: SquadType;
  status: SquadStatus;
  tenant: string;
  description: string;
  tags: string[];
  stepsCount: number;
  objectsCount: number;
  edgesCount: number;
  membersCount: number;
}
