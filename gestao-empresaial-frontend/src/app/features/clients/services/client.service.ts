import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ClientResponse, CreateClientRequest, UpdateClientRequest } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);
  private readonly API_URL = '/api/clients';

  private readonly _clients = signal<ClientResponse[]>([]);
  readonly clients = this._clients.asReadonly();
  readonly hasClients = computed(() => this._clients().length > 0);

  loadClients(): Observable<ClientResponse[]> {
    return this.http.get<ClientResponse[]>(this.API_URL).pipe(
      tap((clients) => this._clients.set(clients)),
    );
  }

  getById(id: string): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`${this.API_URL}/${id}`);
  }

  create(data: CreateClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>(this.API_URL, data);
  }

  update(id: string, data: UpdateClientRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`${this.API_URL}/${id}`, data);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }
}
