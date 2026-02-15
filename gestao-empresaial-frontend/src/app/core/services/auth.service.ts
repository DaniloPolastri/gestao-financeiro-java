import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: { id: string; name: string; email: string };
}

interface UserInfo {
  id: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<UserInfo | null>(null);

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  private readonly API_URL = '/api/auth';

  get accessToken(): string | null {
    return this._accessToken();
  }

  login(email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/login`, { email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  register(name: string, email: string, password: string) {
    return this.http
      .post<AuthResponse>(`${this.API_URL}/register`, { name, email, password })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  refreshToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return;

    return this.http
      .post<AuthResponse>(`${this.API_URL}/refresh`, { refreshToken })
      .pipe(tap((res) => this.handleAuthResponse(res)));
  }

  logout() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {
      this.http.post(`${this.API_URL}/logout`, { refreshToken }).subscribe();
    }
    this._accessToken.set(null);
    this._user.set(null);
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/login']);
  }

  private handleAuthResponse(res: AuthResponse) {
    this._accessToken.set(res.accessToken);
    this._user.set(res.user);
    localStorage.setItem('refreshToken', res.refreshToken);
  }
}
