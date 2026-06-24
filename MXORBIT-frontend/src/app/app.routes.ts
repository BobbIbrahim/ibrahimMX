import { Routes } from '@angular/router';

import { Shell } from './core/layout/shell/shell';
import { AgentRegistryPage } from './features/agent-registry/pages/agent-registry-page/agent-registry-page';
import { AutopilotPage } from './features/autopilot/pages/autopilot-page/autopilot-page';
import { ProjectRegistryPage } from './features/project-registry/pages/project-registry-page/project-registry-page';
import { SquadsPage } from './features/squads/pages/squads-page/squads-page';
import { AgentDetailsPage } from './features/agent-registry/pages/agent-details-page/agent-details-page';
import { ProjectDetailsPage } from './features/project-registry/pages/project-details-page/project-details-page';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
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
        path: 'autopilot',
        component: AutopilotPage,
        title: 'Autopilot',
      },

      {
        path: 'agents/:agentId',
        component: AgentDetailsPage,
      title: 'Agent Details',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'agents',
  },
];