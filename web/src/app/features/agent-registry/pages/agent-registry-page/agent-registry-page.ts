import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Agent, AgentStatus } from '../../../../core/models/agent.model';
import { AgentService } from '../../../../core/services/agent.service';
import { PageHeader } from '../../../../shared/components/page-header/page-header';
import { AgentCard } from '../../components/agent-card/agent-card';

type AgentStatusFilter = AgentStatus | 'all';

type AgentSortOption =
  | 'name-asc'
  | 'status'
  | 'success-desc'
  | 'runs-desc';

@Component({
  selector: 'app-agent-registry-page',
  imports: [
    PageHeader,
    AgentCard,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './agent-registry-page.html',
  styleUrl: './agent-registry-page.scss',
})
export class AgentRegistryPage {
  private readonly agentService = inject(AgentService);

  readonly agents = this.agentService.getAgents();

  readonly searchTerm = signal('');
  readonly selectedStatus = signal<AgentStatusFilter>('all');
  readonly selectedSort = signal<AgentSortOption>('name-asc');

  readonly filteredAgents = computed(() => {
    const searchTerm = this.searchTerm().trim().toLowerCase();
    const selectedStatus = this.selectedStatus();

    const filteredAgents = this.agents().filter((agent) => {
      const matchesStatus =
        selectedStatus === 'all' || agent.status === selectedStatus;

      const searchableText = [
        agent.name,
        agent.role,
        agent.model,
        agent.description,
        ...agent.capabilities,
      ]
        .join(' ')
        .toLowerCase();

      const matchesSearch =
        searchTerm.length === 0 || searchableText.includes(searchTerm);

      return matchesStatus && matchesSearch;
    });

    return this.sortAgents(filteredAgents, this.selectedSort());
  });

  readonly totalAgents = computed(() => this.agents().length);

  readonly onlineAgents = computed(
    () => this.agents().filter((agent) => agent.status === 'online').length,
  );

  readonly idleAgents = computed(
    () => this.agents().filter((agent) => agent.status === 'idle').length,
  );

  readonly averageSuccessRate = computed(() => {
    const agents = this.agents();

    if (agents.length === 0) {
      return 0;
    }

    const totalSuccessRate = agents.reduce(
      (sum, agent) => sum + agent.successRate,
      0,
    );

    return Math.round(totalSuccessRate / agents.length);
  });

  readonly hasActiveFilters = computed(
    () =>
      this.searchTerm().trim().length > 0 ||
      this.selectedStatus() !== 'all' ||
      this.selectedSort() !== 'name-asc',
  );

  setSearchTerm(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  setStatusFilter(status: AgentStatusFilter): void {
    this.selectedStatus.set(status);
  }

  setSortOption(sortOption: AgentSortOption): void {
    this.selectedSort.set(sortOption);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedStatus.set('all');
    this.selectedSort.set('name-asc');
  }

  private sortAgents(agents: Agent[], sortOption: AgentSortOption): Agent[] {
    const agentsToSort = [...agents];

    if (sortOption === 'name-asc') {
      return agentsToSort.sort((firstAgent, secondAgent) =>
        firstAgent.name.localeCompare(secondAgent.name),
      );
    }

    if (sortOption === 'status') {
      const statusPriority: Record<AgentStatus, number> = {
        online: 1,
        idle: 2,
        offline: 3,
      };

      return agentsToSort.sort(
        (firstAgent, secondAgent) =>
          statusPriority[firstAgent.status] - statusPriority[secondAgent.status],
      );
    }

    if (sortOption === 'success-desc') {
      return agentsToSort.sort(
        (firstAgent, secondAgent) =>
          secondAgent.successRate - firstAgent.successRate,
      );
    }

    return agentsToSort.sort(
      (firstAgent, secondAgent) => secondAgent.runCount - firstAgent.runCount,
    );
  }
}