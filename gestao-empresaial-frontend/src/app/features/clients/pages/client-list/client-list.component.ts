import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ClientService } from '../../services/client.service';
import { ClientResponse } from '../../models/client.model';

@Component({
  selector: 'app-client-list',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit {
  private readonly clientService = inject(ClientService);

  protected readonly clients = this.clientService.clients;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit() {
    this.clientService.loadClients().subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar clientes');
      },
    });
  }

  protected deleteClient(client: ClientResponse) {
    if (!confirm(`Deseja realmente excluir o cliente "${client.name}"?`)) return;

    this.clientService.delete(client.id).subscribe({
      next: () => this.clientService.loadClients().subscribe(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir cliente'),
    });
  }
}
