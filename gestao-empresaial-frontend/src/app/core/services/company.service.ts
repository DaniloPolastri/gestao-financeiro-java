import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface CompanyResponse {
  id: string;
  name: string;
  cnpj: string | null;
  segment: string | null;
  ownerId: string;
  ownerName: string;
  role: string;
  active: boolean;
}

export interface CreateCompanyRequest {
  name: string;
  cnpj?: string;
  segment?: string;
}

export interface UpdateCompanyRequest {
  name: string;
  cnpj?: string;
  segment?: string;
}

export interface CompanyMemberResponse {
  userId: string | null;
  name: string | null;
  email: string;
  role: string | null;
  status: string;
  joinedAt: string | null;
}

export interface InviteMemberRequest {
  email: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);

  private readonly _companies = signal<CompanyResponse[]>([]);
  private readonly _activeCompany = signal<CompanyResponse | null>(null);
  private readonly _loaded = signal(false);

  readonly companies = this._companies.asReadonly();
  readonly activeCompany = this._activeCompany.asReadonly();
  readonly isLoaded = this._loaded.asReadonly();
  readonly hasCompanies = computed(() => this._companies().length > 0);

  private readonly API_URL = '/api/companies';

  get activeCompanyId(): string | null {
    return this._activeCompany()?.id ?? null;
  }

  loadCompanies$(): Observable<CompanyResponse[]> {
    return this.http.get<CompanyResponse[]>(this.API_URL).pipe(
      tap((companies) => {
        this._companies.set(companies);
        this._loaded.set(true);
        this.restoreActiveCompany(companies);
      }),
    );
  }

  setActiveCompany(company: CompanyResponse) {
    this._activeCompany.set(company);
    localStorage.setItem('activeCompanyId', company.id);
  }

  addCompany(company: CompanyResponse) {
    this._companies.update((companies) => [...companies, company]);
  }

  createCompany(data: CreateCompanyRequest): Observable<CompanyResponse> {
    return this.http.post<CompanyResponse>(this.API_URL, data);
  }

  updateCompany(id: string, data: UpdateCompanyRequest): Observable<CompanyResponse> {
    return this.http.put<CompanyResponse>(`${this.API_URL}/${id}`, data);
  }

  getCompany(id: string): Observable<CompanyResponse> {
    return this.http.get<CompanyResponse>(`${this.API_URL}/${id}`);
  }

  getMembers(companyId: string): Observable<CompanyMemberResponse[]> {
    return this.http.get<CompanyMemberResponse[]>(`${this.API_URL}/${companyId}/members`);
  }

  inviteMember(companyId: string, data: InviteMemberRequest): Observable<CompanyMemberResponse> {
    return this.http.post<CompanyMemberResponse>(
      `${this.API_URL}/${companyId}/members/invite`,
      data,
    );
  }

  updateMemberRole(companyId: string, userId: string, role: string): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${companyId}/members/${userId}/role`, { role });
  }

  removeMember(companyId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${companyId}/members/${userId}`);
  }

  reset() {
    this._companies.set([]);
    this._activeCompany.set(null);
    this._loaded.set(false);
    localStorage.removeItem('activeCompanyId');
  }

  private restoreActiveCompany(companies: CompanyResponse[]) {
    if (companies.length === 0) {
      this._activeCompany.set(null);
      return;
    }
    const savedId = localStorage.getItem('activeCompanyId');
    const saved = companies.find((c) => c.id === savedId);
    this._activeCompany.set(saved ?? companies[0]);
    if (!saved && companies.length > 0) {
      localStorage.setItem('activeCompanyId', companies[0].id);
    }
  }
}
