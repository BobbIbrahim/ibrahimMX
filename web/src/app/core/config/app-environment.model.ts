/**
 * Entra ID (Azure AD) configuration required for the future MSAL integration.
 * All fields remain empty strings (or empty arrays) while `enabled` is false.
 */
export interface AuthenticationConfig {
  readonly enabled: boolean;
  readonly tenantId: string;
  readonly spaClientId: string;
  readonly authority: string;
  readonly redirectUri: string;
  readonly postLogoutRedirectUri: string;
  /**
   * Delegated scopes requested for interactive sign-in via `MsalGuard`
   * (e.g. Microsoft Graph's `User.Read`). Distinct from `apiScope`: this
   * targets whatever resource is needed just to complete login, not the
   * MXORBIT backend API.
   */
  readonly loginScopes: readonly string[];
  /**
   * Scope for the future MXORBIT backend API (used by the MSAL interceptor's
   * `protectedResourceMap`). Left empty until the backend exposes a real
   * `api://` scope - never populated with `loginScopes` values such as
   * `User.Read`, which target Microsoft Graph, not this API.
   */
  readonly apiScope: string;
  /**
   * Scope for the MXAgents catalog API (a separate resource/audience from
   * the MXORBIT backend API, hence its own scope). Used by the MSAL
   * interceptor's `protectedResourceMap` to attach a bearer token to calls
   * against `agentsApiBaseUrl`. Left empty until the exact `api://` scope is
   * confirmed (see Entra ID App Registrations > Expose an API for the
   * MXAgents app) - while empty, no token is attached and calls to
   * `agentsApiBaseUrl` go out unauthenticated.
   */
  readonly agentsApiScope: string;
}

/**
 * Typed shape of the application's build-time environment configuration.
 */
export interface AppEnvironment {
  readonly production: boolean;
  readonly apiBaseUrl: string;
  /** Base URL of the MXAgents catalog API (e.g. `https://api.mxagents-dev.murex.com/api`). */
  readonly agentsApiBaseUrl: string;
  /** Tenant id used to scope the `/tenants/{tenantId}/agents` MXAgents catalog call. */
  readonly agentsTenantId: string;
  readonly authentication: AuthenticationConfig;
}
