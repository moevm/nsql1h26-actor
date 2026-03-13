import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { components } from '../../shared/api/types';

type LoginRequest = components['schemas']['LoginRequest'];
type LoginResponse = components['schemas']['LoginResponse'];

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiPrefix = '/v1';

  login(body: LoginRequest): Observable<LoginResponse>{
    return this.http.post<LoginResponse>(`${this.apiPrefix}/auth/login`, body);
  }
}
