export type AgentStatus = 'online' | 'idle' | 'offline';

export interface Agent {
  id: string;
  name: string;
  role: string;
  model: string;
  description: string;
  avatar: string;
  accentColor: string;
  status: AgentStatus;
  capabilities: string[];
  runCount: number;
  successRate: number;
}