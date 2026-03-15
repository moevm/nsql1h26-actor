import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { components, operations } from '../../shared/api/types';

type Actor = components['schemas']['Actor'];
type ActorsSearchQuery = operations['v1ActorsGet']['parameters']['query'];
type ActorsSearchResponse = operations['v1ActorsGet']['responses'][200]['content']['application/json'];

@Injectable({
  providedIn: 'root',
})
export class ActorsApi {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  getActorById(actorId: string): Observable<Actor> {
    return this.http.get<Actor>(`${this.apiPrefix}/actors/${encodeURIComponent(actorId)}`);
  }

  getActorbyLimit(limit: number): Observable<Actor[]>{
    return this.http.get<Actor[]>(`${this.apiPrefix}/actors?limit=${limit}`);
  }

  getActors(request: ActorsSearchQuery): Observable<ActorsSearchResponse> {
    return this.http.get<ActorsSearchResponse>(`${this.apiPrefix}/actors`, {
      params: {
        ...(request?.gender ? { gender: request.gender } : {}),
        ...(request?.ageFrom != null ? { ageFrom: request.ageFrom } : {}),
        ...(request?.ageTo != null ? { ageTo: request.ageTo } : {}),
        ...(request?.weightMin != null ? { weightMin: request.weightMin } : {}),
        ...(request?.weightMax != null ? { weightMax: request.weightMax } : {}),
        ...(request?.heightMin != null ? { heightMin: request.heightMin } : {}),
        ...(request?.heightMax != null ? { heightMax: request.heightMax } : {}),
        ...(request?.activityYearFrom != null ? { activityYearFrom: request.activityYearFrom } : {}),
        ...(request?.activityYearTo != null ? { activityYearTo: request.activityYearTo } : {}),
        ...(request?.universityId ? { universityId: request.universityId } : {}),
        ...(request?.theatre ? { theatre: request.theatre } : {}),
        ...(request?.title ? { title: request.title } : {}),
        ...(request?.hairColor ? { hairColor: request.hairColor } : {}),
        ...(request?.eyeColor ? { eyeColor: request.eyeColor } : {}),
        ...(request?.genres && request.genres.length > 0 ? { genres: request.genres } : {}),
        ...(request?.name ? { name: request.name } : {}),
        ...(request?.limit != null ? { limit: request.limit } : {}),
        ...(request?.offset != null ? { offset: request.offset } : {}),
      },
    });
  }
}
