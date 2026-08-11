import { AppEnvironment } from '../config/app-environment.model';

export interface AuthConfigValidationResult {
  readonly valid: boolean;
  readonly missingFields: string[];
}

/**
 * Pure validation of the authentication portion of the app environment.
 *
 * When `authentication.enabled` is false, empty Entra ID values are valid
 * (this is the default, disabled state). When enabled, every field required
 * for interactive sign-in via `MsalGuard` must be present, otherwise the
 * missing field names are returned.
 *
 * `apiScope` targets the future MXORBIT backend API and is intentionally
 * NOT required here: interactive sign-in only needs `loginScopes` (e.g.
 * Microsoft Graph's `User.Read`). `apiScope` should only become required once
 * backend API token protection is enabled through a separate, explicit
 * configuration flag - not merely because `authentication.enabled` is true.
 *
 * This function performs no I/O and is not invoked during application
 * startup in this task.
 */
export function validateAuthConfig(env: AppEnvironment): AuthConfigValidationResult {
  if (!env.authentication.enabled) {
    return { valid: true, missingFields: [] };
  }

  const requiredStringFields: Array<[string, string]> = [
    ['authentication.tenantId', env.authentication.tenantId],
    ['authentication.spaClientId', env.authentication.spaClientId],
    ['authentication.authority', env.authentication.authority],
    ['authentication.redirectUri', env.authentication.redirectUri],
    ['authentication.postLogoutRedirectUri', env.authentication.postLogoutRedirectUri],
    ['apiBaseUrl', env.apiBaseUrl],
  ];

  const missingFields = requiredStringFields
    .filter(([, value]) => value.trim().length === 0)
    .map(([fieldName]) => fieldName);

  if (env.authentication.loginScopes.length === 0) {
    missingFields.push('authentication.loginScopes');
  }

  return { valid: missingFields.length === 0, missingFields };
}
