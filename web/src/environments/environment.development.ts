import { AppEnvironment } from '../app/core/config/app-environment.model';

export const environment: AppEnvironment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  agentsApiBaseUrl: 'https://api.mxagents-dev.murex.com/api',
  agentsTenantId: '00000000-0000-0000-0000-000000000001',
  authentication: {
    enabled: true,
    tenantId: '9a839770-e9fc-4737-905c-370f65b0e224',
    spaClientId: '94d34a81-d19e-40ff-871f-d2a5772c52c6',
    authority: 'https://login.microsoftonline.com/9a839770-e9fc-4737-905c-370f65b0e224',
    redirectUri: 'http://localhost:4200/',
    postLogoutRedirectUri: 'http://localhost:4200/',
    loginScopes: ['User.Read'],
    apiScope: '',
    agentsApiScope: '',
  },
};
