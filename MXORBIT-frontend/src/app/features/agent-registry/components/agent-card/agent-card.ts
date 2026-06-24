import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { Agent } from '../../../../core/models/agent.model';

@Component({
  selector: 'app-agent-card',
  imports: [RouterLink, MatIconModule],
  templateUrl: './agent-card.html',
  styleUrl: './agent-card.scss',
})
export class AgentCard {
  readonly agent = input.required<Agent>();
}