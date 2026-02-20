import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { BankImportService } from '../../services/bank-import.service';
import { BankImport, BankImportItem } from '../../models/bank-import.model';
import { CategoryService } from '../../../categories/services/category.service';
import { SupplierService } from '../../../suppliers/services/supplier.service';
import { ClientService } from '../../../clients/services/client.service';

@Component({
  selector: 'app-bank-import-review',
  templateUrl: './bank-import-review.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, DecimalPipe],
})
export class BankImportReviewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly importService = inject(BankImportService);
  private readonly categoryService = inject(CategoryService);
  private readonly supplierService = inject(SupplierService);
  private readonly clientService = inject(ClientService);

  protected readonly bankImport = signal<BankImport | null>(null);
  protected readonly loading = signal(true);
  protected readonly confirming = signal(false);
  protected readonly selectedIds = signal<Set<string>>(new Set());
  protected readonly showConfirmCancel = signal(false);
  protected readonly reviewed = signal(false);

  protected readonly currentReviewPage = signal(0);
  private readonly pageSize = 25;

  protected readonly allItems = computed(() => this.bankImport()?.items ?? []);

  protected readonly pagedItems = computed(() => {
    const start = this.currentReviewPage() * this.pageSize;
    return this.allItems().slice(start, start + this.pageSize);
  });

  protected readonly totalReviewPages = computed(() =>
    Math.ceil(this.allItems().length / this.pageSize),
  );

  protected readonly reviewPages = computed(() =>
    Array.from({ length: this.totalReviewPages() }),
  );

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly clients = this.clientService.clients;
  protected readonly groups = this.categoryService.groups;

  protected readonly categories = computed(() =>
    this.groups().flatMap((g) => g.categories ?? []),
  );

  protected readonly readyCount = computed(() =>
    (this.bankImport()?.items ?? []).filter((i) => i.supplierId && i.categoryId).length,
  );

  protected readonly totalCount = computed(() => this.bankImport()?.items?.length ?? 0);

  protected readonly allReady = computed(
    () => this.readyCount() === this.totalCount() && this.totalCount() > 0,
  );

  protected readonly allSelected = computed(
    () => this.selectedIds().size === this.totalCount() && this.totalCount() > 0,
  );

  protected readonly isEditable = computed(
    () => this.bankImport()?.status === 'PENDING_REVIEW',
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;

    // Carrega dados dependentes em paralelo
    this.supplierService.loadSuppliers().subscribe();
    this.clientService.loadClients().subscribe();
    this.categoryService.loadGroups().subscribe();

    this.importService.getById(id).subscribe({
      next: (data) => {
        this.bankImport.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected toggleSelectAll(): void {
    const items = this.bankImport()?.items ?? [];
    if (this.allSelected()) {
      this.selectedIds.set(new Set());
    } else {
      this.selectedIds.set(new Set(items.map((i) => i.id)));
    }
  }

  protected toggleSelect(id: string): void {
    const next = new Set(this.selectedIds());
    next.has(id) ? next.delete(id) : next.add(id);
    this.selectedIds.set(next);
  }

  protected goToReviewPage(page: number): void {
    this.currentReviewPage.set(page);
  }

  protected updateItemField(item: BankImportItem, field: string, value: string | null): void {
    const importId = this.bankImport()!.id;
    this.importService
      .updateItem(importId, item.id, { [field]: value || undefined })
      .subscribe((updated) => this.replaceItem(updated));
  }

  protected applyBulk(accountType: string | null, supplierId: string | null, categoryId: string | null): void {
    const importId = this.bankImport()!.id;
    const itemIds = Array.from(this.selectedIds());
    if (!itemIds.length) return;

    this.importService
      .updateItemsBatch(importId, {
        itemIds,
        accountType: (accountType as 'PAYABLE' | 'RECEIVABLE') ?? undefined,
        supplierId: supplierId ?? undefined,
        categoryId: categoryId ?? undefined,
      })
      .subscribe((updated) => updated.forEach((u) => this.replaceItem(u)));
  }

  private replaceItem(updated: BankImportItem): void {
    const current = this.bankImport();
    if (!current) return;
    this.bankImport.set({
      ...current,
      items: current.items.map((i) => (i.id === updated.id ? updated : i)),
    });
  }

  protected confirm(): void {
    const importId = this.bankImport()!.id;
    this.confirming.set(true);
    this.importService.confirm(importId).subscribe({
      next: () => this.router.navigate(['/importacao']),
      error: () => this.confirming.set(false),
    });
  }

  protected cancelImport(): void {
    const importId = this.bankImport()!.id;
    this.importService.cancel(importId).subscribe({
      next: () => this.router.navigate(['/importacao']),
    });
  }
}
