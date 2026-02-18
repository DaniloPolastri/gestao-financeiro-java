import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { SupplierService } from '../../services/supplier.service';
import { SupplierResponse } from '../../models/supplier.model';
import { DrawerComponent } from '../../../../shared/ui/drawer/drawer.component';
import { SupplierDrawerFormComponent } from '../../components/supplier-drawer-form/supplier-drawer-form.component';

@Component({
  selector: 'app-supplier-list',
  imports: [DrawerComponent, SupplierDrawerFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly drawerTitle = () => this.editingId() ? 'Editar Fornecedor' : 'Novo Fornecedor';

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar fornecedores'); },
    });
  }

  protected openNew() { this.editingId.set(null); this.drawerOpen.set(true); }
  protected openEdit(id: string) { this.editingId.set(id); this.drawerOpen.set(true); }
  protected closeDrawer() { this.drawerOpen.set(false); this.editingId.set(null); }

  protected onSaved() {
    this.closeDrawer();
    this.supplierService.loadSuppliers().subscribe();
  }

  protected deleteSupplier(supplier: SupplierResponse) {
    if (!confirm(`Deseja realmente excluir o fornecedor "${supplier.name}"?`)) return;
    this.supplierService.delete(supplier.id).subscribe({
      next: () => this.supplierService.loadSuppliers().subscribe(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir fornecedor'),
    });
  }
}
