import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { Agent } from '../../../../core/models/agent.model';
import {
  SquadBuilderInputRef,
  SquadBuilderStep,
  SquadBuilderEdge,
} from '../../../../core/models/squad-builder.model';
import { AgentService } from '../../../../core/services/agent.service';
import { SquadBuilderStateService, UpdateConditionalRoutePayload, AddConditionalRoutePayload } from '../../../../core/services/squad-builder-state.service';
import { SquadService } from '../../../../core/services/squad.service';
import { getSquadTypeLabel } from '../../../../core/models/squad-type';
import { SquadType } from '../../../../core/models/squad.model';
import { validateSquadWorkflow } from '../../../../core/validation/squad-workflow-validation';
import { validateSquadRoutingCondition } from '../../../../core/validation/squad-routing-condition-validation';
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

type AddRouteOperator = 'equals' | 'notEquals' | 'in' | 'contains';

type AddRouteFormState = {
  targetStepId: string;
  outputField: string;
  operator: AddRouteOperator;
  expectedValue: string;
  priority: string;
  isDefault: boolean;
};

@Component({
  selector: 'app-squad-builder-page',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    ReteSquadFlowEditor,
  ],
  templateUrl: './squad-builder-page.html',
  styleUrl: './squad-builder-page.scss',
  styles: [
    `
      :host ::ng-deep .squad-save-success-snackbar {
        --mdc-snackbar-container-color: #15803d;
        --mdc-snackbar-supporting-text-color: #ffffff;
        --mat-snack-bar-button-color: #ffffff;
      }

      :host ::ng-deep .squad-save-success-snackbar .mdc-snackbar__surface {
        border: 1px solid #22c55e;
        border-radius: 0.85rem;
        box-shadow: 0 1rem 2.5rem rgba(21, 128, 61, 0.28);
      }

      :host ::ng-deep .squad-save-success-snackbar .mdc-snackbar__label {
        font-weight: 700;
      }
    `,
  ],
})
export class SquadBuilderPage implements OnInit {
  private readonly agentService = inject(AgentService);
  private readonly squadBuilderState = inject(SquadBuilderStateService);
  private readonly squadService = inject(SquadService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  private readonly persistedSquadId = signal<string | null>(null);

  // Form state for Add/Edit Route
  readonly addRouteFormVisible = signal(false);
  readonly editingEdgeId = signal<string | null>(null);
  readonly addRouteFormState = signal<AddRouteFormState>({
    targetStepId: '',
    outputField: '',
    operator: 'equals',
    expectedValue: '',
    priority: '',
    isDefault: false,
  });

  // Deletion confirmation states
  readonly deleteRouteConfirmId = signal<string | null>(null);
  readonly deleteConditionalConfirmVisible = signal(false);

  readonly draft = this.squadBuilderState.draft;
  readonly steps = this.squadBuilderState.steps;
  readonly edges = this.squadBuilderState.edges;
  readonly conditionals = this.squadBuilderState.conditionals;
  readonly selectedStep = this.squadBuilderState.selectedStep;
  readonly selectedStepId = computed(() => this.selectedStep()?.id ?? null);
  readonly selectedConditional = this.squadBuilderState.selectedConditional;
  readonly selectedConditionalId = computed(() => this.selectedConditional()?.id ?? null);
  readonly selectedConditionalSourceStep = computed(() => {
    const selectedConditional = this.selectedConditional();
    if (!selectedConditional) {
      return undefined;
    }
    return this.steps().find((step) => step.id === selectedConditional.sourceStepId);
  });
  readonly selectedConditionalRoutes = computed(() => {
    const selectedConditional = this.selectedConditional();
    if (!selectedConditional) {
      return [];
    }
    const routes = this.edges().filter(
      (edge) => edge.sourceStepId === selectedConditional.sourceStepId,
    );
    return routes.sort((a, b) => {
      if (a.isDefault) {
        return 1;
      }
      if (b.isDefault) {
        return -1;
      }
      return a.priority - b.priority;
    });
  });
  readonly ancestorStepsForSelectedStep = computed<SquadBuilderStep[]>(() => {
    const selectedStep = this.selectedStep();

    if (!selectedStep) {
      return [];
    }

    return this.squadBuilderState.getAncestorSteps(selectedStep.id);
  });

  readonly unmappedAgentInputsForSelectedStep = computed(() => {
    const selectedStep = this.selectedStep();

    if (!selectedStep?.assignedAgentId) {
      return [];
    }

    const agent = this.getAgentByKey(selectedStep.assignedAgentId);
    if (!agent?.inputs.length) {
      return [];
    }

    const mappedTargetInputs = new Set(
      selectedStep.inputRefs
        .map((inputRef) => inputRef.targetInput)
        .filter((targetInput): targetInput is string => Boolean(targetInput)),
    );

    return agent.inputs.filter((targetInput) => !mappedTargetInputs.has(targetInput));
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

  readonly assignedAgentCount = computed(() => {
    return this.steps().filter((step) => Boolean(step.assignedAgentId)).length;
  });

  readonly validationIssueCount = computed(() => {
    return this.validationErrors().length;
  });

  readonly workflowReady = computed(() => {
    return this.validationIssueCount() === 0;
  });

  readonly isSelectedStepRoot = computed(() => {
    return this.ancestorStepsForSelectedStep().length === 0;
  });

  readonly canAddSelectedStepInputRef = computed(() => {
    const selectedStep = this.selectedStep();

    return (
      Boolean(selectedStep?.assignedAgentId) &&
      this.ancestorStepsForSelectedStep().length > 0 &&
      this.unmappedAgentInputsForSelectedStep().length > 0
    );
  });

  readonly canAddSelectedStepManualInputRef = computed(() => {
    const selectedStep = this.selectedStep();

    return (
      Boolean(selectedStep?.assignedAgentId) &&
      this.isSelectedStepRoot() &&
      this.unmappedAgentInputsForSelectedStep().length > 0
    );
  });

  readonly canAddConditional = computed(() => {
    const selectedStep = this.selectedStep();
    return Boolean(selectedStep?.assignedAgentId);
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

  // Add Route form computed properties and validators
  readonly addRouteAvailableTargetSteps = computed(() => {
    const sourceConditional = this.selectedConditional();
    if (!sourceConditional) {
      return [];
    }

    // Use all steps (executable steps are determined at save time, not here)
    const allSteps = this.steps();

    // Exclude the conditional source step
    const filtered = allSteps.filter((step) => step.id !== sourceConditional.sourceStepId);

    // Get already-used targets from routes originating from the same source
    const usedTargets = new Set(
      this.selectedConditionalRoutes()
        .filter((route) => route.id !== this.editingEdgeId()) // Exclude the currently edited edge
        .map((route) => route.targetStepId)
    );

    return filtered.filter((step) => !usedTargets.has(step.id));
  });

  readonly addRouteSourceOutputs = computed(() => {
    const sourceStep = this.selectedConditionalSourceStep();
    if (!sourceStep?.assignedAgentId) {
      return [];
    }
    return this.getAgentByKey(sourceStep.assignedAgentId)?.outputs ?? [];
  });

  readonly addRouteConditionPreview = computed(() => {
    return this.buildAddRouteCondition();
  });

  readonly addRouteHasExistingDefault = computed(() => {
    return this.selectedConditionalRoutes().some((route) => route.isDefault);
  });

  readonly addRouteValidationMessage = computed<string | null>(() => {
    const formState = this.addRouteFormState();

    // Target is always required
    if (!formState.targetStepId) {
      return 'Select a target step.';
    }

    // Default route conflict check
    if (formState.isDefault && this.addRouteHasExistingDefault()) {
      return 'A default route already exists for this conditional.';
    }

    if (formState.isDefault) {
      // Default route only needs target
      return null;
    }

    // Non-default route: check required fields
    if (!formState.outputField) {
      return 'Select an output field.';
    }

    if (!formState.expectedValue) {
      return 'Enter an expected value.';
    }

    if (!formState.priority) {
      return 'Priority must be a non-negative integer.';
    }

    // Priority validation
    const priority = Number(formState.priority);
    if (isNaN(priority) || !Number.isFinite(priority) || priority < 0 || !Number.isInteger(priority)) {
      return 'Priority must be a non-negative integer.';
    }

    // Check for empty list items if operator is 'in'
    if (formState.operator === 'in') {
      const trimmed = formState.expectedValue.trim();
      let content = trimmed;
      if ((content.startsWith('[') && content.endsWith(']')) ||
          (content.startsWith('(') && content.endsWith(')'))) {
        content = content.slice(1, -1).trim();
      }
      const items = content.split(',').map((item) => item.trim());
      if (items.some((item) => item === '')) {
        return 'List values must not be empty.';
      }
    }

    // Validate the generated condition preview with existing validator
    const preview = this.buildAddRouteCondition();
    const validationError = validateSquadRoutingCondition(preview);
    if (validationError !== null) {
      return validationError;
    }

    return null;
  });

  readonly addRouteFormValid = computed(() => {
    return this.addRouteValidationMessage() === null;
  });

  constructor() {
    // Reset form when selectedConditionalId changes
    effect(() => {
      this.selectedConditionalId();
      this.resetAddRouteForm();
    });

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
    this.persistedSquadId.set(this.readRouteSquadId());
    this.loadExistingSquadFromRouteIfNeeded();
  }

  getSquadTypeLabel(type: SquadType): string {
    return getSquadTypeLabel(type);
  }

  addStep(): void {
    this.squadBuilderState.addStep();
  }

  addConditional(): void {
    const selectedStep = this.selectedStep();

    if (!selectedStep) {
      return;
    }

    this.squadBuilderState.addConditional(selectedStep.id);
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

  addSelectedStepManualInputRef(): void {
    this.squadBuilderState.addSelectedStepManualInputRef();
  }

  updateSelectedStepInputRefTargetInput(index: number, targetInput: string): void {
    this.squadBuilderState.updateSelectedStepInputRef(index, {
      targetInput,
    });
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

  selectConditional(conditionalId: string): void {
    this.squadBuilderState.selectConditional(conditionalId);
  }

  updateSelectedConditionalName(name: string): void {
    this.squadBuilderState.updateSelectedConditional({
      name: name.trim(),
    });
  }

  deleteSelectedConditional(): void {
    this.squadBuilderState.deleteSelectedConditional();
  }

  addConditionalRoute(): void {
    this.addRouteFormVisible.set(true);
  }

  cancelAddRoute(): void {
    this.resetAddRouteForm();
  }

  resetAddRouteForm(): void {
    this.addRouteFormVisible.set(false);
    this.editingEdgeId.set(null);
    this.addRouteFormState.set({
      targetStepId: '',
      outputField: '',
      operator: 'equals',
      expectedValue: '',
      priority: '',
      isDefault: false,
    });
  }

  editRoute(edgeId: string): void {
    const route = this.selectedConditionalRoutes().find((r) => r.id === edgeId);
    if (!route) {
      return;
    }

    this.editingEdgeId.set(edgeId);
    this.populateAddRouteFormFromEdge(route);
    this.addRouteFormVisible.set(true);
  }

  private populateAddRouteFormFromEdge(edge: SquadBuilderEdge): void {
    const formState: AddRouteFormState = {
      targetStepId: edge.targetStepId,
      outputField: '',
      operator: 'equals',
      expectedValue: '',
      priority: edge.priority.toString(),
      isDefault: edge.isDefault,
    };

    // Parse condition for WHEN routes if it matches the controlled format
    if (edge.routingType === 'WHEN' && edge.condition) {
      const parsed = this.parseAddRouteCondition(edge.condition);
      if (parsed) {
        formState.outputField = parsed.outputField;
        formState.operator = parsed.operator;
        formState.expectedValue = parsed.expectedValue;
      }
    }

    this.addRouteFormState.set(formState);
  }

  private parseAddRouteCondition(condition: string): {
    outputField: string;
    operator: AddRouteOperator;
    expectedValue: string;
  } | null {
    const trimmed = condition.trim();

    // Try to parse "output.<field> <operator> <value>" format
    const scalarMatch = trimmed.match(/^output\.(\w+)\s+(equals|notEquals|contains)\s+(.+)$/);
    if (scalarMatch) {
      return {
        outputField: scalarMatch[1],
        operator: scalarMatch[2] as AddRouteOperator,
        expectedValue: scalarMatch[3],
      };
    }

    // Try to parse "output.<field> in <list>" format
    const inMatch = trimmed.match(/^output\.(\w+)\s+in\s+(.+)$/);
    if (inMatch) {
      return {
        outputField: inMatch[1],
        operator: 'in',
        expectedValue: inMatch[2],
      };
    }

    return null;
  }

  submitAddRoute(): void {
    // Return if form is invalid
    if (!this.addRouteFormValid()) {
      return;
    }

    // Get selected conditional
    const selectedConditional = this.selectedConditional();
    if (!selectedConditional) {
      return;
    }

    const formState = this.addRouteFormState();
    const targetStepId = formState.targetStepId;
    const editingEdgeId = this.editingEdgeId();

    // Prepare the payload
    let payload;
    if (formState.isDefault) {
      payload = {
        routingType: 'ALWAYS' as const,
        condition: null,
        priority: 100,
        isDefault: true,
        targetStepId,
      };
    } else {
      payload = {
        routingType: 'WHEN' as const,
        condition: this.buildAddRouteCondition(),
        priority: Number(formState.priority),
        isDefault: false,
        targetStepId,
      };
    }

    // Handle edit mode
    if (editingEdgeId) {
      const updatedEdge = this.squadBuilderState.updateConditionalRoute(editingEdgeId, payload as UpdateConditionalRoutePayload);
      if (updatedEdge) {
        this.resetAddRouteForm();
      }
      return;
    }

    // Handle add mode
    const createdEdge = this.squadBuilderState.addConditionalRoute({
      conditionalId: selectedConditional.id,
      ...payload,
      targetStepId,
    } as AddConditionalRoutePayload);

    // Only reset and close form if creation succeeded
    if (createdEdge) {
      this.resetAddRouteForm();
    }
  }

  updateAddRouteFormField<K extends keyof AddRouteFormState>(
    field: K,
    value: AddRouteFormState[K]
  ): void {
    const currentState = this.addRouteFormState();
    const newState = {
      ...currentState,
      [field]: value,
    };

    // Handle side effects when isDefault changes
    if (field === 'isDefault') {
      if (value === true) {
        // When becoming default: clear condition fields, set priority to 100
        newState.outputField = '';
        newState.operator = 'equals';
        newState.expectedValue = '';
        newState.priority = '100';
      } else {
        // When becoming non-default: clear priority
        newState.priority = '';
      }
    }

    this.addRouteFormState.set(newState as AddRouteFormState);
  }

  // Pure normalization helpers
  normalizeRouteScalar(rawValue: string): string {
    const trimmed = rawValue.trim();

    if (!trimmed) {
      return '';
    }

    // Preserve already quoted values
    if ((trimmed.startsWith('"') && trimmed.endsWith('"')) ||
        (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
      return trimmed;
    }

    // Preserve true, false, null
    if (trimmed === 'true' || trimmed === 'false' || trimmed === 'null') {
      return trimmed;
    }

    // Preserve numbers (integers and decimals, including negative)
    if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
      return trimmed;
    }

    // Check if it's a single token (no whitespace)
    if (!/\s/.test(trimmed)) {
      return trimmed;
    }

    // Auto-quote strings with whitespace, escaping backslashes and quotes
    const escaped = trimmed
      .replace(/\\/g, '\\\\')
      .replace(/"/g, '\\"');
    return `"${escaped}"`;
  }

  normalizeRouteList(rawValue: string): string {
    const trimmed = rawValue.trim();

    if (!trimmed) {
      return '[]';
    }

    // Remove outer bracket pair if present
    let content = trimmed;
    if ((content.startsWith('[') && content.endsWith(']')) ||
        (content.startsWith('(') && content.endsWith(')'))) {
      content = content.slice(1, -1).trim();
    }

    // Split by comma and normalize each item
    const items = content
      .split(',')
      .map((item) => item.trim())
      .filter((item) => item !== '');

    if (items.length === 0) {
      return '[]';
    }

    const normalizedItems = items.map((item) => this.normalizeRouteScalar(item));

    return `[${normalizedItems.join(', ')}]`;
  }

  buildAddRouteCondition(): string {
    const formState = this.addRouteFormState();

    if (formState.isDefault) {
      return 'Default route';
    }

    if (!formState.outputField || !formState.operator || !formState.expectedValue) {
      return '';
    }

    const normalizedValue = this.normalizeRouteScalar(formState.expectedValue);

    if (formState.operator === 'in') {
      const normalizedList = this.normalizeRouteList(formState.expectedValue);
      return `output.${formState.outputField} in ${normalizedList}`;
    }

    return `output.${formState.outputField} ${formState.operator} ${normalizedValue}`;
  }

  getAddRouteExpectedValuePlaceholder(): string {
    const operator = this.addRouteFormState().operator;
    return operator === 'in'
      ? 'e.g., BUG_FIX, HOTFIX'
      : 'e.g., BUG_FIX or urgent production fix';
  }

  deleteRoute(edgeId: string): void {
    this.deleteRouteConfirmId.set(edgeId);
  }

  confirmDeleteRoute(): void {
    const edgeId = this.deleteRouteConfirmId();
    if (!edgeId) {
      return;
    }

    const removed = this.squadBuilderState.removeConditionalRoute(edgeId);
    if (removed) {
      this.deleteRouteConfirmId.set(null);
    }
  }

  cancelDeleteRoute(): void {
    this.deleteRouteConfirmId.set(null);
  }

  deleteConditional(): void {
    this.deleteConditionalConfirmVisible.set(true);
  }

  confirmDeleteConditional(): void {
    this.deleteSelectedConditional();
    this.deleteConditionalConfirmVisible.set(false);
  }

  cancelDeleteConditional(): void {
    this.deleteConditionalConfirmVisible.set(false);
  }

  handleConditionalRouteRequested(event: {
    conditionalId: string;
    targetStepId: string;
  }): void {
    // Find and select the conditional
    const conditional = this.conditionals().find((c) => c.id === event.conditionalId);
    if (!conditional) {
      return;
    }

    // Check if target is invalid (not an executable step)
    const targetStep = this.steps().find((s) => s.id === event.targetStepId);
    if (!targetStep) {
      return;
    }

    // Check if target is already used by a persisted route from this conditional
    const targetAlreadyUsed = this.selectedConditionalRoutes().some(
      (route) => route.targetStepId === event.targetStepId
    );
    if (targetAlreadyUsed) {
      return;
    }

    // Select the conditional
    this.squadBuilderState.selectConditional(event.conditionalId);

    // Open the Add Route form with the target preselected
    this.addRouteFormVisible.set(true);
    this.editingEdgeId.set(null);
    this.addRouteFormState.set({
      targetStepId: event.targetStepId,
      outputField: '',
      operator: 'equals',
      expectedValue: '',
      priority: '',
      isDefault: false,
    });
  }

  getRouteTargetName(targetStepId: string): string {
    return this.steps().find((step) => step.id === targetStepId)?.name ?? 'Unknown step';
  }

  getConditionalSourceAgent(): string | null {
    const sourceStep = this.selectedConditionalSourceStep();
    if (!sourceStep?.assignedAgentId) {
      return null;
    }
    return this.getAgentName(sourceStep.assignedAgentId);
  }

  getConditionalSourceOutputs(): string[] {
    const sourceStep = this.selectedConditionalSourceStep();
    if (!sourceStep?.assignedAgentId) {
      return [];
    }
    return this.getAgentByKey(sourceStep.assignedAgentId)?.outputs ?? [];
  }

  handleReteConditionalPositionChanged(event: {
    conditionalId: string;
    position: {
      x: number;
      y: number;
    };
  }): void {
    this.squadBuilderState.updateConditionalPosition(event.conditionalId, event.position);
  }

  getStepName(stepId: string): string {
    return this.steps().find((step) => step.id === stepId)?.name ?? 'Unknown step';
  }

  trackInputRef(index: number, inputRef: SquadBuilderInputRef): string {
    return `${index}-${inputRef.targetInput}-${inputRef.fromStepId}-${inputRef.key}`;
  }

  getAvailableTargetInputsForInputRef(index: number): string[] {
    const selectedStep = this.selectedStep();

    if (!selectedStep?.assignedAgentId) {
      return [];
    }

    const agent = this.getAgentByKey(selectedStep.assignedAgentId);
    if (!agent?.inputs.length) {
      return [];
    }

    const currentTargetInput = selectedStep.inputRefs[index]?.targetInput;
    const otherMappedTargetInputs = new Set(
      selectedStep.inputRefs
        .filter((_, inputRefIndex) => inputRefIndex !== index)
        .map((inputRef) => inputRef.targetInput)
        .filter((targetInput): targetInput is string => Boolean(targetInput)),
    );

    return agent.inputs.filter(
      (targetInput) =>
        targetInput === currentTargetInput || !otherMappedTargetInputs.has(targetInput),
    );
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

          this.isSaving.set(false);

          const successMessage = `Squad "${updatedSquad.name}" was updated successfully.`;
          this.saveSuccess.set(successMessage);
          this.showSaveSuccess(successMessage);
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
        this.persistedSquadId.set(createdSquad.id);

        this.isSaving.set(false);

        const successMessage = `Squad "${createdSquad.name}" was created successfully.`;
        this.saveSuccess.set(successMessage);
        this.showSaveSuccess(successMessage);

        void this.router.navigate(['/squads/builder', createdSquad.id], {
          replaceUrl: true,
          state: {
            preserveBuilderDraft: true,
          },
        });
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

    const navigationState = window.history.state as {
      preserveBuilderDraft?: boolean;
    };

    if (navigationState.preserveBuilderDraft && this.draft()) {
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

  private showSaveSuccess(message: string): void {
    this.snackBar.open(`✓ ${message}`, 'Close', {
      duration: 4000,
      horizontalPosition: 'end',
      verticalPosition: 'bottom',
      politeness: 'polite',
      panelClass: ['squad-save-success-snackbar'],
    });
  }

  private readRouteSquadId(): string | null {
    const squadId = this.route.snapshot.paramMap.get('squadId');

    if (!squadId || squadId === 'new') {
      return null;
    }

    return squadId;
  }

  private getRouteSquadId(): string | null {
    return this.persistedSquadId() ?? this.readRouteSquadId();
  }
}
