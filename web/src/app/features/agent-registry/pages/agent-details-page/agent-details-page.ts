import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AgentService } from '../../../../core/services/agent.service';

@Component({
  selector: 'app-agent-details-page',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './agent-details-page.html',
  styleUrl: './agent-details-page.scss',
})
export class AgentDetailsPage {
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
}