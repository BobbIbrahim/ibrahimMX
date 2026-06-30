export type ProjectStatus = 'active' | 'paused' | 'completed';

export interface Project {
  id: string;
  name: string;
  description: string;

  status: ProjectStatus;

  repositoryUrl: string;
  defaultBranch: string;
  stack: string[];

  agentIds: string[];

  createdAt: string;
  updatedAt: string;
}