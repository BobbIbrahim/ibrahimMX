import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { provideMsalFoundation } from './core/auth/msal.providers';
import { provideMsalInitializer } from './core/auth/msal.initializer';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // MsalInterceptor relies on HTTP_INTERCEPTORS (DI-based interceptors),
    // so withInterceptorsFromDi() is required alongside provideHttpClient().
    provideHttpClient(withInterceptorsFromDi()),
    ...provideMsalFoundation(environment),
    provideMsalInitializer(environment)
  ]
};
