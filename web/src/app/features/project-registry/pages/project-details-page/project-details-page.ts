import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Agent } from '../../../../core/models/agent.model';
import { AgentService } from '../../../../core/services/agent.service';
import { ProjectService } from '../../../../core/services/project.service';

@Component({
  selector: 'app-project-details-page',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './project-details-page.html',
  styleUrl: './project-details-page.scss',
})
export class ProjectDetailsPage {
  private readonly route = inject(ActivatedRoute);
  private readonly projectService = inject(ProjectService);
  private readonly agentService = inject(AgentService);

  private readonly projectId = this.route.snapshot.paramMap.get('projectId');

  readonly project = computed(() => {
    if (!this.projectId) {
      return undefined;
    }

    return this.projectService.getProjectById(this.projectId);
  });

  readonly assignedAgents = computed(() => {
    const selectedProject = this.project();

    if (!selectedProject) {
      return [];
    }

    return selectedProject.agentIds
      .map((agentId) => this.agentService.getAgentById(agentId))
      .filter((agent): agent is Agent => Boolean(agent));
  });
}