import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { components } from '../../shared/api/types';

type Actor = components['schemas']['Actor'];

@Injectable({
  providedIn: 'root',
})
export class ActorsApi {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  getActorById(actorId: string): Observable<Actor> {
    return this.http.get<Actor>(`${this.apiPrefix}/actors/${encodeURIComponent(actorId)}`);
  }
}
