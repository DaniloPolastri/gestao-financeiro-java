import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ClientService } from '../../services/client.service';
import { ClientResponse } from '../../models/client.model';
import { DrawerComponent } from '../../../../shared/ui/drawer/drawer.component';
import { ClientDrawerFormComponent } from '../../components/client-drawer-form/client-drawer-form.component';

@Component({
  selector: 'app-client-list',
  imports: [DrawerComponent, ClientDrawerFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-list.component.html',
})
export class ClientListComponent implements OnInit {
  private readonly clientService = inject(ClientService);

  protected readonly clients = this.clientService.clients;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly drawerTitle = computed(() => this.editingId() ? 'Editar Cliente' : 'Novo Cliente');

  ngOnInit() {
    this.clientService.loadClients().subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar clientes'); },
    });
  }

  protected openNew() { this.editingId.set(null); this.drawerOpen.set(true); }
  protected openEdit(id: string) { this.editingId.set(id); this.drawerOpen.set(true); }
  protected closeDrawer() { this.drawerOpen.set(false); this.editingId.set(null); }

  protected onSaved() {
    this.closeDrawer();
    this.clientService.loadClients().subscribe();
  }

  protected deleteClient(client: ClientResponse) {
    if (!confirm(`Deseja realmente excluir o cliente "${client.name}"?`)) return;
    this.clientService.delete(client.id).subscribe({
      next: () => this.clientService.loadClients().subscribe(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir cliente'),
    });
  }
}
