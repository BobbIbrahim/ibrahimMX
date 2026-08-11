import { inject, provideAppInitializer } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { MsalService } from '@azure/msal-angular';

import { AppEnvironment } from '../config/app-environment.model';

export async function initializeMsal(environment: AppEnvironment): Promise<void> {
  if (!environment.authentication.enabled) {
    return;
  }

  const msalService = inject(MsalService);

  await firstValueFrom(msalService.initialize());
  const redirectResult = await firstValueFrom(msalService.handleRedirectObservable());

  if (redirectResult?.account) {
    msalService.instance.setActiveAccount(redirectResult.account);
    return;
  }

  const activeAccount = msalService.instance.getActiveAccount();
  if (!activeAccount) {
    const cachedAccounts = msalService.instance.getAllAccounts();
    if (cachedAccounts.length > 0) {
      msalService.instance.setActiveAccount(cachedAccounts[0]);
    }
  }
}

export function provideMsalInitializer(environment: AppEnvironment) {
  return provideAppInitializer(() => initializeMsal(environment));
}
