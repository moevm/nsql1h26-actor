import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { components } from '../../shared/api/types';

export type CatalogSnapshot = components['schemas']['CatalogSnapshot'];

@Injectable({
  providedIn: 'root',
})
export class ImportExportService {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  importData(snapshot: CatalogSnapshot): Observable<void> {
    return this.http.post<void>(`${this.apiPrefix}/catalog/import`, snapshot);
  }

  exportData(): Observable<CatalogSnapshot> {
    return this.http.get<CatalogSnapshot>(`${this.apiPrefix}/catalog/export`);
  }
}
