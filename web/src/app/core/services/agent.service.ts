import { Injectable, signal } from '@angular/core';

import { Agent } from '../models/agent.model';

@Injectable({
  providedIn: 'root',
})
export class AgentService {
  private readonly agentsSignal = signal<Agent[]>([
    {
      id: 'agent-001',
      agentKey: 'ticket-type-classifier',
      name: 'Ticket Type Classifier',
      role: 'Change intake',
      model: 'GPT-4.1',
      description:
        'Classifies incoming changes and determines their ticket type for downstream planning.',
      avatar: 'TC',
      accentColor: '#f97316',
      status: 'online',
      capabilities: ['Change Classification'],
      inputs: ['change'],
      outputs: ['change', 'ticketType'],
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
      inputs: ['change', 'ticketType'],
      outputs: ['change', 'ticketType', 'test'],
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
      inputs: ['change', 'ticketType', 'test'],
      outputs: ['change', 'ticketType', 'test', 'nextAction'],
      runCount: 0,
      successRate: 0,
    },
  ]);

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
}
