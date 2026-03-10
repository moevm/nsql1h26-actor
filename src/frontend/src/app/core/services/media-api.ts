import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class MediaApi {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  getActorMedia(actorId: string, mediaId: string): Observable<Blob> {
    return this.http.get(
      `${this.apiPrefix}/actors/${encodeURIComponent(actorId)}/media/${encodeURIComponent(mediaId)}`,
      { responseType: 'blob' },
    );
  }
}
