import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { components, operations } from '../../shared/api/types';

type UniversitiesSearchQuery = operations['v1UniversitiesSearchGet']['parameters']['query'];
type UniversitiesSearchItem = components['schemas']['UniversitySearchItem'];

export type UniversityCreateRequest = components['schemas']['UniversityCreate'];
export type UniversityCreateResponse = components['schemas']['UniversityCreateResponse'];

export type UniversityUpdateRequest = components['schemas']['UniversityUpdate'];

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

  createUniversity(request: UniversityCreateRequest): Observable<UniversityCreateResponse> {
    return this.http.post<UniversityCreateResponse>(`${this.apiPrefix}/universities`, request);
  }

  updateUniversity(
    id: string,
    request: UniversityUpdateRequest,
  ): Observable<UniversityCreateResponse> {
    return this.http.patch<UniversityCreateResponse>(
      `${this.apiPrefix}/universities/${id}`,
      request,
    );
  }

  deleteUniversity(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiPrefix}/universities/${encodeURIComponent(id)}`);
  }
}
