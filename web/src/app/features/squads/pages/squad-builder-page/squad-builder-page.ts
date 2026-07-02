import { TitleCasePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {Router ,RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { SquadService } from '../../../../core/services/squad.service';
import { ReteSquadFlowEditor } from '../../components/rete-squad-flow-editor/rete-squad-flow-editor';

type BuilderAgent = {
  id: string;
  name: string;
  role: string;
};

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
export class SquadBuilderPage {
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly squadService = inject(SquadService);
  private readonly router = inject(Router);

  readonly draft = this.squadBuilderState.draft;
  readonly steps = this.squadBuilderState.steps;
  readonly edges = this.squadBuilderState.edges;
  readonly selectedStep = this.squadBuilderState.selectedStep;

  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly saveSuccess = signal<string | null>(null);

  readonly agents = signal<BuilderAgent[]>([
    {
      id: 'code-sentinel',
      name: 'Code Sentinel',
      role: 'Code Review Specialist',
    },
    {
      id: 'test-weaver',
      name: 'Test Weaver',
      role: 'Test Generation Specialist',
    },
    {
      id: 'flow-architect',
      name: 'Flow Architect',
      role: 'Workflow Design Specialist',
    },
  ]);

  readonly assignedAgentCount = computed(() => {
    return this.steps().filter((step) => Boolean(step.assignedAgentId)).length;
  });

  readonly agentNamesById = computed(() => {
    return this.agents().reduce<Record<string, string>>((agentNames, agent) => {
      agentNames[agent.id] = agent.name;
      return agentNames;
    }, {});
  });

  readonly backendReadyPayload = computed(() => {
    return this.squadBuilderState.buildSavePayload();
  });

  readonly validationErrors = computed(() => {
    const errors: string[] = [];
    const steps = this.steps();

    if (steps.length === 0) {
      errors.push('Add at least one step before saving this squad.');
    }

    for (const step of steps) {
      if (!step.assignedAgentId) {
        errors.push(`Step "${step.name}" must have an assigned agent.`);
      }
    }

    return errors;
  });

  readonly canSave = computed(() => {
    return Boolean(this.draft()) && this.validationErrors().length === 0 && !this.isSaving();
  });

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

  getAgentName(agentId: string | null): string {
    if (!agentId) {
      return 'Unassigned';
    }

    return this.getAgentById(agentId)?.name ?? 'Unknown agent';
  }

  getAgentById(agentId: string | null): BuilderAgent | undefined {
    if (!agentId) {
      return undefined;
    }

    return this.agents().find((agent) => agent.id === agentId);
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

    if (!payload || !this.canSave() || this.isSaving()) {
      return;
    }

    this.isSaving.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(null);

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
}
