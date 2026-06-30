import { Injectable, signal } from '@angular/core';

import { Agent } from '../models/agent.model';

@Injectable({
  providedIn: 'root',
})
export class AgentService {
  private readonly agentsSignal = signal<Agent[]>([
    {
      id: 'agent-001',
      name: 'Code Sentinel',
      role: 'Code Review Specialist',
      model: 'GPT-4.1',
      description: 'Reviews code changes, detects risky patterns, and suggests safer implementation options.',
      avatar: 'CS',
      accentColor: '#20d9ef',
      status: 'online',
      capabilities: ['Code Review', 'Security Scan', 'Refactoring'],
      runCount: 248,
      successRate: 97,
    },
    {
      id: 'agent-002',
      name: 'Test Weaver',
      role: 'Test Generation Specialist',
      model: 'Claude Sonnet',
      description: 'Generates unit, integration, and regression tests based on project context and code changes.',
      avatar: 'TW',
      accentColor: '#8b5cf6',
      status: 'idle',
      capabilities: ['Unit Tests', 'Integration Tests', 'Coverage'],
      runCount: 164,
      successRate: 94,
    },
    {
      id: 'agent-003',
      name: 'Flow Architect',
      role: 'Workflow Design Specialist',
      model: 'Gemini Pro',
      description: 'Designs multi-step execution plans and coordinates tasks between specialized agents.',
      avatar: 'FA',
      accentColor: '#22c55e',
      status: 'online',
      capabilities: ['Planning', 'DAG Design', 'Coordination'],
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
  
}