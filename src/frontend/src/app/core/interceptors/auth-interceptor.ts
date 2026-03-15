import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthSessionService } from '../services/auth-session-service';

const METHODS_WITH_AUTH = new Set(['POST', 'PUT', 'DELETE']);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthSessionService);
  const token = auth.getToken();

  if (!METHODS_WITH_AUTH.has(req.method)) {
    return next(req);
  }

  if (!token || req.url.includes('/v1/auth/login')) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    }),
  );
};
