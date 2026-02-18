import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { AccountType, AccountStatus } from '../../models/account.model';

@Component({
  selector: 'app-account-list',
  imports: [RouterLink],
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
  protected readonly basePath = computed(() => this.isPayable() ? '/contas-a-pagar' : '/contas-a-receber');

  protected readonly tabs = [
    { key: 'todos', label: 'Todos', statuses: null },
    { key: 'pendentes', label: 'Pendentes', statuses: ['PENDING'] as AccountStatus[] },
    { key: 'pagos', label: this.type === 'PAYABLE' ? 'Pagos' : 'Recebidos', statuses: (this.type === 'PAYABLE' ? ['PAID'] : ['RECEIVED']) as AccountStatus[] },
    { key: 'atrasados', label: 'Atrasados', statuses: ['OVERDUE'] as AccountStatus[] },
  ];

  ngOnInit() {
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
      PENDING: 'Pendente',
      PAID: 'Pago',
      RECEIVED: 'Recebido',
      OVERDUE: 'Atrasado',
      PARTIAL: 'Parcial',
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
    this.loading.set(true);
    const activeTabObj = this.tabs.find((t) => t.key === this.activeTab());
    const filters = activeTabObj?.statuses ? { status: activeTabObj.statuses } : undefined;

    this.accountService.loadAccounts(this.type, filters, page).subscribe({
      next: () => this.loading.set(false),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erro ao carregar contas');
      },
    });
  }
}
