import {
  Component,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { finalize, switchMap } from 'rxjs';

import { ReteSquadFlowEditor } from '../../components/rete-squad-flow-editor/rete-squad-flow-editor';
import {
  SquadStopConfirmDialog,
  SquadStopConfirmDialogData,
} from '../../components/squad-stop-confirm-dialog/squad-stop-confirm-dialog';
import {
  SquadExecutionStatus,
  SquadRunStartResponse,
  SquadStepStatus,
} from '../../../../core/models/squad-run.model';
import { AgentService } from '../../../../core/services/agent.service';
import { SquadApiResponse, SquadService } from '../../../../core/services/squad.service';
import { SquadStepDetailsInspector } from '../../components/squad-step-details-inspector/squad-step-details-inspector';
import { SelectedStepDetails } from '../../components/squad-step-details-inspector/squad-step-details.types';

type LiveRunAgent = {
  agentKey: string;
  name: string;
};

@Component({
  selector: 'app-squad-live-run-page',
  imports: [
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    ReteSquadFlowEditor,
    SquadStepDetailsInspector,
  ],
  templateUrl: './squad-live-run-page.html',
  styleUrls: ['./squad-live-run-page.scss'],
})
export class SquadLiveRunPage implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly agentService = inject(AgentService);
  private readonly squadService = inject(SquadService);
  private readonly dialog = inject(MatDialog);

  private pollingHandle?: number;
  private workflowStartedAt: number | null = null;
  private timerHandle?: number;
  private expandedStepDetailsDialogRef?: MatDialogRef<unknown>;

  @ViewChild('expandedStepDetailsDialogTemplate')
  private expandedStepDetailsDialogTemplate?: TemplateRef<unknown>;

  readonly elapsedSeconds = signal(0);

  readonly squad = signal<SquadApiResponse | null>(null);

  readonly activeSquadRunId = signal<string | null>(null);

  readonly executionStatus = signal<SquadExecutionStatus | null>(null);
  readonly selectedStepId = signal<string | null>(null);

  readonly interactionLocked = signal(true);
  readonly followModeEnabled = signal(true);
  readonly isStoppingWorkflow = signal(false);

  readonly executionEvents = signal<string[]>([]);

  readonly agents = computed<LiveRunAgent[]>(() => {
    return this.agentService
      .getAgents()()
      .map((agent) => ({
        agentKey: agent.agentKey,
        name: agent.name,
      }));
  });

  readonly squadId = computed(() => {
    return this.route.snapshot.paramMap.get('squadId');
  });

  readonly squadRunId = computed(() => {
    return this.route.snapshot.queryParamMap.get('runId');
  });

  readonly agentNamesById = computed(() => {
    return this.agents().reduce<Record<string, string>>((agentNames, agent) => {
      agentNames[agent.agentKey] = agent.name;
      return agentNames;
    }, {});
  });

  readonly stepNamesById = computed(() => {
    const stepNames: Record<string, string> = {};

    this.squad()?.steps.forEach((step) => {
      stepNames[step.id] = step.name;
    });

    this.executionStatus()?.steps.forEach((step) => {
      if (!stepNames[step.stepId]) {
        stepNames[step.stepId] = step.stepName;
      }
    });

    return stepNames;
  });

  readonly completedSteps = computed(() => {
    return this.executionStatus()?.steps.filter((step) => step.status === 'COMPLETED').length ?? 0;
  });

  readonly runningSteps = computed(() => {
    return this.executionStatus()?.steps.filter((step) => step.status === 'RUNNING').length ?? 0;
  });

  readonly failedSteps = computed(() => {
    return this.executionStatus()?.steps.filter((step) => step.status === 'FAILED').length ?? 0;
  });

  readonly pendingSteps = computed(() => {
    return this.executionStatus()?.steps.filter((step) => step.status === 'PENDING').length ?? 0;
  });

  readonly totalSteps = computed(() => {
    return this.executionStatus()?.steps.length ?? this.squad()?.steps.length ?? 0;
  });

  readonly progressPercentage = computed(() => {
    const total = this.totalSteps();

    if (total === 0) {
      return 0;
    }

    return Math.round((this.completedSteps() / total) * 100);
  });

  readonly isWorkflowRunning = computed(() => {
    return this.executionStatus()?.overallStatus === 'RUNNING';
  });

  readonly workflowCancelled = computed(() => {
    return this.executionStatus()?.overallStatus === 'CANCELLED';
  });

  readonly canStopWorkflow = computed(() => {
    return this.isWorkflowRunning() && !this.isStoppingWorkflow() && !!this.activeSquadRunId();
  });

  readonly selectedStepDetails = computed<SelectedStepDetails | null>(() => {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return null;
    }

    const executionStep = this.executionStatus()?.steps.find(
      (step) => step.stepId === selectedStepId,
    );
    const squadStep = this.squad()?.steps.find((step) => step.id === selectedStepId);

    if (!executionStep && !squadStep) {
      return null;
    }

    const hasExecutionData = Boolean(
      executionStep && (executionStep.input !== undefined || executionStep.output !== undefined),
    );
    const startedAt = this.parseTimestamp(executionStep?.startedAt);
    const completedAt = this.parseTimestamp(executionStep?.completedAt);

    return {
      stepId: selectedStepId,
      stepName: executionStep?.stepName ?? squadStep?.name ?? 'Unknown step',
      agentName: this.resolveAgentName(executionStep, squadStep?.agentKey),
      status: executionStep?.status ?? 'PENDING',
      message: executionStep?.message ?? null,
      startedAt,
      completedAt,
      durationMs: executionStep?.durationMs ?? null,
      configuredInputRefs: squadStep?.inputRefs?.map((inputRef) => ({
        targetInput: inputRef.targetInput,
        fromStepId: inputRef.fromStepId,
        key: inputRef.key,
      })) ?? [],
      input: executionStep?.input,
      output: executionStep?.output,
      hasExecutionData,
    };
  });

  readonly formattedDuration = computed(() => {
    const totalSeconds = this.elapsedSeconds();

    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    return [hours, minutes, seconds].map((value) => value.toString().padStart(2, '0')).join(':');
  });

  ngOnInit(): void {
    this.loadSquad();
    this.loadExistingRun();
  }

  ngOnDestroy(): void {
    this.expandedStepDetailsDialogRef?.close();
    this.stopPolling();
    this.stopTimer();
  }

  runWorkflow(): void {
    const squadId = this.squadId();

    if (!squadId) {
      return;
    }

    this.executionStatus.set(null);
    this.selectedStepId.set(null);
    this.followModeEnabled.set(true);
    this.executionEvents.set(['Starting workflow...']);

    this.squadService.startSquadRun(squadId).subscribe({
      next: (response: SquadRunStartResponse) => {
        this.activeSquadRunId.set(response.squadRunId);

        this.executionEvents.update((events) => [...events, 'Workflow started']);

        this.workflowStartedAt = Date.now();
        this.elapsedSeconds.set(0);

        this.timerHandle = window.setInterval(() => {
          if (!this.workflowStartedAt) {
            return;
          }

          this.elapsedSeconds.set(Math.floor((Date.now() - this.workflowStartedAt) / 1000));
        }, 1000);

        this.startPolling(response.squadRunId);
      },
      error: (error) => {
        console.error('Failed to start workflow', error);

        this.executionEvents.update((events) => [...events, 'Failed to start workflow']);
      },
    });
  }

  private loadExistingRun(): void {
    const squadRunId = this.squadRunId();

    if (!squadRunId) {
      return;
    }

    this.activeSquadRunId.set(squadRunId);
    this.executionEvents.set(['Loading existing run...']);

    this.squadService.getSquadRunStatus(squadRunId).subscribe({
      next: (status) => {
        this.executionStatus.set(status);
        this.executionEvents.set(status.steps.map((step) => `${step.stepName}: ${step.status}`));

        if (
          status.overallStatus !== 'COMPLETED' &&
          status.overallStatus !== 'FAILED' &&
          status.overallStatus !== 'CANCELLED'
        ) {
          this.startPolling(squadRunId);
        }
      },
      error: (error) => {
        console.error('Failed to load existing squad run', error);
        this.executionEvents.set(['Failed to load existing run']);
      },
    });
  }

  mapSteps(squad: SquadApiResponse) {
    return squad.steps.map((step, index) => ({
      id: step.id,
      name: step.name,
      assignedAgentId: step.agentKey || null,
      position: {
        x: 160 + index * 220,
        y: 140 + (index % 2) * 140,
      },
    }));
  }

  mapEdges(squad: SquadApiResponse) {
    return squad.edges.map((edge, index) => ({
      id: `edge-${index}`,
      sourceStepId: edge.sourceStepId,
      targetStepId: edge.targetStepId,
    }));
  }

  toggleFollowMode(): void {
    this.followModeEnabled.update((enabled) => !enabled);
  }

  toggleInteractionLock(): void {
    this.interactionLocked.update((locked) => !locked);
  }

  selectStep(stepId: string): void {
    this.selectedStepId.set(stepId);
  }

  closeStepDetailsDrawer(): void {
    this.expandedStepDetailsDialogRef?.close();
    this.selectedStepId.set(null);
  }

  openExpandedStepDetailsDialog(): void {
    if (!this.expandedStepDetailsDialogTemplate || this.expandedStepDetailsDialogRef) {
      return;
    }

    this.expandedStepDetailsDialogRef = this.dialog.open(this.expandedStepDetailsDialogTemplate, {
      width: 'min(96vw, 96rem)',
      maxWidth: '96vw',
      minHeight: '28rem',
      maxHeight: '85vh',
      autoFocus: false,
      restoreFocus: false,
      panelClass: 'squad-step-details-dialog-panel',
    });

    this.expandedStepDetailsDialogRef.afterClosed().subscribe(() => {
      this.expandedStepDetailsDialogRef = undefined;
    });
  }

  closeExpandedStepDetailsDialog(): void {
    this.expandedStepDetailsDialogRef?.close();
  }

  stopWorkflow(): void {
    const squadRunId = this.activeSquadRunId();

    if (!squadRunId || !this.isWorkflowRunning() || this.isStoppingWorkflow()) {
      return;
    }

    const dialogRef = this.dialog.open<SquadStopConfirmDialog, SquadStopConfirmDialogData, boolean>(
      SquadStopConfirmDialog,
      {
        data: {
          squadName: this.squad()?.name ?? 'This workflow',
        },
        width: '28rem',
        maxWidth: '92vw',
        autoFocus: false,
        restoreFocus: false,
        panelClass: 'squad-stop-dialog-panel',
      },
    );

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }

      this.cancelSquadRun(squadRunId);
    });
  }

  private loadSquad(): void {
    const squadId = this.squadId();

    if (!squadId) {
      void this.router.navigate(['/squads']);
      return;
    }

    this.squadService.getSquadByIdFromApi(squadId).subscribe({
      next: (squad) => {
        this.squad.set(squad);
      },
      error: (error) => {
        console.error('Failed to load squad', error);

        void this.router.navigate(['/squads']);
      },
    });
  }

  private startPolling(squadRunId: string): void {
    this.stopPolling();

    this.pollingHandle = window.setInterval(() => {
      this.squadService.getSquadRunStatus(squadRunId).subscribe({
        next: (status) => {
          this.executionStatus.set(status);

          const events = status.steps.map((step) => `${step.stepName}: ${step.status}`);

          this.executionEvents.set(events);

          if (
            status.overallStatus === 'COMPLETED' ||
            status.overallStatus === 'FAILED' ||
            status.overallStatus === 'CANCELLED'
          ) {
            this.stopTimer();
            this.stopPolling();
          }
        },
        error: (error) => {
          console.error('STATUS ERROR', error);

          this.stopPolling();
        },
      });
    }, 1000);
  }

  private cancelSquadRun(squadRunId: string): void {
    this.stopTimer();
    this.isStoppingWorkflow.set(true);

    this.squadService
      .cancelSquadRun(squadRunId)
      .pipe(
        switchMap(() => this.squadService.getSquadRunStatus(squadRunId)),
        finalize(() => this.isStoppingWorkflow.set(false)),
      )
      .subscribe({
        next: (status) => {
          this.stopPolling();
          this.stopTimer();

          this.executionStatus.set(status);

          this.executionEvents.update((events) => [...events, 'Workflow cancelled']);
        },
        error: (error) => {
          console.error('Failed to stop workflow', error);

          this.executionEvents.update((events) => [...events, 'Failed to stop workflow']);
        },
      });
  }
  private stopTimer(): void {
    if (!this.timerHandle) {
      return;
    }

    clearInterval(this.timerHandle);
    this.timerHandle = undefined;
  }

  private stopPolling(): void {
    if (!this.pollingHandle) {
      return;
    }

    clearInterval(this.pollingHandle);
    this.pollingHandle = undefined;
  }

  private resolveAgentName(executionStep?: SquadStepStatus, squadStepAgentKey?: string): string {
    const fallbackAgentKey = executionStep?.agentKey ?? squadStepAgentKey;

    if (!fallbackAgentKey) {
      return 'Unassigned';
    }

    return this.agentNamesById()[fallbackAgentKey] ?? fallbackAgentKey;
  }

  private parseTimestamp(timestamp: string | null | undefined): number | null {
    if (!timestamp) {
      return null;
    }

    const parsedTimestamp = Date.parse(timestamp);

    if (Number.isNaN(parsedTimestamp)) {
      return null;
    }

    return parsedTimestamp;
  }
}
