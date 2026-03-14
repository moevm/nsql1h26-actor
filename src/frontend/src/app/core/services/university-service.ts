import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { components, operations } from '../../shared/api/types';

type UniversitiesSearchQuery = operations['v1UniversitiesSearchGet']['parameters']['query'];
type UniversitiesSearchItem = components['schemas']['UniversitySearchItem'];

@Injectable({
  providedIn: 'root',
})
export class UniversityService {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  getUniversityByName(request: UniversitiesSearchQuery): Observable<UniversitiesSearchItem[]> {
    return this.http.get<UniversitiesSearchItem[]>(`${this.apiPrefix}/universities/search`, {
      params: {
        q: request.q,
        ...(request.limit != null ? { limit: request.limit } : {}),
      },
    });
  }
}
