import { Injectable, signal } from '@angular/core';

import { Project } from '../models/project.model';

@Injectable({
  providedIn: 'root',
})
export class ProjectService {
  private readonly projectsSignal = signal<Project[]>([
    {
      id: 'project-001',
      name: 'Payments Refactor',
      description:
        'Refactor the legacy payments module to improve reliability and introduce observability.',
      status: 'active',
      repositoryUrl: 'https://github.com/company/payments-refactor',
      defaultBranch: 'main',
      stack: ['Angular', 'Spring Boot', 'PostgreSQL'],
      agentIds: ['agent-001', 'agent-002'],
      createdAt: '2026-06-15',
      updatedAt: '2026-06-22',
    },
    {
      id: 'project-002',
      name: 'Security Hardening',
      description:
        'Improve security posture by running continuous scans and enforcing stricter policies.',
      status: 'active',
      repositoryUrl: 'https://github.com/company/security-hardening',
      defaultBranch: 'develop',
      stack: ['Spring Boot', 'PostgreSQL', 'Docker'],
      agentIds: ['agent-002'],
      createdAt: '2026-06-10',
      updatedAt: '2026-06-21',
    },
    {
      id: 'project-003',
      name: 'Test Coverage Expansion',
      description:
        'Increase automated test coverage across core services and improve regression detection.',
      status: 'paused',
      repositoryUrl: 'https://github.com/company/test-coverage-expansion',
      defaultBranch: 'release/qa',
      stack: ['Angular', 'JUnit', 'Playwright'],
      agentIds: ['agent-003'],
      createdAt: '2026-06-05',
      updatedAt: '2026-06-20',
    },
    {
      id: 'project-004',
      name: 'Architecture Planning',
      description:
        'Design next-generation system architecture for scalability and modularity.',
      status: 'completed',
      repositoryUrl: 'https://github.com/company/architecture-planning',
      defaultBranch: 'main',
      stack: ['Java', 'RabbitMQ', 'Microservices'],
      agentIds: ['agent-001', 'agent-003'],
      createdAt: '2026-05-28',
      updatedAt: '2026-06-18',
    },
  ]);

  readonly projects = this.projectsSignal.asReadonly();

  getProjects() {
    return this.projects;
  }

  getProjectById(projectId: string) {
    return this.projects().find((project) => project.id === projectId);
  }
}