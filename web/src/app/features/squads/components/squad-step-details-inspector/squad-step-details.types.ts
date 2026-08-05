import { SquadBuilderInputRefSourceType } from '../../../../core/models/squad-builder.model';
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
    sourceType: SquadBuilderInputRefSourceType;
    fromStepId: string | null;
    key: string | null;
  }>;
  input?: Record<string, unknown> | null;
  output?: Record<string, unknown> | null;
  hasExecutionData: boolean;
};
