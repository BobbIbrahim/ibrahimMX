import { Provider } from '@angular/core';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import {
  IPublicClientApplication,
  InteractionType,
  PublicClientApplication,
  BrowserCacheLocation,
  LogLevel,
} from '@azure/msal-browser';
import {
  MSAL_INSTANCE,
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MsalGuardConfiguration,
  MsalInterceptorConfiguration,
  MsalService,
  MsalGuard,
  MsalBroadcastService,
  MsalInterceptor,
} from '@azure/msal-angular';

import { AppEnvironment } from '../config/app-environment.model';
import { validateAuthConfig } from './auth-config.validation';

/**
 * Builds the `IPublicClientApplication` instance for `MSAL_INSTANCE`.
 *
 * Disabled mode (the current default, `authentication.enabled === false`):
 * a `PublicClientApplication` is still constructed because `MsalService`,
 * `MsalGuard` and `MsalInterceptor` all require an `MSAL_INSTANCE` to be
 * injectable, but it is configured to be inert:
 *  - `auth.clientId` is left as an empty string (not a fake GUID and not the
 *    "common" tenant), so MSAL has no valid client identity to act on.
 *  - No login/redirect/token method is ever invoked against this instance in
 *    disabled mode (see msal.initializer.ts), so the empty clientId performs
 *    no network activity - it simply sits idle in memory.
 *  - `authority` is left as MSAL's own default rather than falling back to
 *    "common", per the requirement to avoid a common-tenant fallback.
 *
 * Enabled mode: validated real configuration values are used to build an
 * active instance.
 */
export function msalInstanceFactory(environment: AppEnvironment): IPublicClientApplication {
  if (!environment.authentication.enabled) {
    return new PublicClientApplication({
      auth: {
        clientId: '',
      },
      cache: {
        cacheLocation: BrowserCacheLocation.SessionStorage,
      },
      system: {
        loggerOptions: {
          loggerCallback: () => {
            /* no-op: disabled mode must not log anything, including PII */
          },
          logLevel: LogLevel.Error,
          piiLoggingEnabled: false,
        },
      },
    });
  }

  const validation = validateAuthConfig(environment);
  if (!validation.valid) {
    throw new Error(
      `Invalid authentication configuration - missing required field(s): ${validation.missingFields.join(', ')}`
    );
  }

  const { tenantId, spaClientId, authority, redirectUri, postLogoutRedirectUri } =
    environment.authentication;

  return new PublicClientApplication({
    auth: {
      clientId: spaClientId,
      authority: authority || `https://login.microsoftonline.com/${tenantId}`,
      redirectUri,
      postLogoutRedirectUri,
    },
    cache: {
      cacheLocation: BrowserCacheLocation.SessionStorage,
    },
    system: {
      loggerOptions: {
        loggerCallback: () => {
          /* no-op: token/account content must never be logged */
        },
        logLevel: LogLevel.Error,
        piiLoggingEnabled: false,
      },
    },
  });
}

/**
 * `MSAL_GUARD_CONFIG` factory.
 *
 * `authRequest.scopes` is driven entirely by `environment.authentication.loginScopes`
 * (e.g. `['User.Read']` in development) - never hardcoded here, so the scopes
 * requested for interactive sign-in stay centralized in environment config.
 *
 * No `loginFailedRoute` is set: the route tree has no dedicated
 * authentication-error/login-failure route to redirect to, and inventing one
 * is out of scope for this task.
 */
export function msalGuardConfigFactory(environment: AppEnvironment): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: {
      scopes: [...environment.authentication.loginScopes],
    },
  };
}

/**
 * `MSAL_INTERCEPTOR_CONFIG` factory.
 *
 * Disabled mode: `protectedResourceMap` is empty, so `MsalInterceptor` never
 * matches any outgoing request and therefore never attempts token
 * acquisition, even though the interceptor class itself is registered.
 *
 * Enabled mode: maps `environment.apiBaseUrl` to `environment.authentication.apiScope`
 * (MXORBIT backend) and, once configured, `environment.agentsApiBaseUrl` to
 * `environment.authentication.agentsApiScope` (MXAgents catalog API - a
 * separate protected resource/audience). Each mapping is skipped while its
 * scope is empty, so calls to that host go out without a bearer token until
 * the scope is confirmed and set.
 */
export function msalInterceptorConfigFactory(environment: AppEnvironment): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>();

  if (environment.authentication.enabled && environment.authentication.apiScope) {
    protectedResourceMap.set(environment.apiBaseUrl, [environment.authentication.apiScope]);
  }

  if (environment.authentication.enabled && environment.authentication.agentsApiScope) {
    protectedResourceMap.set(environment.agentsApiBaseUrl, [environment.authentication.agentsApiScope]);
  }

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}

/**
 * All providers required to register the MSAL foundation with the standalone
 * Angular application. Safe to include even while `authentication.enabled`
 * is `false`: no provider here performs redirect, login, or token work by
 * itself - see `msal.initializer.ts` for the one-time startup sequence.
 */
export function provideMsalFoundation(environment: AppEnvironment): Provider[] {
  return [
    {
      provide: MSAL_INSTANCE,
      useFactory: () => msalInstanceFactory(environment),
    },
    {
      provide: MSAL_GUARD_CONFIG,
      useFactory: () => msalGuardConfigFactory(environment),
    },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: () => msalInterceptorConfigFactory(environment),
    },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
    {
      provide: HTTP_INTERCEPTORS,
      useClass: MsalInterceptor,
      multi: true,
    },
  ];
}
