export type AgentStatus = 'online' | 'idle' | 'offline';

export interface Agent {
  id: string;
  agentKey: string;
  name: string;
  role: string;
  model: string;
  description: string;
  avatar: string;
  accentColor: string;
  status: AgentStatus;
  capabilities: string[];
  inputs: string[];
  outputs: string[];
  runCount: number;
  successRate: number;
}