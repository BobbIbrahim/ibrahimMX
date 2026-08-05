export type SquadStepExecutionStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'SKIPPED';

export type SquadRunOverallStatus = 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

export interface SquadRunListItem {
  squadId: string;
  squadName: string;
  squadRunId: string;
  startedAt: string;
  overallStatus?: SquadRunOverallStatus | null;
  completedAt?: string | null;
  durationMs?: number | null;
}

export interface SquadRunStartResponse {
  squadId: string;
  squadRunId: string;
  status: string;
}

export interface SquadStepStatus {
  stepId: string;
  stepName: string;
  status: SquadStepExecutionStatus;
  agentKey?: string | null;
  message?: string | null;
  input?: Record<string, unknown> | null;
  output?: Record<string, unknown> | null;
  startedAt?: string | null;
  completedAt?: string | null;
  durationMs?: number | null;
}

export interface SquadRoutingCheckedEdge {
  edgeId: string;
  targetStepId: string;
  routingType: string;
  condition?: string | null;
  priority?: number | null;
  isDefault?: boolean | null;
  matched: boolean;
  reason?: string | null;
}

export interface SquadRoutingDecision {
  sourceStepId: string;
  selectedEdgeId?: string | null;
  selectedTargetStepId?: string | null;
  outcome: string;
  reason?: string | null;
  checkedEdges: SquadRoutingCheckedEdge[];
}

export interface SquadExecutionStatus {
  squadId: string;
  overallStatus: SquadRunOverallStatus;
  steps: SquadStepStatus[];
  routingDecisions?: SquadRoutingDecision[] | null;
  finalResult?: Record<string, unknown> | null;
  finalResultFieldLabels?: Record<string, string> | null;
}
