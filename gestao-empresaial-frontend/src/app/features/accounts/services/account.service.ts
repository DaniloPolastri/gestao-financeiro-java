import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  AccountResponse,
  AccountType,
  AccountStatus,
  CreateAccountRequest,
  UpdateAccountRequest,
  PayAccountRequest,
  PageResponse,
} from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/accounts';

  private readonly _accounts = signal<AccountResponse[]>([]);
  private readonly _totalElements = signal(0);
  private readonly _totalPages = signal(0);
  private readonly _currentPage = signal(0);

  readonly accounts = this._accounts.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly totalPages = this._totalPages.asReadonly();
  readonly currentPage = this._currentPage.asReadonly();
  readonly hasAccounts = computed(() => this._accounts().length > 0);

  loadAccounts(
    type: AccountType,
    filters?: {
      status?: AccountStatus[];
      categoryId?: string;
      supplierId?: string;
      clientId?: string;
      dueDateFrom?: string;
      dueDateTo?: string;
    },
    page = 0,
    size = 20,
  ): Observable<PageResponse<AccountResponse>> {
    let params = new HttpParams()
      .set('type', type)
      .set('page', page.toString())
      .set('size', size.toString());

    if (filters?.status?.length) {
      params = params.set('status', filters.status.join(','));
    }
    if (filters?.categoryId) {
      params = params.set('categoryId', filters.categoryId);
    }
    if (filters?.supplierId) {
      params = params.set('supplierId', filters.supplierId);
    }
    if (filters?.clientId) {
      params = params.set('clientId', filters.clientId);
    }
    if (filters?.dueDateFrom) {
      params = params.set('dueDateFrom', filters.dueDateFrom);
    }
    if (filters?.dueDateTo) {
      params = params.set('dueDateTo', filters.dueDateTo);
    }

    return this.http.get<PageResponse<AccountResponse>>(this.API_URL, { params }).pipe(
      tap((response) => {
        this._accounts.set(response.content);
        this._totalElements.set(response.totalElements);
        this._totalPages.set(response.totalPages);
        this._currentPage.set(response.number);
      }),
    );
  }

  getById(id: string): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.API_URL}/${id}`);
  }

  create(data: CreateAccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(this.API_URL, data);
  }

  update(id: string, data: UpdateAccountRequest): Observable<AccountResponse> {
    return this.http.put<AccountResponse>(`${this.API_URL}/${id}`, data);
  }

  pay(id: string, data: PayAccountRequest): Observable<AccountResponse> {
    return this.http.patch<AccountResponse>(`${this.API_URL}/${id}/pay`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
