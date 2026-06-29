import { KeyValuePipe } from '@angular/common';
import { Component, Input } from '@angular/core';

import { SocketComponent } from 'rete-angular-plugin/21';

export interface SquadNodeViewModel {
  stepName: string;
  agentName: string;
  isAssigned: boolean;
}

export type SquadNodeData = {
  id: string;
  label: string;
  inputs: Record<string, unknown>;
  outputs: Record<string, unknown>;
  view: SquadNodeViewModel;
};

@Component({
  selector: 'app-rete-squad-node',
  imports: [KeyValuePipe, SocketComponent],
  templateUrl: './rete-squad-node.html',
  styleUrl: './rete-squad-node.scss',
})
export class ReteSquadNode {
  @Input({ required: true }) data!: SquadNodeData;
  @Input() emit!: unknown;
  @Input() rendered!: unknown;

  get stepName(): string {
    return this.data.view?.stepName || 'Untitled Step';
  }

  get agentName(): string {
    return this.data.view?.agentName || 'Unassigned';
  }

  get isAssigned(): boolean {
    return Boolean(this.data.view?.isAssigned);
  }
}
