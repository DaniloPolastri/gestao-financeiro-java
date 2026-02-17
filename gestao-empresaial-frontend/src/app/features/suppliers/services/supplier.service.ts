import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { SupplierResponse, CreateSupplierRequest, UpdateSupplierRequest } from '../models/supplier.model';

@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/suppliers';

  private readonly _suppliers = signal<SupplierResponse[]>([]);
  readonly suppliers = this._suppliers.asReadonly();
  readonly hasSuppliers = computed(() => this._suppliers().length > 0);

  loadSuppliers(): Observable<SupplierResponse[]> {
    return this.http.get<SupplierResponse[]>(this.API_URL).pipe(
      tap((suppliers) => this._suppliers.set(suppliers)),
    );
  }

  getById(id: string): Observable<SupplierResponse> {
    return this.http.get<SupplierResponse>(`${this.API_URL}/${id}`);
  }

  create(data: CreateSupplierRequest): Observable<SupplierResponse> {
    return this.http.post<SupplierResponse>(this.API_URL, data);
  }

  update(id: string, data: UpdateSupplierRequest): Observable<SupplierResponse> {
    return this.http.put<SupplierResponse>(`${this.API_URL}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
