import { Injectable, computed, signal } from '@angular/core';
import { SquadApiResponse } from './squad.service';
import {
  SquadBuilderDraft,
  SquadBuilderEdge,
  SquadBuilderInputRef,
  SquadBuilderStep,
  SquadBuilderType,
  SquadSavePayload,
} from '../models/squad-builder.model';

export type CreateSquadDraftPayload = {
  name: string;
  description: string;
  type: SquadBuilderType;
};

export type UpdateSquadStepPayload = Partial<
  Pick<SquadBuilderStep, 'name' | 'assignedAgentId' | 'parameters' | 'inputRefs'>
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
      steps: [],
      edges: [],
    };

    this.draftSignal.set(draft);
    this.selectedStepIdSignal.set(null);

    return draft;
  }

  loadDraftFromApi(squad: SquadApiResponse): SquadBuilderDraft {
    const draft: SquadBuilderDraft = {
      id: squad.id,
      name: squad.name.trim(),
      description: squad.description?.trim() ?? '',
      type: squad.type as SquadBuilderType,
      steps: squad.steps.map((step, index) => ({
        id: step.id,
        name: step.name,
        assignedAgentId: step.agentKey,
        parameters: {},
        inputRefs: (step.inputRefs ?? []).map((inputRef) => ({
          fromStepId: inputRef.fromStepId,
          key: inputRef.key,
        })),
        position: {
          x: 160 + index * 220,
          y: 140 + (index % 2) * 140,
        },
      })),
      edges: squad.edges.map((edge, index) => ({
        id: this.generateId(`edge-${index + 1}`),
        sourceStepId: edge.sourceStepId,
        targetStepId: edge.targetStepId,
      })),
    };

    const normalizedDraft = this.normalizeDraftInputRefs(draft);
    this.draftSignal.set(normalizedDraft);
    this.selectedStepIdSignal.set(null);

    return normalizedDraft;
  }

  addStep(): SquadBuilderStep {
    const currentDraft = this.requireDraft();

    const stepIndex = currentDraft.steps.length + 1;

    const newStep: SquadBuilderStep = {
      id: this.generateId('step'),
      name: `New Step ${stepIndex}`,
      assignedAgentId: null,
      parameters: {},
      inputRefs: [],
      position: {
        x: 160 + currentDraft.steps.length * 220,
        y: 140 + (currentDraft.steps.length % 2) * 140,
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

  getAncestorSteps(stepId: string): SquadBuilderStep[] {
    const draft = this.draft();

    if (!draft) {
      return [];
    }

    const ancestorStepIds = this.computeAncestorStepIds(stepId, draft.edges);
    const stepsById = new Map(draft.steps.map((step) => [step.id, step]));
    const ancestors: SquadBuilderStep[] = [];

    for (const ancestorStepId of ancestorStepIds) {
      const ancestorStep = stepsById.get(ancestorStepId);
      if (ancestorStep) {
        ancestors.push(ancestorStep);
      }
    }

    return ancestors;
  }

  addSelectedStepInputRef(): void {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return;
    }

    const ancestors = this.getAncestorSteps(selectedStepId);
    const selectedStep = this.selectedStep();

    if (ancestors.length === 0 || !selectedStep) {
      return;
    }

    this.updateSelectedStep({
      inputRefs: [
        ...selectedStep.inputRefs,
        {
          fromStepId: ancestors[0].id,
          key: '',
        },
      ],
    });
  }

  updateSelectedStepInputRef(index: number, patch: Partial<SquadBuilderInputRef>): void {
    const selectedStepId = this.selectedStepId();
    const selectedStep = this.selectedStep();

    if (!selectedStepId || !selectedStep) {
      return;
    }

    if (index < 0 || index >= selectedStep.inputRefs.length) {
      return;
    }

    const nextInputRefs = selectedStep.inputRefs.map((inputRef, inputRefIndex) => {
      if (inputRefIndex !== index) {
        return inputRef;
      }

      return {
        ...inputRef,
        ...patch,
      };
    });

    const updatedInputRef = nextInputRefs[index];
    if (!updatedInputRef) {
      return;
    }

    const ancestorStepIds = new Set(this.computeAncestorStepIds(selectedStepId, this.edges()));
    if (!ancestorStepIds.has(updatedInputRef.fromStepId)) {
      return;
    }

    this.updateSelectedStep({
      inputRefs: nextInputRefs,
    });
  }

  removeSelectedStepInputRef(index: number): void {
    const selectedStep = this.selectedStep();

    if (!selectedStep) {
      return;
    }

    if (index < 0 || index >= selectedStep.inputRefs.length) {
      return;
    }

    this.updateSelectedStep({
      inputRefs: selectedStep.inputRefs.filter((_, inputRefIndex) => inputRefIndex !== index),
    });
  }

  updateStepPosition(
    stepId: string,
    position: {
      x: number;
      y: number;
    },
  ): void {
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
                position: {
                  x: position.x,
                  y: position.y,
                },
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

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        steps: draft.steps.filter((step) => step.id !== stepId),
        edges: draft.edges.filter(
          (edge) => edge.sourceStepId !== stepId && edge.targetStepId !== stepId,
        ),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    if (this.selectedStepId() === stepId) {
      this.selectedStepIdSignal.set(null);
    }
  }

  addEdge(sourceStepId: string, targetStepId: string): boolean {
    let edgeCreated = false;

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      const sourceStepExists = draft.steps.some((step) => step.id === sourceStepId);
      const targetStepExists = draft.steps.some((step) => step.id === targetStepId);

      if (!sourceStepExists || !targetStepExists) {
        return draft;
      }

      if (sourceStepId === targetStepId) {
        return draft;
      }

      const edgeAlreadyExists = draft.edges.some(
        (edge) => edge.sourceStepId === sourceStepId && edge.targetStepId === targetStepId,
      );

      if (edgeAlreadyExists) {
        return draft;
      }

      edgeCreated = true;

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        edges: [
          ...draft.edges,
          {
            id: this.generateId('edge'),
            sourceStepId,
            targetStepId,
          },
        ],
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    return edgeCreated;
  }

  removeEdge(sourceStepId: string, targetStepId: string): void {
    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        edges: draft.edges.filter(
          (edge) => edge.sourceStepId !== sourceStepId || edge.targetStepId !== targetStepId,
        ),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });
  }

  buildSavePayload(): SquadSavePayload | null {
    const draft = this.draft();

    if (!draft) {
      return null;
    }

    return {
      name: draft.name,
      description: draft.description,
      type: draft.type,
      steps: draft.steps.map((step) => ({
        id: step.id,
        name: step.name,
        type: 'AI_AGENT',
        agentKey: step.assignedAgentId ?? '',
        inputRefs: step.inputRefs.map((inputRef) => ({
          fromStepId: inputRef.fromStepId,
          key: inputRef.key,
        })),
      })),
      edges: draft.edges.map((edge) => ({
        sourceStepId: edge.sourceStepId,
        targetStepId: edge.targetStepId,
      })),
    };
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

  private normalizeDraftInputRefs(draft: SquadBuilderDraft): SquadBuilderDraft {
    return {
      ...draft,
      steps: draft.steps.map((step) => ({
        ...step,
        inputRefs: this.filterValidInputRefs(step, draft),
      })),
    };
  }

  private filterValidInputRefs(step: SquadBuilderStep, draft: SquadBuilderDraft): SquadBuilderInputRef[] {
    const ancestorStepIds = new Set(this.computeAncestorStepIds(step.id, draft.edges));

    return step.inputRefs.filter((inputRef) => ancestorStepIds.has(inputRef.fromStepId));
  }

  private computeAncestorStepIds(stepId: string, edges: SquadBuilderEdge[]): string[] {
    const reverseEdges = new Map<string, Set<string>>();
    for (const edge of edges) {
      const parentStepIds = reverseEdges.get(edge.targetStepId) ?? new Set<string>();
      parentStepIds.add(edge.sourceStepId);
      reverseEdges.set(edge.targetStepId, parentStepIds);
    }

    const ancestors = new Set<string>();
    const queue: string[] = [stepId];

    while (queue.length > 0) {
      const currentStepId = queue.shift();
      if (!currentStepId) {
        continue;
      }

      const parentStepIds = reverseEdges.get(currentStepId);
      if (!parentStepIds) {
        continue;
      }

      for (const parentStepId of parentStepIds) {
        if (parentStepId === stepId || ancestors.has(parentStepId)) {
          continue;
        }

        ancestors.add(parentStepId);
        queue.push(parentStepId);
      }
    }

    return Array.from(ancestors);
  }

  private generateId(prefix: string): string {
    return `${prefix}-${crypto.randomUUID()}`;
  }
}
