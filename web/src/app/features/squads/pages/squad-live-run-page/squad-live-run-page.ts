import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';

import { ReteSquadFlowEditor } from '../../components/rete-squad-flow-editor/rete-squad-flow-editor';
import {
  SquadExecutionStatus,
  SquadRunStartResponse,
} from '../../../../core/models/squad-run.model';
import { SquadApiResponse, SquadService } from '../../../../core/services/squad.service';

type LiveRunAgent = {
  id: string;
  name: string;
};

@Component({
  selector: 'app-squad-live-run-page',
  imports: [RouterLink, MatButtonModule, ReteSquadFlowEditor],
  templateUrl: './squad-live-run-page.html',
  styleUrl: './squad-live-run-page.scss',
})
export class SquadLiveRunPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly squadService = inject(SquadService);

  private pollingHandle?: number;

  readonly squad = signal<SquadApiResponse | null>(null);

  readonly activeSquadRunId = signal<string | null>(null);

  readonly executionStatus = signal<SquadExecutionStatus | null>(null);

  readonly executionEvents = signal<string[]>([]);

  readonly agents = signal<LiveRunAgent[]>([
    {
      id: 'code-sentinel',
      name: 'Code Sentinel',
    },
    {
      id: 'test-weaver',
      name: 'Test Weaver',
    },
    {
      id: 'flow-architect',
      name: 'Flow Architect',
    },
  ]);

  readonly squadId = computed(() => {
    return this.route.snapshot.paramMap.get('squadId');
  });

  readonly agentNamesById = computed(() => {
    return this.agents().reduce<Record<string, string>>((agentNames, agent) => {
      agentNames[agent.id] = agent.name;
      return agentNames;
    }, {});
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

  ngOnInit(): void {
    this.loadSquad();
  }

  runWorkflow(): void {
    const squadId = this.squadId();

    if (!squadId) {
      return;
    }

    this.executionStatus.set(null);
    this.executionEvents.set(['Starting workflow...']);

    this.squadService.startSquadRun(squadId).subscribe({
      next: (response: SquadRunStartResponse) => {
        this.activeSquadRunId.set(response.squadRunId);

        this.executionEvents.update((events) => [
          ...events,
          'Workflow started',
        ]);

        this.startPolling(response.squadRunId);
      },
      error: (error) => {
        console.error('Failed to start workflow', error);

        this.executionEvents.update((events) => [
          ...events,
          'Failed to start workflow',
        ]);
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
    if (this.pollingHandle) {
      clearInterval(this.pollingHandle);
    }

    this.pollingHandle = window.setInterval(() => {
      this.squadService.getSquadRunStatus(squadRunId).subscribe({
        next: (status) => {
          this.executionStatus.set(status);

          const events = status.steps.map((step) => `${step.stepName}: ${step.status}`);

          this.executionEvents.set(events);

          if (status.overallStatus === 'COMPLETED' || status.overallStatus === 'FAILED') {
            clearInterval(this.pollingHandle);
          }
        },
        error: (error) => {
          console.error('STATUS ERROR', error);

          clearInterval(this.pollingHandle);
        },
      });
    }, 1000);
  }
}
