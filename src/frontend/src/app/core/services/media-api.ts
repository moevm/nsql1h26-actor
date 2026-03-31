import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { components, operations } from '../../shared/api/types';

type ActorMediaType = components['schemas']['ActorMediaType'];
type MediaUploadResponse = components['schemas']['MediaUploadResponse'];

export type CreateActorMediaRequest = {
  file: File;
  type: components['schemas']['ActorMediaType'];
  caption?: string | null;
};

@Injectable({
  providedIn: 'root',
})
export class MediaApi {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  createActorMedia(
    actorId: string,
    request: CreateActorMediaRequest,
  ): Observable<MediaUploadResponse> {
    const formData = new FormData();
    formData.append('file', request.file);
    formData.append('type', request.type satisfies ActorMediaType);

    return this.http.post<MediaUploadResponse>(
      `${this.apiPrefix}/actors/${encodeURIComponent(actorId)}/media`,
      formData,
    );
  }

  deleteActorMedia(actorId: string, mediaId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiPrefix}/actors/${encodeURIComponent(actorId)}/media/${encodeURIComponent(mediaId)}`,
    );
  }

  getActorMedia(actorId: string, mediaId: string): Observable<Blob> {
    return this.http.get(
      `${this.apiPrefix}/actors/${encodeURIComponent(actorId)}/media/${encodeURIComponent(mediaId)}`,
      { responseType: 'blob' },
    );
  }
}
