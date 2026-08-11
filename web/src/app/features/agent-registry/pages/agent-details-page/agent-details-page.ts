import { Component, OnInit, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AgentService } from '../../../../core/services/agent.service';
import { getModelTheme } from '../../../../core/utils/model-theme';

@Component({
  selector: 'app-agent-details-page',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './agent-details-page.html',
  styleUrl: './agent-details-page.scss',
})
export class AgentDetailsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly agentService = inject(AgentService);

  private readonly agentId = this.route.snapshot.paramMap.get('agentId');

  readonly returnUrl =
    this.route.snapshot.queryParamMap.get('returnUrl') ?? '/agents';

  readonly backLabel = this.returnUrl.startsWith('/projects/')
    ? 'Back to Project Details'
    : 'Back to Agent Registry';

  readonly agent = computed(() => {
    if (!this.agentId) {
      return undefined;
    }

    return this.agentService.getAgentById(this.agentId);
  });

  /** Brand color theme derived from the agent's runtime model (e.g. GPT, Claude, Gemini, MXAgents). */
  readonly theme = computed(() => getModelTheme(this.agent()?.model));

  ngOnInit(): void {
    // Supports direct navigation/refresh on this route: the MXAgents catalog
    // may not have been loaded yet if the user didn't come from the registry
    // list page first.
    if (!this.agent()) {
      this.agentService.loadAgentsFromApi().subscribe({
        error: (error) => console.error('Failed to load agents:', error),
      });
    }
  }
}