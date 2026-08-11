import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';

import { environment } from '../../../environments/environment';

/**
 * Conditional wrapper around `MsalGuard.canActivate`.
 *
 * When `environment.authentication.enabled` is `false` (the default),
 * navigation is always allowed and `MsalGuard` is never invoked - no Entra
 * redirect or authentication check occurs, so existing navigation behavior
 * is unchanged.
 *
 * When `authentication.enabled` is `true`, this delegates directly to the
 * installed `MsalGuard.canActivate` API, which redirects unauthenticated
 * users to Microsoft's hosted Entra sign-in page and allows authenticated
 * users through.
 */
export const msalAuthGuard: CanActivateFn = (route, state) => {
  if (!environment.authentication.enabled) {
    return true;
  }

  return inject(MsalGuard).canActivate(route, state);
};

/**
 * Conditional wrapper around `MsalGuard.canActivateChild`, for use on a
 * parent route so every child route is protected without repeating the
 * guard on each child entry individually.
 */
export const msalAuthGuardChild: CanActivateChildFn = (route, state) => {
  if (!environment.authentication.enabled) {
    return true;
  }

  return inject(MsalGuard).canActivateChild(route, state);
};
