import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  BankImport,
  BankImportItem,
  BankImportSummary,
  BatchUpdateImportItemsRequest,
  UpdateImportItemRequest,
} from '../models/bank-import.model';

@Injectable({ providedIn: 'root' })
export class BankImportService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/imports';

  upload(file: File): Observable<BankImport> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<BankImport>(`${this.base}/upload`, form);
  }

  list(): Observable<BankImportSummary[]> {
    return this.http.get<BankImportSummary[]>(this.base);
  }

  getById(id: string): Observable<BankImport> {
    return this.http.get<BankImport>(`${this.base}/${id}`);
  }

  updateItem(importId: string, itemId: string, body: UpdateImportItemRequest): Observable<BankImportItem> {
    return this.http.patch<BankImportItem>(`${this.base}/${importId}/items/${itemId}`, body);
  }

  updateItemsBatch(importId: string, body: BatchUpdateImportItemsRequest): Observable<BankImportItem[]> {
    return this.http.patch<BankImportItem[]>(`${this.base}/${importId}/items/batch`, body);
  }

  confirm(importId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${importId}/confirm`, {});
  }

  cancel(importId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${importId}/cancel`, {});
  }
}
