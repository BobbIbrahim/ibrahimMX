export type SquadStepExecutionStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export type SquadRunOverallStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

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
  overallStatus: SquadRunOverallStatus;
  steps: SquadStepStatus[];
}
