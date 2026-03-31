import { inject } from '@angular/core';
import { ResolveFn, Router, RedirectCommand } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ActorsApi } from '../services/actors-api';
import { components } from '../../shared/api/types';

type Actor = components['schemas']['Actor'];

export const actorResolver: ResolveFn<Actor | RedirectCommand> = (route) => {
  const actorsApi = inject(ActorsApi);
  const router = inject(Router);

  const id = route.paramMap.get('id');
  if (!id) {
    return of(new RedirectCommand(router.parseUrl('/admin')));
  }

  return actorsApi
    .getActorById(id)
    .pipe(catchError(() => of(new RedirectCommand(router.parseUrl('/admin')))));
};
