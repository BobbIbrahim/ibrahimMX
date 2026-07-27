import { SquadStepExecutionStatus } from '../../../../core/models/squad-run.model';

export type SelectedStepDetails = {
  stepId: string;
  stepName: string;
  agentName: string;
  status: SquadStepExecutionStatus;
  message: string | null;
  startedAt: number | null;
  completedAt: number | null;
  durationMs: number | null;
  configuredInputRefs: Array<{
    targetInput: string;
    fromStepId: string;
    key: string;
  }>;
  input?: Record<string, unknown> | null;
  output?: Record<string, unknown> | null;
  hasExecutionData: boolean;
};
