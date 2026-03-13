import { inject, Injectable } from '@angular/core';
import { components } from '../../shared/api/types';

type LoginResponse = components['schemas']['LoginResponse'];
type AuthSession = {
  token: string;
  expiresAt: number;
};

@Injectable({
  providedIn: 'root',
})
export class AuthSessionService {
  private readonly storageKey = 'auth_session';
  private readonly autoLogoutSkewMs = 5000;
  
  private logoutTimerId: number | null = null;
  private session: AuthSession | null = null;

  get isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  initializeSession(): void {
    const restored = this.readSessionFromStorage();
    if (!restored || this.isExpired(restored.expiresAt)) {
      this.clearSession();
      return;
    }

    this.session = restored;
    this.scheduleAutoLogout(restored.expiresAt);
  }

  setSession(response: LoginResponse): void {
    const token = response.token?.trim();
    const expiresInSeconds = response.expiresIn ?? 0;
    if (!token || expiresInSeconds <= 0) {
      this.clearSession();
      return;
    }

    const nextSession: AuthSession = {
      token,
      expiresAt: Date.now() + expiresInSeconds * 1000,
    };
    this.session = nextSession;
    this.writeSessionToStorage(nextSession);
    this.scheduleAutoLogout(nextSession.expiresAt);
  }

  getToken(): string | null {
    if (!this.session) {
      this.initializeSession();
    }

    if (!this.session || this.isExpired(this.session.expiresAt)) {
      this.clearSession();
      return null;
    }

    return this.session.token;
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }

  clearSession(): void {
    this.clearLogoutTimer();
    this.session = null;
    localStorage.removeItem(this.storageKey);
  }

  private scheduleAutoLogout(expiresAt: number): void {
    this.clearLogoutTimer();
    const timeoutMs = Math.max(expiresAt - Date.now() - this.autoLogoutSkewMs, 0);
    this.logoutTimerId = window.setTimeout(() => {
      this.clearSession();
    }, timeoutMs);
  }

  private clearLogoutTimer(): void {
    if (this.logoutTimerId !== null) {
      window.clearTimeout(this.logoutTimerId);
      this.logoutTimerId = null;
    }
  }

  private isExpired(expiresAt: number): boolean {
    return expiresAt <= Date.now();
  }

  private readSessionFromStorage(): AuthSession | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return null;
    }

    try {
      const parsed = JSON.parse(raw) as Partial<AuthSession>;
      const token = parsed.token?.trim();
      const expiresAt = parsed.expiresAt;
      if (!token || typeof expiresAt !== 'number' || !Number.isFinite(expiresAt)) {
        return null;
      }
      return { token, expiresAt };
    } catch {
      return null;
    }
  }

  private writeSessionToStorage(session: AuthSession): void {
    localStorage.setItem(this.storageKey, JSON.stringify(session));
  }
}
