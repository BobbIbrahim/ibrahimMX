import { Injectable, signal } from '@angular/core';

import { Agent } from '../models/agent.model';

@Injectable({
  providedIn: 'root',
})
export class AgentService {
  private readonly agentsSignal = signal<Agent[]>([
    {
      id: 'agent-001',
      agentKey: 'code-sentinel',
      name: 'Code Sentinel',
      role: 'Code Review Specialist',
      model: 'GPT-4.1',
      description: 'Reviews code changes, detects risky patterns, and suggests safer implementation options.',
      avatar: 'CS',
      accentColor: '#20d9ef',
      status: 'online',
      capabilities: ['Code Review', 'Security Scan', 'Refactoring'],
      inputs: [],
      outputs: ['message'],
      runCount: 248,
      successRate: 97,
    },
    {
      id: 'agent-002',
      agentKey: 'test-weaver',
      name: 'Test Weaver',
      role: 'Test Generation Specialist',
      model: 'Claude Sonnet',
      description: 'Generates unit, integration, and regression tests based on project context and code changes.',
      avatar: 'TW',
      accentColor: '#8b5cf6',
      status: 'idle',
      capabilities: ['Unit Tests', 'Integration Tests', 'Coverage'],
      inputs: [],
      outputs: ['message'],
      runCount: 164,
      successRate: 94,
    },
    {
      id: 'agent-003',
      agentKey: 'flow-architect',
      name: 'Flow Architect',
      role: 'Workflow Design Specialist',
      model: 'Gemini Pro',
      description: 'Designs multi-step execution plans and coordinates tasks between specialized agents.',
      avatar: 'FA',
      accentColor: '#22c55e',
      status: 'online',
      capabilities: ['Planning', 'DAG Design', 'Coordination'],
      inputs: [],
      outputs: ['message'],
      runCount: 91,
      successRate: 92,
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