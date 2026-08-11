import { AppEnvironment } from '../app/core/config/app-environment.model';

export const environment: AppEnvironment = {
  production: true,
  apiBaseUrl: '',
  agentsApiBaseUrl: '',
  agentsTenantId: '',
  authentication: {
    enabled: false,
    tenantId: '',
    spaClientId: '',
    authority: '',
    redirectUri: '',
    postLogoutRedirectUri: '',
    loginScopes: [],
    apiScope: '',
    agentsApiScope: '',
  },
};
