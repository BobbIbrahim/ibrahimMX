export type SquadStepExecutionStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface SquadRunStartResponse {
  squadId: string;
  squadRunId: string;
  status: string;
}

export interface SquadStepStatus {
  stepId: string;
  stepName: string;
  status: SquadStepExecutionStatus;
  message: string;
}

export interface SquadExecutionStatus {
  squadId: string;
  overallStatus: string;
  steps: SquadStepStatus[];
}

export interface SquadStepStatus {
  stepId: string;
  stepName: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  message: string;
}

export interface SquadExecutionStatus {
  squadId: string;
  overallStatus: string;
  steps: SquadStepStatus[];
}
