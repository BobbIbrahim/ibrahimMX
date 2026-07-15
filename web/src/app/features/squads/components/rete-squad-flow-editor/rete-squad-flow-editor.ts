import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Injector,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
} from '@angular/core';

import { ClassicPreset, GetSchemes, NodeEditor } from 'rete';
import { AreaExtensions, AreaPlugin } from 'rete-area-plugin';
import { ConnectionPlugin, Presets as ConnectionPresets } from 'rete-connection-plugin';
import { getDOMSocketPosition } from 'rete-render-utils';
import { AngularArea2D, AngularPlugin, Presets as AngularPresets } from 'rete-angular-plugin/21';

import { SquadExecutionStatus } from '../../../../core/models/squad-run.model';

type StepExecutionState = SquadExecutionStatus['steps'][number]['status'];

interface ReteFlowStep {
  id: string;
  name: string;
  assignedAgentId: string | null;
  position: {
    x: number;
    y: number;
  };
}

interface ReteFlowEdge {
  id: string;
  sourceStepId: string;
  targetStepId: string;
}

interface ReteConnectionCreatedEvent {
  sourceStepId: string;
  targetStepId: string;
}

interface ReteConnectionRemovedEvent {
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

type ReteNode = ClassicPreset.Node;

type ReteConnection = ClassicPreset.Connection<ReteNode, ReteNode>;

type ReteSchemes = GetSchemes<ReteNode, ReteConnection>;

type AreaExtra = AngularArea2D<ReteSchemes>;

type EdgeAnimationState = 'pending' | 'running' | 'completed';

interface ReteConnectionView {
  element: HTMLElement;
}

interface AreaWithConnectionViews {
  connectionViews?: Map<string, ReteConnectionView>;
}

const NODE_WIDTH = 210;
const NODE_HEIGHT = 92;

@Component({
  selector: 'app-rete-squad-flow-editor',
  imports: [],
  templateUrl: './rete-squad-flow-editor.html',
  styleUrl: './rete-squad-flow-editor.scss',
})
export class ReteSquadFlowEditor implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) steps: ReteFlowStep[] = [];
  @Input({ required: true }) edges: ReteFlowEdge[] = [];
  @Input() agentNamesById: Record<string, string> = {};
  @Input() executionStatus: SquadExecutionStatus | null = null;
  @Input() workflowCancelled = false;
  @Input() followModeEnabled = true;
  @Input() interactionLocked = false;
  @Input() selectedStepId: string | null = null;

  @Output() stepSelected = new EventEmitter<string>();
  @Output() connectionCreated = new EventEmitter<ReteConnectionCreatedEvent>();
  @Output() connectionRemoved = new EventEmitter<ReteConnectionRemovedEvent>();
  @Output() nodePositionChanged = new EventEmitter<ReteNodePositionChangedEvent>();

  @ViewChild('reteContainer', { static: true })
  private readonly reteContainer!: ElementRef<HTMLElement>;

  private readonly injector = inject(Injector);

  private editor: NodeEditor<ReteSchemes> | null = null;
  private area: AreaPlugin<ReteSchemes, AreaExtra> | null = null;

  private editorReady = false;
  private isSyncingFromAngularState = false;

  private connectionArrowObserver: MutationObserver | null = null;
  private readonly connectionArrowMarkerId = 'mxorbit-rete-edge-arrow';

  private readonly socket = new ClassicPreset.Socket('Agent Flow');

  private readonly nodeByStepId = new Map<string, ReteNode>();
  private readonly stepIdByNodeId = new Map<string, string>();
  private readonly connectionIdByEdgeKey = new Map<string, string>();
  private readonly ignoredConnectionRemovalIds = new Set<string>();
  private readonly nodeStatusByStepId = new Map<string, StepExecutionState>();
  private readonly edgeAnimationStateByEdgeKey = new Map<string, EdgeAnimationState>();
  private readonly runningStepStartedAtByStepId = new Map<string, number>();
  private runningStepStartSequence = 0;
  private latestRunningStepId: string | null = null;
  private lastFollowedStepId: string | null = null;
  private followAnimationFrameId: number | null = null;
  private readonly handleContainerPointerDown = (event: PointerEvent): void => {
    if (!this.interactionLocked) {
      return;
    }

    const selectedStepId = this.resolveStepIdFromDomEventTarget(event.target);

    if (!selectedStepId) {
      return;
    }

    event.stopPropagation();
  };
  private readonly handleContainerClick = (event: MouseEvent): void => {
    if (!this.interactionLocked) {
      return;
    }

    const selectedStepId = this.resolveStepIdFromDomEventTarget(event.target);

    if (!selectedStepId) {
      return;
    }

    event.stopPropagation();
    this.stepSelected.emit(selectedStepId);
  };

  async ngAfterViewInit(): Promise<void> {
    await this.initializeEditor();

    this.editorReady = true;

    this.applyInteractionLockMode();
    await this.syncGraphFromInputs();
    this.applyNodeStatuses();
    this.scheduleFollowToActiveNode();
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    if (changes['executionStatus']) {
      this.handleExecutionStatusChange();
    }

    if (changes['followModeEnabled']) {
      if (this.followModeEnabled) {
        this.lastFollowedStepId = null;
        this.scheduleFollowToActiveNode();
      } else if (this.followAnimationFrameId !== null) {
        cancelAnimationFrame(this.followAnimationFrameId);
        this.followAnimationFrameId = null;
      }
    }

    if (changes['interactionLocked']) {
      this.applyInteractionLockMode();
    }

    if (changes['selectedStepId']) {
      requestAnimationFrame(() => {
        this.applyNodeStatuses();
      });
    }

    if (!this.editorReady) {
      return;
    }

    if (changes['steps'] || changes['edges'] || changes['agentNamesById']) {
      await this.syncGraphFromInputs();
      this.scheduleFollowToActiveNode();
    }
    if (changes['workflowCancelled']) {
      this.updateEdgeAnimationStates();
      this.applyNodeStatuses();

      requestAnimationFrame(() => {
        this.applyConnectionAnimationStates();
      });
    }
  }

  ngOnDestroy(): void {
    if (this.followAnimationFrameId !== null) {
      cancelAnimationFrame(this.followAnimationFrameId);
      this.followAnimationFrameId = null;
    }

    this.connectionArrowObserver?.disconnect();
    this.connectionArrowObserver = null;

    this.reteContainer.nativeElement.removeEventListener(
      'pointerdown',
      this.handleContainerPointerDown,
      true,
    );
    this.reteContainer.nativeElement.removeEventListener('click', this.handleContainerClick, true);

    this.area?.destroy();

    this.editor = null;
    this.area = null;

    this.nodeByStepId.clear();
    this.stepIdByNodeId.clear();
    this.connectionIdByEdgeKey.clear();
    this.ignoredConnectionRemovalIds.clear();
  }

  private async initializeEditor(): Promise<void> {
    const container = this.reteContainer.nativeElement;

    const editor = new NodeEditor<ReteSchemes>();
    const area = new AreaPlugin<ReteSchemes, AreaExtra>(container);
    const connection = new ConnectionPlugin<ReteSchemes, AreaExtra>();
    const render = new AngularPlugin<ReteSchemes, AreaExtra>({
      injector: this.injector,
    });

    render.addPreset(
      AngularPresets.classic.setup({
        socketPositionWatcher: getDOMSocketPosition({
          offset(position, _nodeId, side) {
            return {
              x: position.x + (side === 'input' ? -8 : 8),
              y: position.y,
            };
          },
        }),
      }),
    );
    connection.addPreset(ConnectionPresets.classic.setup());

    editor.use(area);
    area.use(connection);
    area.use(render);

    AreaExtensions.selectableNodes(area, AreaExtensions.selector(), {
      accumulating: AreaExtensions.accumulateOnCtrl(),
    });

    AreaExtensions.simpleNodesOrder(area);

    editor.addPipe((context) => {
      if (context.type === 'connectioncreated' && !this.isSyncingFromAngularState) {
        if (this.interactionLocked) {
          const connectionData = context.data as ReteSchemes['Connection'];
          void this.editor?.removeConnection(connectionData.id);
          return context;
        }

        const connectionData = context.data as ReteSchemes['Connection'];

        const sourceStepId = this.stepIdByNodeId.get(connectionData.source);
        const targetStepId = this.stepIdByNodeId.get(connectionData.target);

        if (!sourceStepId || !targetStepId) {
          return context;
        }

        const edgeKey = this.buildEdgeKey(sourceStepId, targetStepId);
        const existingConnectionId = this.connectionIdByEdgeKey.get(edgeKey);

        if (existingConnectionId && existingConnectionId !== connectionData.id) {
          this.ignoredConnectionRemovalIds.add(connectionData.id);
          void this.editor?.removeConnection(connectionData.id);

          return context;
        }

        this.connectionIdByEdgeKey.set(edgeKey, connectionData.id);

        this.connectionCreated.emit({
          sourceStepId,
          targetStepId,
        });

        requestAnimationFrame(() => {
          this.applyConnectionArrows();
          this.applyConnectionAnimationStates();
        });
      }

      if (context.type === 'connectionremoved' && !this.isSyncingFromAngularState) {
        if (this.interactionLocked) {
          return context;
        }

        const connectionData = context.data as ReteSchemes['Connection'];

        if (this.ignoredConnectionRemovalIds.has(connectionData.id)) {
          this.ignoredConnectionRemovalIds.delete(connectionData.id);

          return context;
        }

        const sourceStepId = this.stepIdByNodeId.get(connectionData.source);
        const targetStepId = this.stepIdByNodeId.get(connectionData.target);

        if (!sourceStepId || !targetStepId) {
          return context;
        }

        const edgeKey = this.buildEdgeKey(sourceStepId, targetStepId);

        this.connectionIdByEdgeKey.delete(edgeKey);

        this.connectionRemoved.emit({
          sourceStepId,
          targetStepId,
        });
      }

      return context;
    });

    area.addPipe((context) => {
      const areaContext = context as {
        type: string;
        data: unknown;
      };

      if (areaContext.type === 'nodepicked') {
        const nodeData = areaContext.data as { id?: string };

        if (!nodeData.id) {
          return context;
        }

        const stepId = this.stepIdByNodeId.get(nodeData.id);

        if (stepId) {
          this.stepSelected.emit(stepId);
        }
      }

      if (areaContext.type === 'nodetranslated' && !this.isSyncingFromAngularState) {
        if (this.interactionLocked) {
          const nodeData = areaContext.data as {
            id?: string;
            position?: {
              x: number;
              y: number;
            };
          };

          if (!nodeData.id) {
            return context;
          }

          const stepId = this.stepIdByNodeId.get(nodeData.id);
          const step = this.steps.find((candidate) => candidate.id === stepId);

          if (!step || !this.area) {
            return context;
          }

          const currentX = nodeData.position?.x ?? 0;
          const currentY = nodeData.position?.y ?? 0;
          const deltaX = Math.abs(currentX - step.position.x);
          const deltaY = Math.abs(currentY - step.position.y);

          if (deltaX < 0.5 && deltaY < 0.5) {
            return context;
          }

          this.isSyncingFromAngularState = true;
          void this.area
            .translate(nodeData.id, {
              x: step.position.x,
              y: step.position.y,
            })
            .finally(() => {
              this.isSyncingFromAngularState = false;
            });

          return context;
        }

        const nodeData = areaContext.data as {
          id?: string;
          position?: {
            x: number;
            y: number;
          };
        };

        if (!nodeData.id || !nodeData.position) {
          return context;
        }

        const stepId = this.stepIdByNodeId.get(nodeData.id);

        if (stepId) {
          this.nodePositionChanged.emit({
            stepId,
            position: {
              x: nodeData.position.x,
              y: nodeData.position.y,
            },
          });
        }
      }

      if (areaContext.type === 'translated') {
        const translateData = areaContext.data as {
          position?: {
            x: number;
            y: number;
          };
          x?: number;
          y?: number;
        };

        const x = translateData.position?.x ?? translateData.x ?? 0;
        const y = translateData.position?.y ?? translateData.y ?? 0;

        this.updateGridPosition(x, y);
      }

      if (areaContext.type === 'zoomed') {
        const zoomData = areaContext.data as {
          zoom?: number;
          k?: number;
          scale?: number;
        };

        const zoom = zoomData.zoom ?? zoomData.k ?? zoomData.scale ?? 1;

        this.updateGridScale(zoom);
      }

      return context;
    });

    this.editor = editor;
    this.area = area;

    container.addEventListener('pointerdown', this.handleContainerPointerDown, true);
    container.addEventListener('click', this.handleContainerClick, true);

    this.startConnectionArrowObserver(container);
  }

  private async syncGraphFromInputs(): Promise<void> {
    if (!this.editor || !this.area) {
      return;
    }

    this.isSyncingFromAngularState = true;

    try {
      await this.removeDeletedConnections();
      await this.removeDeletedNodes();
      await this.addOrUpdateNodes();
      await this.addMissingConnections();
      this.updateEdgeAnimationStates();
      this.applyNodeStatuses();
      this.applyConnectionAnimationStates();
    } finally {
      this.isSyncingFromAngularState = false;
    }
  }

  private handleExecutionStatusChange(): void {
    const previousStatuses = new Map(this.nodeStatusByStepId);
    const currentlyRunningStepIds = new Set<string>();

    this.nodeStatusByStepId.clear();

    const status = this.executionStatus;

    if (status) {
      status.steps.forEach((step) => {
        this.nodeStatusByStepId.set(step.stepId, step.status);

        if (step.status === 'RUNNING') {
          currentlyRunningStepIds.add(step.stepId);

          if (previousStatuses.get(step.stepId) !== 'RUNNING') {
            this.runningStepStartSequence += 1;
            this.runningStepStartedAtByStepId.set(step.stepId, this.runningStepStartSequence);
          } else if (!this.runningStepStartedAtByStepId.has(step.stepId)) {
            this.runningStepStartSequence += 1;
            this.runningStepStartedAtByStepId.set(step.stepId, this.runningStepStartSequence);
          }
        } else {
          this.runningStepStartedAtByStepId.delete(step.stepId);
        }
      });
    }

    for (const stepId of this.runningStepStartedAtByStepId.keys()) {
      if (!currentlyRunningStepIds.has(stepId)) {
        this.runningStepStartedAtByStepId.delete(stepId);
      }
    }

    this.latestRunningStepId = this.resolveLatestRunningStepId(Array.from(currentlyRunningStepIds));
    this.updateEdgeAnimationStates();

    requestAnimationFrame(() => {
      this.applyNodeStatuses();
      this.applyConnectionAnimationStates();
      this.scheduleFollowToActiveNode();
    });
  }

  private resolveLatestRunningStepId(currentlyRunningStepIds: string[]): string | null {
    if (currentlyRunningStepIds.length === 0) {
      return null;
    }

    let latestStepId = currentlyRunningStepIds[0];
    let latestStartedAt = this.runningStepStartedAtByStepId.get(latestStepId) ?? -1;

    for (const stepId of currentlyRunningStepIds) {
      const startedAt = this.runningStepStartedAtByStepId.get(stepId) ?? -1;

      if (startedAt >= latestStartedAt) {
        latestStepId = stepId;
        latestStartedAt = startedAt;
      }
    }

    return latestStepId;
  }

  private async addOrUpdateNodes(): Promise<void> {
    if (!this.editor || !this.area) {
      return;
    }

    for (const step of this.steps) {
      const existingNode = this.nodeByStepId.get(step.id);

      if (existingNode) {
        existingNode.label = this.buildNodeLabel(step);
        this.applyNodeLayout(existingNode);

        await this.area.update('node', existingNode.id);

        continue;
      }

      const node = new ClassicPreset.Node(this.buildNodeLabel(step));

      this.applyNodeLayout(node);

      node.addInput('previous', new ClassicPreset.Input(this.socket, 'In', true));
      node.addOutput('next', new ClassicPreset.Output(this.socket, 'Out', true));

      await this.editor.addNode(node);

      await this.area.translate(node.id, {
        x: step.position.x,
        y: step.position.y,
      });

      this.nodeByStepId.set(step.id, node);
      this.stepIdByNodeId.set(node.id, step.id);
    }
  }

  private async addMissingConnections(): Promise<void> {
    if (!this.editor) {
      return;
    }

    for (const edge of this.edges) {
      const edgeKey = this.buildEdgeKey(edge.sourceStepId, edge.targetStepId);

      if (this.connectionIdByEdgeKey.has(edgeKey)) {
        continue;
      }

      const sourceNode = this.nodeByStepId.get(edge.sourceStepId);
      const targetNode = this.nodeByStepId.get(edge.targetStepId);

      if (!sourceNode || !targetNode) {
        continue;
      }

      const connection = new ClassicPreset.Connection(sourceNode, 'next', targetNode, 'previous');

      await this.editor.addConnection(connection);

      requestAnimationFrame(() => {
        this.applyConnectionArrows();
        this.applyConnectionAnimationStates();
      });

      this.connectionIdByEdgeKey.set(edgeKey, connection.id);
    }
  }

  private async removeDeletedNodes(): Promise<void> {
    if (!this.editor) {
      return;
    }

    const currentStepIds = new Set(this.steps.map((step) => step.id));

    for (const [stepId, node] of this.nodeByStepId.entries()) {
      if (currentStepIds.has(stepId)) {
        continue;
      }

      await this.editor.removeNode(node.id);

      this.nodeByStepId.delete(stepId);
      this.stepIdByNodeId.delete(node.id);
    }
  }

  private async removeDeletedConnections(): Promise<void> {
    if (!this.editor) {
      return;
    }

    const currentEdgeKeys = new Set(
      this.edges.map((edge) => this.buildEdgeKey(edge.sourceStepId, edge.targetStepId)),
    );

    for (const [edgeKey, connectionId] of this.connectionIdByEdgeKey.entries()) {
      if (currentEdgeKeys.has(edgeKey)) {
        continue;
      }

      await this.editor.removeConnection(connectionId);

      this.connectionIdByEdgeKey.delete(edgeKey);
    }
  }

  private applyNodeLayout(node: ReteNode): void {
    const sizedNode = node as ReteNode & {
      width?: number;
      height?: number;
    };

    sizedNode.width = NODE_WIDTH;
    sizedNode.height = NODE_HEIGHT;
  }

  private updateGridPosition(x: number, y: number): void {
    const container = this.reteContainer.nativeElement;

    container.style.setProperty('--rete-grid-x', `${x}px`);
    container.style.setProperty('--rete-grid-y', `${y}px`);
  }

  private updateGridScale(zoom: number): void {
    const container = this.reteContainer.nativeElement;
    const baseGridSize = 28;
    const scaledGridSize = Math.max(16, Math.min(64, baseGridSize * zoom));

    container.style.setProperty('--rete-grid-size', `${scaledGridSize}px`);
  }

  private startConnectionArrowObserver(container: HTMLElement): void {
    this.connectionArrowObserver?.disconnect();

    this.connectionArrowObserver = new MutationObserver(() => {
      this.applyConnectionArrows();
      this.applyConnectionAnimationStates();
    });

    this.connectionArrowObserver.observe(container, {
      childList: true,
      subtree: true,
    });

    requestAnimationFrame(() => {
      this.applyConnectionArrows();
      this.applyConnectionAnimationStates();
    });
  }

  private applyConnectionArrows(): void {
    const container = this.reteContainer.nativeElement;
    const roots = this.collectRenderableRoots(container);

    roots.forEach((root) => {
      const svgs = root.querySelectorAll<SVGSVGElement>('svg');

      svgs.forEach((svg) => {
        this.ensureArrowMarker(svg);
        this.applyMarkerToConnectionPaths(svg);
      });
    });
  }

  private collectRenderableRoots(root: HTMLElement | ShadowRoot): Array<HTMLElement | ShadowRoot> {
    const roots: Array<HTMLElement | ShadowRoot> = [root];
    const elements = root.querySelectorAll<HTMLElement>('*');

    elements.forEach((element) => {
      if (element.shadowRoot) {
        roots.push(element.shadowRoot);
        roots.push(...this.collectRenderableRoots(element.shadowRoot));
      }
    });

    return roots;
  }

  private ensureArrowMarker(svg: SVGSVGElement): void {
    const existingMarker = svg.querySelector(`#${this.connectionArrowMarkerId}`);

    if (existingMarker) {
      return;
    }

    const svgNamespace = 'http://www.w3.org/2000/svg';

    let defs = svg.querySelector('defs');

    if (!defs) {
      defs = document.createElementNS(svgNamespace, 'defs');
      svg.prepend(defs);
    }

    const marker = document.createElementNS(svgNamespace, 'marker');

    marker.setAttribute('id', this.connectionArrowMarkerId);
    marker.setAttribute('markerWidth', '12');
    marker.setAttribute('markerHeight', '12');
    marker.setAttribute('refX', '9');
    marker.setAttribute('refY', '6');
    marker.setAttribute('orient', 'auto');
    marker.setAttribute('markerUnits', 'userSpaceOnUse');

    const arrowPath = document.createElementNS(svgNamespace, 'path');

    arrowPath.setAttribute('d', 'M 2 2 L 10 6 L 2 10 z');
    arrowPath.setAttribute('fill', 'context-stroke');

    marker.appendChild(arrowPath);
    defs.appendChild(marker);
  }

  private applyMarkerToConnectionPaths(svg: SVGSVGElement): void {
    const paths = svg.querySelectorAll<SVGPathElement>('path');

    paths.forEach((path) => {
      if (path.closest('marker')) {
        return;
      }

      path.setAttribute('marker-end', `url(#${this.connectionArrowMarkerId})`);
      path.setAttribute('stroke-linecap', 'round');
      path.setAttribute('stroke-linejoin', 'round');
      path.classList.add('edge-path');
    });
  }

  private updateEdgeAnimationStates(): void {
    this.edgeAnimationStateByEdgeKey.clear();

    for (const edge of this.edges) {
      const edgeKey = this.buildEdgeKey(edge.sourceStepId, edge.targetStepId);

      const sourceStatus = this.nodeStatusByStepId.get(edge.sourceStepId);
      const targetStatus = this.nodeStatusByStepId.get(edge.targetStepId);

      if (this.workflowCancelled) {
        if (
          sourceStatus === 'COMPLETED' &&
          (targetStatus === 'COMPLETED' || targetStatus === 'CANCELLED')
        ) {
          this.edgeAnimationStateByEdgeKey.set(edgeKey, 'completed');
        } else {
          this.edgeAnimationStateByEdgeKey.set(edgeKey, 'pending');
        }
        continue;
      }

      if (sourceStatus === 'RUNNING' && targetStatus !== 'COMPLETED') {
        this.edgeAnimationStateByEdgeKey.set(edgeKey, 'running');
        continue;
      }

      if (sourceStatus === 'COMPLETED' && targetStatus === 'COMPLETED') {
        this.edgeAnimationStateByEdgeKey.set(edgeKey, 'completed');
        continue;
      }

      this.edgeAnimationStateByEdgeKey.set(edgeKey, 'pending');
    }
  }

  private applyConnectionAnimationStates(): void {
    this.clearConnectionAnimationClasses();

    if (!this.area) {
      return;
    }

    for (const [edgeKey, connectionId] of this.connectionIdByEdgeKey.entries()) {
      const animationState = this.edgeAnimationStateByEdgeKey.get(edgeKey);

      if (!animationState || animationState === 'pending') {
        continue;
      }

      const connectionElement = this.findConnectionElement(connectionId);

      if (!connectionElement) {
        continue;
      }

      const connectionClass =
        animationState === 'running' ? 'edge-status--running' : 'edge-status--completed';
      const pathClass =
        animationState === 'running' ? 'edge-path--running' : 'edge-path--completed';

      connectionElement.classList.add(connectionClass);

      const paths = connectionElement.querySelectorAll<SVGPathElement>('path');

      paths.forEach((path) => {
        if (path.closest('marker')) {
          return;
        }

        path.classList.add(pathClass);
      });
    }
  }

  private clearConnectionAnimationClasses(): void {
    const container = this.reteContainer.nativeElement;
    const roots = this.collectRenderableRoots(container);

    roots.forEach((root) => {
      const connectionElements = root.querySelectorAll<HTMLElement>('[data-testid="connection"]');

      connectionElements.forEach((connectionElement) => {
        connectionElement.classList.remove('edge-status--running', 'edge-status--completed');
      });

      const edgePaths = root.querySelectorAll<SVGPathElement>('path.edge-path');

      edgePaths.forEach((path) => {
        path.classList.remove('edge-path--running', 'edge-path--completed');
      });
    });
  }

  private findConnectionElement(connectionId: string): HTMLElement | null {
    if (!this.area) {
      return null;
    }

    const areaWithConnectionViews = this.area as unknown as AreaWithConnectionViews;
    const viewElement = areaWithConnectionViews.connectionViews?.get(connectionId)?.element;

    if (viewElement) {
      return viewElement;
    }

    const container = this.reteContainer.nativeElement;
    const roots = this.collectRenderableRoots(container);

    for (const root of roots) {
      const byDataId = root.querySelector<HTMLElement>(
        `[data-testid="connection"][data-id="${connectionId}"]`,
      );

      if (byDataId) {
        return byDataId;
      }
    }

    return null;
  }

  private buildNodeLabel(step: ReteFlowStep): string {
    const stepName = step.name.trim() || 'Untitled Step';

    if (!step.assignedAgentId) {
      return `${stepName}\nUnassigned`;
    }

    const agentName = this.agentNamesById[step.assignedAgentId] ?? 'Unknown agent';

    return `${stepName}\n${agentName}`;
  }

  private buildEdgeKey(sourceStepId: string, targetStepId: string): string {
    return `${sourceStepId}->${targetStepId}`;
  }

  private scheduleFollowToActiveNode(): void {
    if (!this.followModeEnabled || !this.latestRunningStepId) {
      return;
    }

    const stepId = this.latestRunningStepId;

    if (this.lastFollowedStepId === stepId) {
      return;
    }

    this.lastFollowedStepId = stepId;

    requestAnimationFrame(() => {
      void this.followStep(stepId);
    });
  }

  private async followStep(stepId: string): Promise<void> {
    if (!this.area || !this.followModeEnabled) {
      return;
    }

    const node = this.nodeByStepId.get(stepId);

    if (!node) {
      return;
    }

    const nodeView = this.area.nodeViews.get(node.id);

    if (!nodeView) {
      return;
    }

    const viewportWidth = this.area.container.clientWidth;
    const viewportHeight = this.area.container.clientHeight;
    const transform = this.area.area.transform;
    const nodeWidth = nodeView.element.clientWidth || NODE_WIDTH;
    const nodeHeight = nodeView.element.clientHeight || NODE_HEIGHT;

    const nodeCenterX = nodeView.position.x + nodeWidth / 2;
    const nodeCenterY = nodeView.position.y + nodeHeight / 2;

    const targetX = viewportWidth / 2 - nodeCenterX * transform.k;
    const targetY = viewportHeight / 2 - nodeCenterY * transform.k;

    await this.animateAreaTranslation(targetX, targetY);
  }

  private async animateAreaTranslation(targetX: number, targetY: number): Promise<void> {
    if (!this.area) {
      return;
    }

    if (this.followAnimationFrameId !== null) {
      cancelAnimationFrame(this.followAnimationFrameId);
      this.followAnimationFrameId = null;
    }

    const area = this.area.area;
    const startX = area.transform.x;
    const startY = area.transform.y;
    const deltaX = targetX - startX;
    const deltaY = targetY - startY;

    if (Math.abs(deltaX) < 1 && Math.abs(deltaY) < 1) {
      await area.translate(targetX, targetY);
      return;
    }

    const durationMs = 420;
    const startedAt = performance.now();

    await new Promise<void>((resolve) => {
      const stepAnimation = async (timestamp: number) => {
        if (!this.area || !this.followModeEnabled) {
          this.followAnimationFrameId = null;
          resolve();
          return;
        }

        const progress = Math.min((timestamp - startedAt) / durationMs, 1);
        const easedProgress =
          progress < 0.5
            ? 4 * progress * progress * progress
            : 1 - Math.pow(-2 * progress + 2, 3) / 2;

        const nextX = startX + deltaX * easedProgress;
        const nextY = startY + deltaY * easedProgress;

        await area.translate(nextX, nextY);

        if (progress >= 1) {
          this.followAnimationFrameId = null;
          resolve();
          return;
        }

        this.followAnimationFrameId = requestAnimationFrame((frameTime) => {
          void stepAnimation(frameTime);
        });
      };

      this.followAnimationFrameId = requestAnimationFrame((frameTime) => {
        void stepAnimation(frameTime);
      });
    });
  }

  private applyNodeStatuses(): void {
    const container = this.reteContainer.nativeElement;

    const nodeElements = container.querySelectorAll<HTMLElement>('[data-testid="node"]');

    nodeElements.forEach((nodeElement) => {
      nodeElement.classList.remove(
        'node-running',
        'node-completed',
        'node-failed',
        'node-cancelled',
        'node-selected-runtime',
      );
    });

    let index = 0;

    for (const step of this.steps) {
      const nodeElement = nodeElements[index];

      if (!nodeElement) {
        index++;
        continue;
      }

      const status = this.nodeStatusByStepId.get(step.id);

      if (status === 'RUNNING') {
        if (this.workflowCancelled) {
          nodeElement.classList.add('node-cancelled');
        } else {
          nodeElement.classList.add('node-running');
        }
      }

      if (status === 'COMPLETED') {
        nodeElement.classList.add('node-completed');
      }

      if (status === 'FAILED') {
        nodeElement.classList.add('node-failed');
      }

      if (status === 'CANCELLED') {
        nodeElement.classList.add('node-cancelled');
      }

      if (step.id === this.selectedStepId) {
        nodeElement.classList.add('node-selected-runtime');
      }

      index++;
    }
  }

  private applyInteractionLockMode(): void {
    const container = this.reteContainer?.nativeElement;

    if (!container) {
      return;
    }

    container.classList.toggle('rete-squad-flow-editor--interaction-locked', this.interactionLocked);
  }

  private resolveStepIdFromDomEventTarget(target: EventTarget | null): string | null {
    if (!(target instanceof HTMLElement)) {
      return null;
    }

    const nodeElement = target.closest<HTMLElement>('[data-testid="node"]');

    if (!nodeElement) {
      return null;
    }

    const explicitNodeId = nodeElement.getAttribute('data-id');

    if (explicitNodeId) {
      const stepId = this.stepIdByNodeId.get(explicitNodeId);

      if (stepId) {
        return stepId;
      }
    }

    if (!this.area) {
      return null;
    }

    for (const [stepId, node] of this.nodeByStepId.entries()) {
      const nodeView = this.area.nodeViews.get(node.id);

      if (!nodeView) {
        continue;
      }

      if (nodeView.element === nodeElement) {
        return stepId;
      }
    }

    return null;
  }
}
