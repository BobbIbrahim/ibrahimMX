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
import {
  AngularArea2D,
  AngularPlugin,
  Presets as AngularPresets,
} from 'rete-angular-plugin/21';

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

  private readonly socket = new ClassicPreset.Socket('Agent Flow');

  private readonly nodeByStepId = new Map<string, ReteNode>();
  private readonly stepIdByNodeId = new Map<string, string>();
  private readonly connectionIdByEdgeKey = new Map<string, string>();
  private readonly ignoredConnectionRemovalIds = new Set<string>();

  async ngAfterViewInit(): Promise<void> {
    await this.initializeEditor();

    this.editorReady = true;

    await this.syncGraphFromInputs();
  }

  async ngOnChanges(changes: SimpleChanges): Promise<void> {
    if (!this.editorReady) {
      return;
    }

    if (changes['steps'] || changes['edges'] || changes['agentNamesById']) {
      await this.syncGraphFromInputs();
    }
  }

  ngOnDestroy(): void {
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

    render.addPreset(AngularPresets.classic.setup());
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
      }

      if (context.type === 'connectionremoved' && !this.isSyncingFromAngularState) {
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
    } finally {
      this.isSyncingFromAngularState = false;
    }
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

      node.addInput('previous', new ClassicPreset.Input(this.socket, 'In'));
      node.addOutput('next', new ClassicPreset.Output(this.socket, 'Out'));

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

      const connection = new ClassicPreset.Connection(
        sourceNode,
        'next',
        targetNode,
        'previous',
      );

      await this.editor.addConnection(connection);

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

    sizedNode.width = 210;
    sizedNode.height = 92;
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

  private buildNodeLabel(step: ReteFlowStep): string {
    const stepName = step.name.trim() || 'Untitled Step';

    if (!step.assignedAgentId) {
      return `${stepName} · Unassigned`;
    }

    const agentName = this.agentNamesById[step.assignedAgentId] ?? 'Unknown agent';

    return `${stepName} · ${agentName}`;
  }

  private buildEdgeKey(sourceStepId: string, targetStepId: string): string {
    return `${sourceStepId}->${targetStepId}`;
  }
}