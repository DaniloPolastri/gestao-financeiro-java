import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { AccountType, AccountStatus, AccountResponse } from '../../models/account.model';
import { DrawerComponent } from '../../../../shared/ui/drawer/drawer.component';
import { AccountDrawerFormComponent } from '../../components/account-drawer-form/account-drawer-form.component';

@Component({
  selector: 'app-account-list',
  imports: [DrawerComponent, AccountDrawerFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-list.component.html',
})
export class AccountListComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly accountService = inject(AccountService);

  protected readonly accounts = this.accountService.accounts;
  protected readonly totalElements = this.accountService.totalElements;
  protected readonly totalPages = this.accountService.totalPages;
  protected readonly currentPage = this.accountService.currentPage;
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly activeTab = signal<string>('todos');
  protected readonly drawerOpen = signal(false);
  protected readonly editingId = signal<string | null>(null);
  protected readonly selectedIds = signal<Set<string>>(new Set());
  protected readonly hasSelection = computed(() => this.selectedIds().size > 0);
  protected readonly selectableAccounts = computed(() =>
    this.accounts().filter(a => a.status !== 'PAID' && a.status !== 'RECEIVED')
  );
  protected readonly allSelectableSelected = computed(() => {
    const selectable = this.selectableAccounts();
    return selectable.length > 0 && selectable.every(a => this.selectedIds().has(a.id));
  });

  protected readonly type: AccountType = this.route.snapshot.data['type'] ?? 'PAYABLE';

  protected readonly isPayable = computed(() => this.type === 'PAYABLE');
  protected readonly pageTitle = computed(() => this.isPayable() ? 'Contas a Pagar' : 'Contas a Receber');
  protected readonly pageSubtitle = computed(() =>
    this.isPayable()
      ? 'Gerencie seus compromissos financeiros e vencimentos.'
      : 'Acompanhe as entradas e faturamentos.'
  );
  protected readonly entityLabel = computed(() => this.isPayable() ? 'Fornecedor' : 'Cliente');
  protected readonly newButtonLabel = computed(() => this.isPayable() ? 'Nova Conta' : 'Novo Recebimento');
  protected readonly drawerTitle = computed(() => {
    if (this.editingId()) return this.isPayable() ? 'Editar Conta a Pagar' : 'Editar Conta a Receber';
    return this.isPayable() ? 'Nova Conta a Pagar' : 'Nova Conta a Receber';
  });
  protected readonly pages = computed(() => Array.from({ length: this.totalPages() }));

  protected readonly tabs = [
    { key: 'todos', label: 'Todos', statuses: null },
    { key: 'pendentes', label: 'Pendentes', statuses: ['PENDING'] as AccountStatus[] },
    { key: 'pagos', label: this.type === 'PAYABLE' ? 'Pagos' : 'Recebidos', statuses: (this.type === 'PAYABLE' ? ['PAID'] : ['RECEIVED']) as AccountStatus[] },
    { key: 'atrasados', label: 'Atrasados', statuses: ['OVERDUE'] as AccountStatus[] },
  ];

  ngOnInit() {
    this.loadData();
  }

  protected openNew() {
    this.editingId.set(null);
    this.drawerOpen.set(true);
  }

  protected openEdit(id: string) {
    this.editingId.set(id);
    this.drawerOpen.set(true);
  }

  protected closeDrawer() {
    this.drawerOpen.set(false);
    this.editingId.set(null);
  }

  protected toggleSelect(id: string) {
    const current = new Set(this.selectedIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedIds.set(current);
  }

  protected toggleSelectAll() {
    if (this.allSelectableSelected()) {
      this.selectedIds.set(new Set());
    } else {
      const ids = new Set(this.selectableAccounts().map(a => a.id));
      this.selectedIds.set(ids);
    }
  }

  protected clearSelection() {
    this.selectedIds.set(new Set());
  }

  protected isSelectable(account: AccountResponse): boolean {
    return account.status !== 'PAID' && account.status !== 'RECEIVED';
  }

  protected onSaved() {
    this.closeDrawer();
    this.loadData();
  }

  protected setTab(tab: string) {
    this.activeTab.set(tab);
    this.loadData();
  }

  protected goToPage(page: number) {
    this.loadData(page);
  }

  protected deleteAccount(id: string) {
    if (!confirm('Deseja realmente excluir esta conta?')) return;
    this.accountService.delete(id).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir conta'),
    });
  }

  protected getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pendente', PAID: 'Pago', RECEIVED: 'Recebido', OVERDUE: 'Atrasado', PARTIAL: 'Parcial',
    };
    return labels[status] || status;
  }

  protected formatCurrency(value: number): string {
    const prefix = this.isPayable() ? '- ' : '+ ';
    return prefix + 'R$ ' + value.toLocaleString('pt-BR', { minimumFractionDigits: 2 });
  }

  protected formatDate(dateStr: string): string {
    const date = new Date(dateStr + 'T00:00:00');
    return date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  private loadData(page = 0) {
    this.clearSelection();
    this.loading.set(true);
    const activeTabObj = this.tabs.find((t) => t.key === this.activeTab());
    const filters = activeTabObj?.statuses ? { status: activeTabObj.statuses } : undefined;
    this.accountService.loadAccounts(this.type, filters, page).subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar contas'); },
    });
  }
}
