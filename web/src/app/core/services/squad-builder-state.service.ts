import { Injectable, computed, inject, signal } from '@angular/core';
import { AgentService } from './agent.service';
import { SquadApiResponse } from './squad.service';
import {
  SquadBuilderConditional,
  SquadBuilderDraft,
  SquadBuilderEdge,
  SquadBuilderInputRef,
  SquadBuilderStep,
  SquadBuilderType,
  SquadSavePayload,
  DEFAULT_ROUTE_PRIORITY,
  MIN_ROUTE_PRIORITY,
  MAX_ROUTE_PRIORITY,
} from '../models/squad-builder.model';
import { normalizeSquadType, toSquadWireType } from '../models/squad-type';
import { validateSquadRoutingCondition } from '../validation/squad-routing-condition-validation';
import {
  CONDITIONAL_OFFSET_X,
  CONDITIONAL_OFFSET_Y,
  layoutWorkflowSteps,
} from '../layout/workflow-layout';

export type CreateSquadDraftPayload = {
  name: string;
  description: string;
  type: SquadBuilderType;
};

export type UpdateSquadStepPayload = Partial<
  Pick<SquadBuilderStep, 'name' | 'assignedAgentId' | 'parameters' | 'inputRefs'>
>;

export type AddConditionalRoutePayload = {
  conditionalId: string;
  targetStepId: string;
  routingType: 'WHEN' | 'ALWAYS';
  condition: string | null;
  priority: number;
  isDefault: boolean;
};

export type UpdateConditionalRoutePayload = {
  targetStepId: string;
  routingType: 'WHEN' | 'ALWAYS';
  condition: string | null;
  priority: number;
  isDefault: boolean;
};

@Injectable({
  providedIn: 'root',
})
export class SquadBuilderStateService {
  private readonly agentService = inject(AgentService);
  private readonly draftSignal = signal<SquadBuilderDraft | null>(null);
  private readonly selectedStepIdSignal = signal<string | null>(null);
  private readonly selectedConditionalIdSignal = signal<string | null>(null);

  readonly draft = this.draftSignal.asReadonly();
  readonly selectedStepId = this.selectedStepIdSignal.asReadonly();
  readonly selectedConditionalId = this.selectedConditionalIdSignal.asReadonly();

  readonly steps = computed(() => this.draft()?.steps ?? []);
  readonly conditionals = computed(() => this.draft()?.conditionals ?? []);
  readonly edges = computed(() => this.draft()?.edges ?? []);

  readonly selectedStep = computed(() => {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return undefined;
    }

    return this.steps().find((step) => step.id === selectedStepId);
  });

  readonly selectedConditional = computed(() => {
    const selectedConditionalId = this.selectedConditionalId();

    if (!selectedConditionalId) {
      return undefined;
    }

    return this.conditionals().find((conditional) => conditional.id === selectedConditionalId);
  });

  createDraft(payload: CreateSquadDraftPayload): SquadBuilderDraft {
    const draft: SquadBuilderDraft = {
      id: this.generateId('draft'),
      name: payload.name.trim(),
      description: payload.description.trim(),
      type: payload.type,
      steps: [],
      conditionals: [],
      edges: [],
    };

    this.draftSignal.set(draft);
    this.selectedStepIdSignal.set(null);
    this.selectedConditionalIdSignal.set(null);

    return draft;
  }

  loadDraftFromApi(squad: SquadApiResponse): SquadBuilderDraft {
    const stepPositions = layoutWorkflowSteps(
      squad.steps.map((step) => step.id),
      squad.edges,
    );

    const draft: SquadBuilderDraft = {
      id: squad.id,
      name: squad.name.trim(),
      description: squad.description?.trim() ?? '',
      type: normalizeSquadType(squad.type),
      steps: squad.steps.map((step, index) => ({
        id: step.id,
        name: step.name,
        assignedAgentId: step.agentKey,
        parameters: {},
        inputRefs: (step.inputRefs ?? []).map((inputRef) => ({
          targetInput: inputRef.targetInput ?? '',
          sourceType: inputRef.sourceType ?? 'STEP_OUTPUT',
          fromStepId: inputRef.fromStepId,
          key: inputRef.key,
        })),
        position: stepPositions.get(step.id) ?? { x: 120 + index * 400, y: 120 },
      })),
      conditionals: [],
      edges: squad.edges.map((edge, index) => ({
        id: this.generateId(`edge-${index + 1}`),
        sourceStepId: edge.sourceStepId,
        targetStepId: edge.targetStepId,
        routingType: edge.routingType ?? 'ALWAYS',
        condition: edge.condition ?? null,
        priority: edge.priority ?? (edge.isDefault ? DEFAULT_ROUTE_PRIORITY : MIN_ROUTE_PRIORITY),
        isDefault: edge.isDefault ?? false,
      })),
    };

    const normalizedDraft = this.normalizeDraftInputRefs(draft);
    const draftWithConditionals = this.reconstructConditionals(normalizedDraft);
    this.draftSignal.set(draftWithConditionals);
    this.selectedStepIdSignal.set(null);
    this.selectedConditionalIdSignal.set(null);

    return draftWithConditionals;
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
        x: 120 + currentDraft.steps.length * 400,
        y: 120,
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
    this.selectedConditionalIdSignal.set(null);

    return newStep;
  }

  addConditional(sourceStepId: string): SquadBuilderConditional | null {
    const currentDraft = this.requireDraft();

    const sourceStep = currentDraft.steps.find((step) => step.id === sourceStepId);

    if (!sourceStep) {
      return null;
    }

    const existingConditional = currentDraft.conditionals.find(
      (conditional) => conditional.sourceStepId === sourceStepId,
    );

    if (existingConditional) {
      this.selectConditional(existingConditional.id);
      return null;
    }

    const newConditional: SquadBuilderConditional = {
      id: this.generateId('conditional'),
      name: 'Decision',
      sourceStepId,
      position: {
        x: sourceStep.position.x + CONDITIONAL_OFFSET_X,
        y: sourceStep.position.y + CONDITIONAL_OFFSET_Y,
      },
    };

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      return {
        ...draft,
        conditionals: [...draft.conditionals, newConditional],
      };
    });

    this.selectedConditionalIdSignal.set(newConditional.id);
    this.selectedStepIdSignal.set(null);

    return newConditional;
  }

  updateSelectedConditional(
    payload: Partial<Pick<SquadBuilderConditional, 'name' | 'position'>>,
  ): void {
    const selectedConditionalId = this.selectedConditionalId();

    if (!selectedConditionalId) {
      return;
    }

    this.updateConditional(selectedConditionalId, payload);
  }

  updateConditional(
    conditionalId: string,
    payload: Partial<Pick<SquadBuilderConditional, 'name' | 'position'>>,
  ): void {
    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      const conditionalExists = draft.conditionals.some(
        (conditional) => conditional.id === conditionalId,
      );

      if (!conditionalExists) {
        return draft;
      }

      return {
        ...draft,
        conditionals: draft.conditionals.map((conditional) =>
          conditional.id === conditionalId
            ? {
                ...conditional,
                ...payload,
              }
            : conditional,
        ),
      };
    });
  }

  updateConditionalPosition(
    conditionalId: string,
    position: {
      x: number;
      y: number;
    },
  ): void {
    this.updateConditional(conditionalId, {
      position: {
        x: position.x,
        y: position.y,
      },
    });
  }

  deleteSelectedConditional(): void {
    const selectedConditionalId = this.selectedConditionalId();

    if (!selectedConditionalId) {
      return;
    }

    this.deleteConditional(selectedConditionalId);
  }

  deleteConditional(conditionalId: string): void {
    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      const conditional = draft.conditionals.find((candidate) => candidate.id === conditionalId);

      if (!conditional) {
        return draft;
      }

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        conditionals: draft.conditionals.filter((candidate) => candidate.id !== conditionalId),
        edges: draft.edges.filter((edge) => edge.sourceStepId !== conditional.sourceStepId),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    if (this.selectedConditionalId() === conditionalId) {
      this.selectedConditionalIdSignal.set(null);
    }
  }

  selectStep(stepId: string): void {
    const exists = this.steps().some((step) => step.id === stepId);

    if (!exists) {
      return;
    }

    this.selectedStepIdSignal.set(stepId);
    this.selectedConditionalIdSignal.set(null);
  }

  selectConditional(conditionalId: string): void {
    const exists = this.conditionals().some((conditional) => conditional.id === conditionalId);

    if (!exists) {
      return;
    }

    this.selectedConditionalIdSignal.set(conditionalId);
    this.selectedStepIdSignal.set(null);
  }

  clearSelection(): void {
    this.selectedStepIdSignal.set(null);
    this.selectedConditionalIdSignal.set(null);
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

      const currentStep = draft.steps.find((step) => step.id === stepId);
      if (!currentStep) {
        return draft;
      }

      const nextStep: SquadBuilderStep = {
        ...currentStep,
        ...payload,
      };

      const shouldReconcileTargetInputs = Object.prototype.hasOwnProperty.call(
        payload,
        'assignedAgentId',
      );

      const updatedStep = shouldReconcileTargetInputs
        ? {
            ...nextStep,
            inputRefs: this.filterInputRefsForAssignedAgent(nextStep),
          }
        : nextStep;

      return {
        ...draft,
        steps: draft.steps.map((step) => (step.id === stepId ? updatedStep : step)),
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
    const unmappedInputs = this.getUnmappedAgentInputs(selectedStep);

    if (ancestors.length === 0 || !selectedStep || unmappedInputs.length === 0) {
      return;
    }

    this.updateSelectedStep({
      inputRefs: [
        ...selectedStep.inputRefs,
        {
          targetInput: unmappedInputs[0],
          sourceType: 'STEP_OUTPUT',
          fromStepId: ancestors[0].id,
          key: '',
        },
      ],
    });
  }

  addSelectedStepManualInputRef(): void {
    const selectedStepId = this.selectedStepId();

    if (!selectedStepId) {
      return;
    }

    const draft = this.draft();
    if (!draft) {
      return;
    }

    const ancestors = this.getAncestorStepIds(selectedStepId);
    if (ancestors.length > 0) {
      return; // MANUAL inputs only for root steps
    }

    const selectedStep = this.selectedStep();
    const unmappedInputs = this.getUnmappedAgentInputs(selectedStep);

    if (!selectedStep || unmappedInputs.length === 0) {
      return;
    }

    this.updateSelectedStep({
      inputRefs: [
        ...selectedStep.inputRefs,
        {
          targetInput: unmappedInputs[0],
          sourceType: 'MANUAL',
        },
      ],
    });
  }

  private getAncestorStepIds(stepId: string): string[] {
    const draft = this.draft();
    if (!draft) {
      return [];
    }
    return this.computeAncestorStepIds(stepId, draft.edges);
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

    // MANUAL inputs don't need ancestor validation
    if (updatedInputRef.sourceType === 'MANUAL') {
      this.updateSelectedStep({
        inputRefs: nextInputRefs,
      });
      return;
    }

    // STEP_OUTPUT inputs require ancestor validation
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

      const conditionalIdsToDelete = new Set(
        draft.conditionals
          .filter((conditional) => conditional.sourceStepId === stepId)
          .map((conditional) => conditional.id),
      );

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        steps: draft.steps.filter((step) => step.id !== stepId),
        conditionals: draft.conditionals.filter(
          (conditional) => !conditionalIdsToDelete.has(conditional.id),
        ),
        edges: draft.edges.filter(
          (edge) => edge.sourceStepId !== stepId && edge.targetStepId !== stepId,
        ),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    if (this.selectedStepId() === stepId) {
      this.selectedStepIdSignal.set(null);
    }

    if (
      this.selectedConditionalId() &&
      !this.conditionals().some((conditional) => conditional.id === this.selectedConditionalId())
    ) {
      this.selectedConditionalIdSignal.set(null);
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
            routingType: 'ALWAYS',
            condition: null,
            priority: MIN_ROUTE_PRIORITY,
            isDefault: false,
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

  addConditionalRoute(payload: AddConditionalRoutePayload): SquadBuilderEdge | null {
    let createdEdge: SquadBuilderEdge | null = null;

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      // Resolve the conditional
      const conditional = draft.conditionals.find((cond) => cond.id === payload.conditionalId);
      if (!conditional) {
        return draft;
      }

      // Check source step exists
      const sourceStep = draft.steps.find((step) => step.id === conditional.sourceStepId);
      if (!sourceStep) {
        return draft;
      }

      // Check target step exists
      const targetStep = draft.steps.find((step) => step.id === payload.targetStepId);
      if (!targetStep) {
        return draft;
      }

      // Source and target cannot be the same
      if (conditional.sourceStepId === payload.targetStepId) {
        return draft;
      }

      // Check for duplicate source-target edge
      const edgeAlreadyExists = draft.edges.some(
        (edge) =>
          edge.sourceStepId === conditional.sourceStepId &&
          edge.targetStepId === payload.targetStepId,
      );
      if (edgeAlreadyExists) {
        return draft;
      }

      // Validate priority: default routes are normalized below and never take a
      // user-provided value, so only non-default priorities need range checks.
      if (
        !payload.isDefault &&
        (!Number.isFinite(payload.priority) ||
          !Number.isInteger(payload.priority) ||
          payload.priority < MIN_ROUTE_PRIORITY ||
          payload.priority > MAX_ROUTE_PRIORITY)
      ) {
        return draft;
      }

      // Validate routingType
      if (payload.routingType !== 'WHEN' && payload.routingType !== 'ALWAYS') {
        return draft;
      }

      // WHEN-specific validations
      if (payload.routingType === 'WHEN') {
        // WHEN requires non-blank, non-null condition
        if (!payload.condition || !payload.condition.trim()) {
          return draft;
        }

        // WHEN cannot be marked as default
        if (payload.isDefault) {
          return draft;
        }

        // Validate condition with existing validator
        const validationError = validateSquadRoutingCondition(payload.condition);
        if (validationError !== null) {
          return draft;
        }

        // Check for duplicate priority under same source
        const duplicatePriority = draft.edges.some(
          (edge) =>
            edge.sourceStepId === conditional.sourceStepId &&
            edge.routingType === 'WHEN' &&
            edge.priority === payload.priority,
        );
        if (duplicatePriority) {
          return draft;
        }
      }

      // ALWAYS-specific validations
      if (payload.routingType === 'ALWAYS') {
        // ALWAYS cannot have a condition
        if (payload.condition !== null) {
          return draft;
        }

        // Default route must use ALWAYS
        if (payload.isDefault) {
          if (payload.routingType !== 'ALWAYS') {
            return draft;
          }

          // No duplicate default routes for same source
          const hasExistingDefault = draft.edges.some(
            (edge) =>
              edge.sourceStepId === conditional.sourceStepId && edge.isDefault,
          );
          if (hasExistingDefault) {
            return draft;
          }
        }
      }

      // Create the edge. Default routes always use the reserved priority
      // constant; the caller's priority is never trusted for a default route.
      const edge: SquadBuilderEdge = {
        id: this.generateId('edge'),
        sourceStepId: conditional.sourceStepId,
        targetStepId: payload.targetStepId,
        routingType: payload.routingType,
        condition: payload.condition,
        priority: payload.isDefault ? DEFAULT_ROUTE_PRIORITY : payload.priority,
        isDefault: payload.isDefault,
      };

      createdEdge = edge;

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        edges: [...draft.edges, edge],
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    return createdEdge;
  }

  updateConditionalRoute(
    edgeId: string,
    payload: UpdateConditionalRoutePayload,
  ): SquadBuilderEdge | null {
    let updatedEdge: SquadBuilderEdge | null = null;

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      // Find the edge to update
      const edge = draft.edges.find((e) => e.id === edgeId);
      if (!edge) {
        return draft;
      }

      // Check that source step owns a conditional
      const sourceConditional = draft.conditionals.find(
        (conditional) => conditional.sourceStepId === edge.sourceStepId,
      );
      if (!sourceConditional) {
        return draft;
      }

      // Check target step exists
      const targetStep = draft.steps.find((step) => step.id === payload.targetStepId);
      if (!targetStep) {
        return draft;
      }

      // Source and target cannot be the same
      if (edge.sourceStepId === payload.targetStepId) {
        return draft;
      }

      // Check for duplicate source-target edge (excluding the edge being updated)
      const edgeAlreadyExists = draft.edges.some(
        (e) =>
          e.id !== edgeId &&
          e.sourceStepId === edge.sourceStepId &&
          e.targetStepId === payload.targetStepId,
      );
      if (edgeAlreadyExists) {
        return draft;
      }

      // Validate priority: default routes are normalized below and never take a
      // user-provided value, so only non-default priorities need range checks.
      if (
        !payload.isDefault &&
        (!Number.isFinite(payload.priority) ||
          !Number.isInteger(payload.priority) ||
          payload.priority < MIN_ROUTE_PRIORITY ||
          payload.priority > MAX_ROUTE_PRIORITY)
      ) {
        return draft;
      }

      // Validate routingType
      if (payload.routingType !== 'WHEN' && payload.routingType !== 'ALWAYS') {
        return draft;
      }

      // WHEN-specific validations
      if (payload.routingType === 'WHEN') {
        // WHEN requires non-blank, non-null condition
        if (!payload.condition || !payload.condition.trim()) {
          return draft;
        }

        // WHEN cannot be marked as default
        if (payload.isDefault) {
          return draft;
        }

        // Validate condition with existing validator
        const validationError = validateSquadRoutingCondition(payload.condition);
        if (validationError !== null) {
          return draft;
        }

        // Check for duplicate priority under same source (excluding the edge being updated)
        const duplicatePriority = draft.edges.some(
          (e) =>
            e.id !== edgeId &&
            e.sourceStepId === edge.sourceStepId &&
            e.routingType === 'WHEN' &&
            e.priority === payload.priority,
        );
        if (duplicatePriority) {
          return draft;
        }
      }

      // ALWAYS-specific validations
      if (payload.routingType === 'ALWAYS') {
        // ALWAYS cannot have a condition
        if (payload.condition !== null) {
          return draft;
        }

        // Default route must use ALWAYS
        if (payload.isDefault) {
          if (payload.routingType !== 'ALWAYS') {
            return draft;
          }

          // No duplicate default routes for same source (excluding the edge being updated)
          const hasExistingDefault = draft.edges.some(
            (e) =>
              e.id !== edgeId &&
              e.sourceStepId === edge.sourceStepId &&
              e.isDefault,
          );
          if (hasExistingDefault) {
            return draft;
          }
        }
      }

      // Update the edge, preserving id and sourceStepId. Default routes always
      // use the reserved priority constant; the caller's priority is never
      // trusted for a default route.
      const nextEdge: SquadBuilderEdge = {
        id: edge.id,
        sourceStepId: edge.sourceStepId,
        targetStepId: payload.targetStepId,
        routingType: payload.routingType,
        condition: payload.condition,
        priority: payload.isDefault ? DEFAULT_ROUTE_PRIORITY : payload.priority,
        isDefault: payload.isDefault,
      };

      updatedEdge = nextEdge;

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        edges: draft.edges.map((e) => (e.id === edgeId ? nextEdge : e)),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    return updatedEdge;
  }

  removeConditionalRoute(edgeId: string): boolean {
    let removed = false;

    this.draftSignal.update((draft) => {
      if (!draft) {
        return draft;
      }

      // Find the edge to remove
      const edge = draft.edges.find((e) => e.id === edgeId);
      if (!edge) {
        return draft;
      }

      // Check that the edge source owns a conditional
      const sourceConditional = draft.conditionals.find(
        (conditional) => conditional.sourceStepId === edge.sourceStepId,
      );
      if (!sourceConditional) {
        return draft;
      }

      removed = true;

      const nextDraft: SquadBuilderDraft = {
        ...draft,
        edges: draft.edges.filter((e) => e.id !== edgeId),
      };

      return this.normalizeDraftInputRefs(nextDraft);
    });

    return removed;
  }

  buildSavePayload(): SquadSavePayload | null {
    const draft = this.draft();

    if (!draft) {
      return null;
    }

    return {
      name: draft.name,
      description: draft.description,
      type: toSquadWireType(draft.type),
      steps: draft.steps.map((step) => ({
        id: step.id,
        name: step.name,
        type: 'AI_AGENT',
        agentKey: step.assignedAgentId ?? '',
        inputRefs: step.inputRefs.map((inputRef) => ({
          targetInput: inputRef.targetInput,
          sourceType: inputRef.sourceType,
          fromStepId: inputRef.fromStepId,
          key: inputRef.key,
        })),
      })),
      edges: draft.edges.map((edge) => ({
        sourceStepId: edge.sourceStepId,
        targetStepId: edge.targetStepId,
        routingType: edge.routingType,
        condition: edge.condition,
        priority: edge.priority,
        isDefault: edge.isDefault,
      })),
    };
  }

  private reconstructConditionals(draft: SquadBuilderDraft): SquadBuilderDraft {
    // Group edges by sourceStepId
    const edgesBySourceStepId = new Map<string, SquadBuilderEdge[]>();
    for (const edge of draft.edges) {
      const sourceEdges = edgesBySourceStepId.get(edge.sourceStepId) ?? [];
      sourceEdges.push(edge);
      edgesBySourceStepId.set(edge.sourceStepId, sourceEdges);
    }

    // Create a map of steps for quick lookup
    const stepsById = new Map(draft.steps.map((step) => [step.id, step]));

    // Reconstruct conditionals for sources with WHEN edges
    const reconstructedConditionals: SquadBuilderConditional[] = [];
    for (const [sourceStepId, edges] of edgesBySourceStepId.entries()) {
      // Check if this source has at least one WHEN edge
      const hasWhenEdge = edges.some((edge) => edge.routingType === 'WHEN');

      if (hasWhenEdge) {
        const sourceStep = stepsById.get(sourceStepId);
        if (sourceStep) {
          const conditional: SquadBuilderConditional = {
            id: this.generateId('conditional'),
            name: 'Decision',
            sourceStepId,
            position: {
              x: sourceStep.position.x + CONDITIONAL_OFFSET_X,
              y: sourceStep.position.y + CONDITIONAL_OFFSET_Y,
            },
          };
          reconstructedConditionals.push(conditional);
        }
      }
    }

    return {
      ...draft,
      conditionals: reconstructedConditionals,
    };
  }

  resetDraft(): void {
    this.draftSignal.set(null);
    this.selectedStepIdSignal.set(null);
    this.selectedConditionalIdSignal.set(null);
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

  private filterValidInputRefs(
    step: SquadBuilderStep,
    draft: SquadBuilderDraft,
  ): SquadBuilderInputRef[] {
    const ancestorStepIds = new Set(this.computeAncestorStepIds(step.id, draft.edges));

    return step.inputRefs.filter((inputRef) => {
      if (inputRef.sourceType === 'MANUAL') {
        return true; // MANUAL inputs are always valid
      }
      return ancestorStepIds.has(inputRef.fromStepId);
    });
  }

  private filterInputRefsForAssignedAgent(step: SquadBuilderStep): SquadBuilderInputRef[] {
    const inputs = this.getAssignedAgentInputs(step);

    if (inputs.length === 0) {
      return [];
    }

    return step.inputRefs.filter((inputRef) => inputs.includes(inputRef.targetInput));
  }

  private getUnmappedAgentInputs(step: SquadBuilderStep | undefined): string[] {
    if (!step?.assignedAgentId) {
      return [];
    }

    const agentInputs = this.getAssignedAgentInputs(step);
    if (agentInputs.length === 0) {
      return [];
    }

    const mappedTargetInputs = new Set(
      step.inputRefs
        .map((inputRef) => inputRef.targetInput)
        .filter((targetInput) => Boolean(targetInput?.trim())),
    );

    return agentInputs.filter((input) => !mappedTargetInputs.has(input));
  }

  private getAssignedAgentInputs(step: SquadBuilderStep): string[] {
    const assignedAgentId = step.assignedAgentId;
    if (!assignedAgentId) {
      return [];
    }

    return this.agentService.getAgentByKey(assignedAgentId)?.inputs ?? [];
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
