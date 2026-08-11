import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { Agent } from '../../../../core/models/agent.model';
import { getModelTheme } from '../../../../core/utils/model-theme';

@Component({
  selector: 'app-agent-card',
  imports: [RouterLink, MatIconModule],
  templateUrl: './agent-card.html',
  styleUrl: './agent-card.scss',
})
export class AgentCard {
  readonly agent = input.required<Agent>();

  /** Brand color theme derived from the agent's runtime model (e.g. GPT, Claude, Gemini, MXAgents). */
  readonly theme = computed(() => getModelTheme(this.agent().model));
}