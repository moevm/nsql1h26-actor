import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthSessionService } from '../services/auth-session-service';

const METHODS_WITH_AUTH = new Set(['POST', 'PATCH', 'DELETE']);
const URLS_WITH_AUTH_GET = ['/v1/catalog/export'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthSessionService);
  const token = auth.getToken();

  const needsAuth =
    METHODS_WITH_AUTH.has(req.method) ||
    (req.method === 'GET' && URLS_WITH_AUTH_GET.some((url) => req.url.includes(url)));

  if (!needsAuth) {
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
