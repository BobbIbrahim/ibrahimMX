import { Routes } from '@angular/router';

import { Shell } from './core/layout/shell/shell';
import { msalAuthGuardChild } from './core/auth/msal-auth.guard';
import { AgentRegistryPage } from './features/agent-registry/pages/agent-registry-page/agent-registry-page';
import { AutopilotPage } from './features/autopilot/pages/autopilot-page/autopilot-page';
import { ProjectRegistryPage } from './features/project-registry/pages/project-registry-page/project-registry-page';
import { RunsDashboardPage } from './features/runs/pages/runs-dashboard-page/runs-dashboard-page';
import { SquadsPage } from './features/squads/pages/squads-page/squads-page';
import { AgentDetailsPage } from './features/agent-registry/pages/agent-details-page/agent-details-page';
import { ProjectDetailsPage } from './features/project-registry/pages/project-details-page/project-details-page';
import { SquadBuilderPage } from './features/squads/pages/squad-builder-page/squad-builder-page';
import { SquadLiveRunPage } from './features/squads/pages/squad-live-run-page/squad-live-run-page';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    canActivateChild: [msalAuthGuardChild],
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'agents',
      },
      {
        path: 'agents',
        component: AgentRegistryPage,
        title: 'Agent Registry',
      },
      {
        path: 'agents/:agentId',
        component: AgentDetailsPage,
        title: 'Agent Details',
      },
      {
        path: 'projects',
        component: ProjectRegistryPage,
        title: 'Project Registry',
      },
      {
        path: 'projects/:projectId',
        component: ProjectDetailsPage,
        title: 'Project Details',
      },
      {
        path: 'squads',
        component: SquadsPage,
        title: 'Squads',
      },
      {
        path: 'runs',
        component: RunsDashboardPage,
        title: 'Squad Runs',
      },
      {
        path: 'squads/builder/new',
        component: SquadBuilderPage,
        title: 'Squad Builder',
      },
      {
        path: 'squads/builder/:squadId',
        component: SquadBuilderPage,
        title: 'Squad Builder',
      },
      {
        path: 'squads/live-run/:squadId',
        component: SquadLiveRunPage,
        title: 'Squad Live Run',
      },
      {
        path: 'autopilot',
        component: AutopilotPage,
        title: 'Autopilot',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'agents',
  },
];
