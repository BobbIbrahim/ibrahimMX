import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Agent, AgentStatus } from '../models/agent.model';

/** Shape returned by `GET /tenants/{tenantId}/agents` on the MXAgents catalog API. */
export interface AgentApiResponse {
  id: string;
  agentKey: string;
  displayName: string;
  description: string | null;
  status: string;
  channels: string[];
  integrations: string[];
  dockerImage: string;
  contextWindow: number;
  managedContext: boolean;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Static pipeline-step agents mirroring the backend's `InMemoryAgentRegistry`
 * (change-classifier, test-selector, deployment-planner). These power squad
 * builder steps rather than deployed conversational agents, so they are not
 * part of the MXAgents tenant catalog and are not returned by
 * `loadAgentsFromApi`; they stay hardcoded here so squad/project mock data
 * that references them keeps working.
 */
const PIPELINE_AGENTS: Agent[] = [
  {
    id: 'agent-001',
    agentKey: 'change-classifier',
    name: 'Change Classifier',
    role: 'Change intake',
    model: 'GPT-4.1',
    description:
      'Classifies incoming changes and determines their change type for downstream planning.',
    avatar: 'CC',
    accentColor: '#f97316',
    status: 'online',
    capabilities: ['Change Classification'],
    inputs: ['change'],
    outputs: ['change', 'changeType'],
    runCount: 0,
    successRate: 0,
  },
  {
    id: 'agent-002',
    agentKey: 'test-selector',
    name: 'Test Selector',
    role: 'Test planning',
    model: 'Claude Sonnet',
    description:
      'Selects the relevant tests to run based on a change and its classified change type.',
    avatar: 'TS',
    accentColor: '#0ea5e9',
    status: 'online',
    capabilities: ['Test Selection'],
    inputs: ['change', 'changeType'],
    outputs: ['change', 'changeType', 'test'],
    runCount: 0,
    successRate: 0,
  },
  {
    id: 'agent-003',
    agentKey: 'deployment-planner',
    name: 'Deployment Planner',
    role: 'Release planning',
    model: 'Gemini Pro',
    description:
      'Plans the deployment steps based on the change, its change type, and the selected tests.',
    avatar: 'DP',
    accentColor: '#a855f7',
    status: 'online',
    capabilities: ['Deployment Planning'],
    inputs: ['change', 'changeType', 'test'],
    outputs: ['change', 'changeType', 'test', 'nextAction'],
    runCount: 0,
    successRate: 0,
  },
];

@Injectable({
  providedIn: 'root',
})
export class AgentService {
  private readonly http = inject(HttpClient);

  private readonly baseUrl = environment.agentsApiBaseUrl;
  private readonly tenantId = environment.agentsTenantId;

  private readonly agentsSignal = signal<Agent[]>([...PIPELINE_AGENTS]);

  readonly agents = this.agentsSignal.asReadonly();

  getAgents() {
    return this.agents;
  }

  getAgentById(agentId: string) {
    return this.agents().find((agent) => agent.id === agentId);
  }

  getAgentByKey(agentKey: string) {
    return this.agents().find((agent) => agent.agentKey === agentKey);
  }

  /**
   * Loads the deployed MXAgents catalog for the current tenant and merges it
   * with the static pipeline-step agents.
   */
  loadAgentsFromApi(): Observable<Agent[]> {
    return this.http
      .get<AgentApiResponse[]>(`${this.baseUrl}/tenants/${this.tenantId}/agents`)
      .pipe(
        map((apiAgents) => apiAgents.map((apiAgent) => this.mapApiResponseToAgent(apiAgent))),
        tap((agents) => {
          this.agentsSignal.set([...agents, ...PIPELINE_AGENTS]);
        }),
      );
  }

  private mapApiResponseToAgent(apiAgent: AgentApiResponse): Agent {
    return {
      id: apiAgent.id,
      agentKey: apiAgent.agentKey,
      name: apiAgent.displayName,
      role: apiAgent.channels.length > 0 ? apiAgent.channels.join(', ') : 'AI Agent',
      model: 'MXAgents',
      description: apiAgent.description ?? `Deployed from ${apiAgent.dockerImage}.`,
      avatar: this.buildAvatar(apiAgent.displayName),
      accentColor: '#0891b2',
      status: this.mapStatus(apiAgent.status),
      capabilities: apiAgent.channels,
      inputs: [],
      outputs: [],
      runCount: 0,
      successRate: 0,
    };
  }

  private buildAvatar(displayName: string): string {
    const words = displayName.trim().split(/\s+/).filter(Boolean);

    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase();
    }

    return (words[0] ?? '').slice(0, 2).toUpperCase();
  }

  /** Maps the MXAgents deployment status to the registry's simplified status. */
  private mapStatus(apiStatus: string): AgentStatus {
    switch (apiStatus) {
      case 'DEPLOYED':
        return 'online';
      case 'PAUSED':
      case 'STOPPED':
      case 'DISABLED':
        return 'idle';
      default:
        return 'offline';
    }
  }
}
