import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SupplierService } from '../../services/supplier.service';
import { SupplierResponse } from '../../models/supplier.model';

@Component({
  selector: 'app-supplier-list',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {
  private readonly supplierService = inject(SupplierService);

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar fornecedores');
      },
    });
  }

  protected readonly openMenuId = signal<string | null>(null);

  protected toggleMenu(id: string) {
    this.openMenuId.update((current) => (current === id ? null : id));
  }

  protected deleteSupplier(supplier: SupplierResponse) {
    if (!confirm(`Deseja realmente excluir o fornecedor "${supplier.name}"?`)) return;

    this.supplierService.delete(supplier.id).subscribe({
      next: () => this.supplierService.loadSuppliers().subscribe(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir fornecedor'),
    });
  }
}
