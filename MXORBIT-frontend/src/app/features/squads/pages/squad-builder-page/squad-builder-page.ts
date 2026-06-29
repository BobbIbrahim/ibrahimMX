import { JsonPipe, TitleCasePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Squad } from '../../../../core/models/squad.model';
import { SquadBuilderDraft } from '../../../../core/models/squad-builder.model';
import { AgentService } from '../../../../core/services/agent.service';
import { SquadBuilderStateService } from '../../../../core/services/squad-builder-state.service';
import { SquadService } from '../../../../core/services/squad.service';
import { ReteSquadFlowEditor } from '../../components/rete-squad-flow-editor/rete-squad-flow-editor';

interface ReteConnectionCreatedEvent {
  sourceStepId: string;
  targetStepId: string;
}

interface ReteNodePositionChangedEvent {
  stepId: string;
  position: {
    x: number;
    y: number;
  };
}

interface ReteConnectionRemovedEvent {
  sourceStepId: string;
  targetStepId: string;
}

@Component({
  selector: 'app-squad-builder-page',
  imports: [
    RouterLink,
    JsonPipe,
    TitleCasePipe,
    FormsModule,
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
  private readonly router = inject(Router);
  private readonly squadService = inject(SquadService);
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly agentService = inject(AgentService);

  readonly draft = this.squadBuilderState.draft;
  readonly steps = this.squadBuilderState.steps;
  readonly edges = this.squadBuilderState.edges;
  readonly selectedStep = this.squadBuilderState.selectedStep;
  readonly selectedStepId = this.squadBuilderState.selectedStepId;

  readonly agents = this.agentService.getAgents();

  readonly assignedAgentCount = computed(() => {
    const assignedAgentIds = this.steps()
      .map((step) => step.assignedAgentId)
      .filter((agentId): agentId is string => Boolean(agentId));

    return new Set(assignedAgentIds).size;
  });

  readonly validationErrors = computed(() => {
    const draft = this.draft();
    const errors: string[] = [];

    if (!draft) {
      errors.push('No squad draft exists.');
      return errors;
    }

    if (draft.name.trim().length === 0) {
      errors.push('Squad name is required.');
    }

    if (draft.projectKey.trim().length === 0) {
      errors.push('Project key is required.');
    }

    if (draft.steps.length < 2) {
      errors.push('At least two steps are required.');
    }

    if (draft.edges.length < 1) {
      errors.push('At least one edge is required.');
    }

    draft.steps.forEach((step, index) => {
      const stepNumber = index + 1;

      if (step.name.trim().length === 0) {
        errors.push(`Step ${stepNumber} must have a name.`);
      }

      if (!step.assignedAgentId) {
        errors.push(`Step "${step.name || stepNumber}" must have an assigned agent.`);
      }
    });

    const stepIds = new Set(draft.steps.map((step) => step.id));

    draft.edges.forEach((edge, index) => {
      const edgeNumber = index + 1;

      if (!stepIds.has(edge.sourceStepId)) {
        errors.push(`Edge ${edgeNumber} has an invalid source step.`);
      }

      if (!stepIds.has(edge.targetStepId)) {
        errors.push(`Edge ${edgeNumber} has an invalid target step.`);
      }
    });

    return errors;
  });

  readonly canSave = computed(() => this.validationErrors().length === 0);

  addStep(): void {
    this.squadBuilderState.addStep();
  }

  selectStep(stepId: string): void {
    this.squadBuilderState.selectStep(stepId);
  }

  handleReteConnectionCreated(event: ReteConnectionCreatedEvent): void {
    this.squadBuilderState.addEdge(event.sourceStepId, event.targetStepId);
  }

  handleReteConnectionRemoved(event: ReteConnectionRemovedEvent): void {
  this.squadBuilderState.removeEdge(event.sourceStepId, event.targetStepId);
}


  handleReteNodePositionChanged(event: ReteNodePositionChangedEvent): void {
  this.squadBuilderState.updateStepPosition(event.stepId, event.position);
}



  updateSelectedStepName(name: string): void {
    this.squadBuilderState.updateSelectedStep({ name });
  }

  updateSelectedStepDescription(description: string): void {
    this.squadBuilderState.updateSelectedStep({ description });
  }

  updateSelectedStepAssignedAgent(assignedAgentId: string | null): void {
    this.squadBuilderState.updateSelectedStep({ assignedAgentId });
  }

  deleteSelectedStep(): void {
    this.squadBuilderState.deleteSelectedStep();
  }

  getAgentName(agentId: string | null): string {
    if (!agentId) {
      return 'Unassigned';
    }

    return this.agents().find((agent) => agent.id === agentId)?.name ?? 'Unknown agent';
  }

  getStepName(stepId: string): string {
    return this.steps().find((step) => step.id === stepId)?.name ?? 'Unknown step';
  }

  saveDraft(): void {
    const validationErrors = this.validationErrors();

    if (validationErrors.length > 0) {
      console.warn('Squad builder validation failed:', validationErrors);
      return;
    }

    const payload = this.buildCleanSavePayload();

    if (!payload) {
      return;
    }

    const savedSquad = this.convertDraftToSquad(payload);

    this.squadService.addSquad(savedSquad);
    this.squadBuilderState.resetDraft();

    console.log('Squad builder backend-ready payload:', payload);
    console.log('Saved local squad:', savedSquad);

    this.router.navigate(['/squads']);
  }

  private buildCleanSavePayload(): SquadBuilderDraft | null {
    const draft = this.squadBuilderState.buildSavePayload();

    if (!draft) {
      return null;
    }

    return {
      id: draft.id,
      name: draft.name.trim(),
      description: draft.description.trim(),
      type: draft.type,
      projectKey: draft.projectKey.trim(),
      steps: draft.steps.map((step) => ({
        id: step.id,
        name: step.name.trim(),
        description: step.description.trim(),
        assignedAgentId: step.assignedAgentId,
        parameters: { ...step.parameters },
        position: {
          x: step.position.x,
          y: step.position.y,
        },
      })),
      edges: draft.edges.map((edge) => ({
        id: edge.id,
        sourceStepId: edge.sourceStepId,
        targetStepId: edge.targetStepId,
      })),
    };
  }

  private convertDraftToSquad(draft: SquadBuilderDraft): Squad {
    const assignedAgentIds = draft.steps
      .map((step) => step.assignedAgentId)
      .filter((agentId): agentId is string => Boolean(agentId));

    const uniqueAssignedAgentCount = new Set(assignedAgentIds).size;

    return {
      id: `squad-${crypto.randomUUID()}`,
      name: draft.name,
      description: draft.description,
      type: draft.type,
      status: 'draft',
      projectKey: draft.projectKey,
      tags: ['Draft', draft.type === 'hardcoded-flow' ? 'Hardcoded Flow' : 'Prompt Squad'],
      metrics: {
        steps: draft.steps.length,
        objects: 0,
        edges: draft.edges.length,
        members: uniqueAssignedAgentCount,
      },
    };
  }
}
