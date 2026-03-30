import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { AuthSessionService } from '../services/auth-session-service';

export const adminGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthSessionService);
  const router = inject(Router);
  const token = auth.getToken();

  return token ? true : router.createUrlTree(['/']);
};
