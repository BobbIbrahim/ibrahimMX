import { Injectable, computed, signal } from '@angular/core';

import {
  SquadBuilderDraft,
  SquadBuilderEdge,
  SquadBuilderObject,
  SquadBuilderObjectType,
  SquadBuilderStep,
  SquadBuilderType,
} from '../models/squad-builder.model';

export type CreateSquadDraftPayload = {
  name: string;
  description: string;
  type: SquadBuilderType;
  projectKey: string;
  objectTypes: SquadBuilderObjectType[];
};

export type UpdateSquadStepPayload = Partial<
  Pick<
    SquadBuilderStep,
    'name' | 'description' | 'assignedAgentId' | 'triggerObjectType' | 'parameters'
  >
>;

@Injectable({
  providedIn: 'root',
})
export class SquadBuilderStateService {
  private readonly draftSignal = signal<SquadBuilderDraft | null>(null);
  private readonly selectedStepIdSignal = signal<string | null>(null);

  readonly draft = this.draftSignal.asReadonly();
  readonly selectedStepId = this.selectedStepIdSignal.asReadonly();

  readonly steps = computed(() => this.draft()?.steps ?? []);
  readonly edges = computed(() => this.draft()?.edges ?? []);
  readonly objects = computed(() => this.draft()?.objects ?? []);

  readonly selectedStep = computed(() => {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return undefined;
    }

    return this.steps().find((step) => step.id === selectedStepId);
  });

  createDraft(payload: CreateSquadDraftPayload): SquadBuilderDraft {
    const draft: SquadBuilderDraft = {
      id: this.generateId('draft'),
      name: payload.name.trim(),
      description: payload.description.trim(),
      type: payload.type,
      projectKey: payload.projectKey,
      objectTypes: payload.objectTypes,
      steps: [],
      edges: [],
      objects: [],
    };

    this.draftSignal.set(draft);
    this.selectedStepIdSignal.set(null);

    return draft;
  }

  addStep(): SquadBuilderStep {
    const currentDraft = this.requireDraft();

    const stepIndex = currentDraft.steps.length + 1;

    const newStep: SquadBuilderStep = {
      id: this.generateId('step'),
      name: `New Step ${stepIndex}`,
      description: '',
      assignedAgentId: null,
      triggerObjectType: 'ANY',
      parameters: {},
      position: {
        x: 160 + currentDraft.steps.length * 40,
        y: 140 + currentDraft.steps.length * 40,
      },
    };

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        steps: [...draft.steps, newStep],
      };
    });

    this.selectedStepIdSignal.set(newStep.id);

    return newStep;
  }

  selectStep(stepId: string): void {
    const exists = this.steps().some((step) => step.id === stepId);

    if (!exists) {
      return;
    }

    this.selectedStepIdSignal.set(stepId);
  }

  clearSelection(): void {
    this.selectedStepIdSignal.set(null);
  }

  updateSelectedStep(payload: UpdateSquadStepPayload): void {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return;
    }

    this.updateStep(selectedStepId, payload);
  }

  updateStep(stepId: string, payload: UpdateSquadStepPayload): void {
    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        steps: draft.steps.map((step) =>
          step.id === stepId
            ? {
                ...step,
                ...payload,
              }
            : step,
        ),
      };
    });
  }

  deleteSelectedStep(): void {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return;
    }

    this.deleteStep(selectedStepId);
  }

  deleteStep(stepId: string): void {
    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        steps: draft.steps.filter((step) => step.id !== stepId),
        edges: draft.edges.filter(
          (edge) => edge.sourceStepId !== stepId && edge.targetStepId !== stepId,
        ),
      };
    });

    if (this.selectedStepId() === stepId) {
      this.selectedStepIdSignal.set(null);
    }
  }

  addObject(objectType: SquadBuilderObjectType): SquadBuilderObject {
    const currentDraft = this.requireDraft();

    const objectIndex = currentDraft.objects.length + 1;

    const newObject: SquadBuilderObject = {
      id: this.generateId('object'),
      type: objectType,
      name: `${objectType} Object ${objectIndex}`,
      position: {
        x: 220 + currentDraft.objects.length * 40,
        y: 220 + currentDraft.objects.length * 40,
      },
    };

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        objects: [...draft.objects, newObject],
      };
    });

    return newObject;
  }

  addEdge(sourceStepId: string, targetStepId: string): SquadBuilderEdge | null {
    if (sourceStepId === targetStepId) {
      return null;
    }

    const edgeAlreadyExists = this.edges().some(
      (edge) =>
        edge.sourceStepId === sourceStepId && edge.targetStepId === targetStepId,
    );

    if (edgeAlreadyExists) {
      return null;
    }

    const newEdge: SquadBuilderEdge = {
      id: this.generateId('edge'),
      sourceStepId,
      targetStepId,
    };

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        edges: [...draft.edges, newEdge],
      };
    });

    return newEdge;
  }

  buildSavePayload(): SquadBuilderDraft | null {
    return this.draft();
  }

  resetDraft(): void {
    this.draftSignal.set(null);
    this.selectedStepIdSignal.set(null);
  }

  private requireDraft(): SquadBuilderDraft {
    const draft = this.draft();

    if (!draft) {
      throw new Error('No squad draft exists. Create a draft before editing.');
    }

    return draft;
  }

  private generateId(prefix: string): string {
    return `${prefix}-${crypto.randomUUID()}`;
  }
}