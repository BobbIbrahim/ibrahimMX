import { TitleCasePipe } from '@angular/common';
import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Agent } from '../../../../core/models/agent.model';
import {
  SquadBuilderInputRef,
  SquadBuilderStep,
} from '../../../../core/models/squad-builder.model';
import { AgentService } from '../../../../core/services/agent.service';
import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { SquadService } from '../../../../core/services/squad.service';
import { validateSquadWorkflow } from '../../../../core/validation/squad-workflow-validation';
import { ReteSquadFlowEditor } from '../../components/rete-squad-flow-editor/rete-squad-flow-editor';

type BuilderAgent = Pick<Agent, 'agentKey' | 'name' | 'role' | 'inputs' | 'outputs'>;

type ReteConnectionEvent = {
  sourceStepId: string;
  targetStepId: string;
};

type ReteNodePositionChangedEvent = {
  stepId: string;
  position: {
    x: number;
    y: number;
  };
};

@Component({
  selector: 'app-squad-builder-page',
  imports: [
    FormsModule,
    RouterLink,
    TitleCasePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    ReteSquadFlowEditor,
  ],
  templateUrl: './squad-builder-page.html',
  styleUrl: './squad-builder-page.scss',
})
export class SquadBuilderPage implements OnInit {
  private readonly agentService = inject(AgentService);
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly squadService = inject(SquadService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly draft = this.squadBuilderState.draft;
  readonly steps = this.squadBuilderState.steps;
  readonly edges = this.squadBuilderState.edges;
  readonly selectedStep = this.squadBuilderState.selectedStep;
  readonly ancestorStepsForSelectedStep = computed<SquadBuilderStep[]>(() => {
    const selectedStep = this.selectedStep();

    if (!selectedStep) {
      return [];
    }

    return this.squadBuilderState.getAncestorSteps(selectedStep.id);
  });

  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly saveSuccess = signal<string | null>(null);
  readonly isLoadingExistingSquad = signal(false);

  readonly agents = computed<BuilderAgent[]>(() => {
    return this.agentService
      .getAgents()()
      .map((agent) => ({
        agentKey: agent.agentKey,
        name: agent.name,
        role: agent.role,
        inputs: agent.inputs,
        outputs: agent.outputs,
      }));
  });

  readonly selectedStepId = computed(() => this.selectedStep()?.id ?? null);

  readonly assignedAgentCount = computed(() => {
    return this.steps().filter((step) => Boolean(step.assignedAgentId)).length;
  });

  readonly validationIssueCount = computed(() => {
    return this.validationErrors().length;
  });

  readonly workflowReady = computed(() => {
    return this.validationIssueCount() === 0;
  });

  readonly agentNamesById = computed(() => {
    return this.agents().reduce<Record<string, string>>((agentNames, agent) => {
      agentNames[agent.agentKey] = agent.name;
      return agentNames;
    }, {});
  });

  readonly backendReadyPayload = computed(() => {
    return this.squadBuilderState.buildSavePayload();
  });

  readonly selectedStepValidation = computed(() => {
    const step = this.selectedStep();

    if (!step) {
      return null;
    }

    return {
      hasAgent: Boolean(step.assignedAgentId),
      hasInputMappings: step.inputRefs.length > 0,
      hasConnections: this.edges().some(
        (edge) => edge.sourceStepId === step.id || edge.targetStepId === step.id,
      ),
    };
  });

  readonly validationErrors = computed(() => {
    return validateSquadWorkflow(this.draft(), this.agents());
  });

  readonly canSave = computed(() => {
    return (
      Boolean(this.draft()) &&
      this.validationErrors().length === 0 &&
      !this.isSaving() &&
      !this.isLoadingExistingSquad()
    );
  });

  constructor() {
    effect(() => {
      const selectedStep = this.selectedStep();

      if (!selectedStep) {
        return;
      }

      selectedStep.inputRefs.forEach((inputRef, index) => {
        if (!inputRef.key) {
          return;
        }

        const availableOutputKeys = this.getOutputKeysForSourceStepId(inputRef.fromStepId);
        if (availableOutputKeys.includes(inputRef.key)) {
          return;
        }

        this.squadBuilderState.updateSelectedStepInputRef(index, {
          key: '',
        });
      });
    });
  }

  ngOnInit(): void {
    this.loadExistingSquadFromRouteIfNeeded();
  }

  addStep(): void {
    this.squadBuilderState.addStep();
  }

  selectStep(stepId: string): void {
    this.squadBuilderState.selectStep(stepId);
  }

  deleteSelectedStep(): void {
    this.squadBuilderState.deleteSelectedStep();
  }

  updateSelectedStepName(name: string): void {
    this.squadBuilderState.updateSelectedStep({
      name: name.trim(),
    });
  }

  updateSelectedStepAssignedAgent(agentId: string | null): void {
    this.squadBuilderState.updateSelectedStep({
      assignedAgentId: agentId,
    });
  }

  addSelectedStepInputRef(): void {
    this.squadBuilderState.addSelectedStepInputRef();
  }

  updateSelectedStepInputRefSource(index: number, fromStepId: string | null): void {
    if (!fromStepId) {
      return;
    }

    const selectedStep = this.selectedStep();
    const currentInputRef = selectedStep?.inputRefs[index];
    const availableOutputKeys = this.getOutputKeysForSourceStepId(fromStepId);
    const nextKey =
      currentInputRef && availableOutputKeys.includes(currentInputRef.key)
        ? currentInputRef.key
        : '';

    this.squadBuilderState.updateSelectedStepInputRef(index, {
      fromStepId,
      key: nextKey,
    });
  }

  updateSelectedStepInputRefKey(index: number, key: string): void {
    this.squadBuilderState.updateSelectedStepInputRef(index, {
      key,
    });
  }

  removeSelectedStepInputRef(index: number): void {
    this.squadBuilderState.removeSelectedStepInputRef(index);
  }

  handleReteConnectionCreated(event: ReteConnectionEvent): void {
    this.squadBuilderState.addEdge(event.sourceStepId, event.targetStepId);
  }

  handleReteConnectionRemoved(event: ReteConnectionEvent): void {
    this.squadBuilderState.removeEdge(event.sourceStepId, event.targetStepId);
  }

  handleReteNodePositionChanged(event: ReteNodePositionChangedEvent): void {
    this.squadBuilderState.updateStepPosition(event.stepId, event.position);
  }

  getStepName(stepId: string): string {
    return this.steps().find((step) => step.id === stepId)?.name ?? 'Unknown step';
  }

  trackInputRef(index: number, inputRef: SquadBuilderInputRef): string {
    return `${index}-${inputRef.fromStepId}-${inputRef.key}`;
  }

  getOutputKeysForSourceStepId(fromStepId: string | null): string[] {
    if (!fromStepId) {
      return [];
    }

    const sourceStep = this.steps().find((step) => step.id === fromStepId);
    if (!sourceStep?.assignedAgentId) {
      return [];
    }

    return this.getAgentByKey(sourceStep.assignedAgentId)?.outputs ?? [];
  }

  getAgentName(agentKey: string | null): string {
    if (!agentKey) {
      return 'Unassigned';
    }

    return this.getAgentByKey(agentKey)?.name ?? 'Unknown agent';
  }

  getAgentByKey(agentKey: string | null): BuilderAgent | undefined {
    if (!agentKey) {
      return undefined;
    }

    return this.agents().find((agent) => agent.agentKey === agentKey);
  }

  getAgentInitials(agentName: string): string {
    return agentName
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word.charAt(0).toUpperCase())
      .join('');
  }

  saveDraft(): void {
    const payload = this.backendReadyPayload();

    if (!payload || !this.canSave()) {
      return;
    }

    const existingSquadId = this.getRouteSquadId();

    this.isSaving.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(null);

    if (existingSquadId) {
      this.squadService.updateSquad(existingSquadId, payload).subscribe({
        next: (updatedSquad) => {
          this.squadService.upsertSquadFromApi(updatedSquad);
          this.squadBuilderState.resetDraft();

          this.isSaving.set(false);
          this.saveSuccess.set(`Squad "${updatedSquad.name}" was updated successfully.`);

          void this.router.navigate(['/squads']);
        },
        error: (error) => {
          this.isSaving.set(false);
          this.saveError.set(
            'Failed to update squad. Please check the backend logs and try again.',
          );

          console.error('Failed to update squad:', error);
        },
      });

      return;
    }

    this.squadService.createSquad(payload).subscribe({
      next: (createdSquad) => {
        this.squadService.addCreatedSquadFromApi(createdSquad);
        this.squadBuilderState.resetDraft();

        this.isSaving.set(false);
        this.saveSuccess.set(`Squad "${createdSquad.name}" was created successfully.`);

        void this.router.navigate(['/squads']);
      },
      error: (error) => {
        this.isSaving.set(false);
        this.saveError.set('Failed to save squad. Please check the backend logs and try again.');

        console.error('Failed to create squad:', error);
      },
    });
  }

  downloadBackendPayloadJson(): void {
    const payload = this.backendReadyPayload();

    if (!payload) {
      return;
    }

    const blob = new Blob([JSON.stringify(payload, null, 2)], {
      type: 'application/json',
    });

    const objectUrl = URL.createObjectURL(blob);
    const anchor = document.createElement('a');

    anchor.href = objectUrl;
    anchor.download = `${payload.name.toLowerCase().replace(/\s+/g, '-')}-squad.json`;
    anchor.click();

    URL.revokeObjectURL(objectUrl);
  }

  startLiveRun(): void {
    const squadId = this.getRouteSquadId();

    if (!squadId) {
      return;
    }

    void this.router.navigate(['/squads/live-run', squadId]);
  }

  private loadExistingSquadFromRouteIfNeeded(): void {
    const squadId = this.getRouteSquadId();

    if (!squadId) {
      return;
    }

    this.isLoadingExistingSquad.set(true);

    this.squadService.getSquadByIdFromApi(squadId).subscribe({
      next: (squad) => {
        this.squadBuilderState.loadDraftFromApi(squad);
        this.isLoadingExistingSquad.set(false);
      },
      error: (error) => {
        this.isLoadingExistingSquad.set(false);

        console.error('Failed to load squad for editing:', error);

        void this.router.navigate(['/squads']);
      },
    });
  }

  private getRouteSquadId(): string | null {
    const squadId = this.route.snapshot.paramMap.get('squadId');

    if (!squadId || squadId === 'new') {
      return null;
    }

    return squadId;
  }
}
