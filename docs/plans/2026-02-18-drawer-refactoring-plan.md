# Drawer Refactoring Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace all create/edit page navigations with a reusable right-side drawer panel across Accounts, Suppliers, Clients, and Categories.

**Architecture:** A single `DrawerComponent` shell (shared/ui) wraps any form via `<ng-content>`. Each feature gets a dedicated drawer form component using Angular `input()`/`output()` signals. List components control open/close state with signals. Routes are simplified to list-only.

**Tech Stack:** Angular 21, Tailwind CSS 4, Signals, OnPush, no PrimeNG drawer (pure CSS)

---

## Task 1: Shared DrawerComponent

**Files:**
- Create: `gestao-empresaial-frontend/src/app/shared/ui/drawer/drawer.component.ts`

**Step 1: Create the component**

```typescript
import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';

@Component({
  selector: 'app-drawer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'onEsc()' },
  template: `
    @if (open()) {
      <div class="fixed inset-0 bg-black/30 z-40"></div>
      <div class="fixed top-0 right-0 h-full w-[480px] bg-white z-50 flex flex-col shadow-xl">
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 flex-shrink-0">
          <h2 class="text-base font-semibold text-gray-900">{{ title() }}</h2>
          <button (click)="close()" class="text-gray-400 hover:text-gray-600 transition-colors duration-150">
            <i class="pi pi-times text-sm"></i>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto px-6 py-6">
          <ng-content></ng-content>
        </div>
      </div>
    }
  `,
})
export class DrawerComponent {
  readonly open = input.required<boolean>();
  readonly title = input.required<string>();
  readonly closed = output<void>();

  protected close() {
    this.closed.emit();
  }

  protected onEsc() {
    if (this.open()) this.closed.emit();
  }
}
```

**Step 2: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/shared/ui/drawer/drawer.component.ts
git commit -m "feat(ui): add shared DrawerComponent"
```

---

## Task 2: AccountDrawerFormComponent

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/accounts/components/account-drawer-form/account-drawer-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/accounts/components/account-drawer-form/account-drawer-form.component.html`

**Step 1: Create the TypeScript component**

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, OnInit, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AccountService } from '../../services/account.service';
import { AccountType } from '../../models/account.model';
import { SupplierService } from '../../../suppliers/services/supplier.service';
import { ClientService } from '../../../clients/services/client.service';
import { CategoryService } from '../../../categories/services/category.service';

@Component({
  selector: 'app-account-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-drawer-form.component.html',
})
export class AccountDrawerFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly accountService = inject(AccountService);
  private readonly supplierService = inject(SupplierService);
  private readonly clientService = inject(ClientService);
  private readonly categoryService = inject(CategoryService);

  readonly type = input.required<AccountType>();
  readonly accountId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);
  protected readonly showRecurrence = signal(false);

  protected readonly suppliers = this.supplierService.suppliers;
  protected readonly clients = this.clientService.clients;
  protected readonly groups = this.categoryService.groups;

  protected readonly form = this.fb.nonNullable.group({
    description: ['', [Validators.required, Validators.minLength(2)]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    dueDate: ['', [Validators.required]],
    categoryId: ['', [Validators.required]],
    supplierId: [''],
    clientId: [''],
    notes: [''],
  });

  protected readonly recurrenceForm = this.fb.nonNullable.group({
    frequency: ['MONTHLY'],
    endDate: [''],
    maxOccurrences: [0],
  });

  constructor() {
    effect(() => {
      const id = this.accountId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.accountService.getById(id).subscribe({
          next: (account) => {
            this.form.patchValue({
              description: account.description,
              amount: account.amount,
              dueDate: account.dueDate,
              categoryId: account.category?.id || '',
              supplierId: account.supplier?.id || '',
              clientId: account.client?.id || '',
              notes: account.notes || '',
            });
            this.loading.set(false);
          },
          error: () => {
            this.error.set('Erro ao carregar conta');
            this.loading.set(false);
          },
        });
      } else {
        this.isEdit.set(false);
        this.form.reset({ description: '', amount: 0, dueDate: '', categoryId: '', supplierId: '', clientId: '', notes: '' });
        this.showRecurrence.set(false);
        this.error.set(null);
      }
    });
  }

  ngOnInit() {
    this.supplierService.loadSuppliers().subscribe();
    this.clientService.loadClients().subscribe();
    this.categoryService.loadGroups().subscribe();
  }

  protected get isPayable() {
    return this.type() === 'PAYABLE';
  }

  protected toggleRecurrence() {
    this.showRecurrence.update((v) => !v);
  }

  protected cancel() {
    this.cancelled.emit();
  }

  protected save() {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.error.set(null);

    const { description, amount, dueDate, categoryId, supplierId, clientId, notes } = this.form.getRawValue();

    if (this.isEdit()) {
      this.accountService
        .update(this.accountId()!, {
          description, amount, dueDate, categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
        })
        .subscribe({
          next: () => { this.loading.set(false); this.saved.emit(); },
          error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar conta'); },
        });
    } else {
      const recurrence = this.showRecurrence()
        ? {
            frequency: this.recurrenceForm.getRawValue().frequency as 'MONTHLY' | 'WEEKLY' | 'BIWEEKLY' | 'YEARLY',
            endDate: this.recurrenceForm.getRawValue().endDate || undefined,
            maxOccurrences: this.recurrenceForm.getRawValue().maxOccurrences || undefined,
          }
        : undefined;

      this.accountService
        .create({ type: this.type(), description, amount, dueDate, categoryId,
          supplierId: supplierId || undefined,
          clientId: clientId || undefined,
          notes: notes || undefined,
          recurrence,
        })
        .subscribe({
          next: () => { this.loading.set(false); this.saved.emit(); },
          error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao criar conta'); },
        });
    }
  }
}
```

**Step 2: Create the HTML template**

```html
@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

<form [formGroup]="form" (ngSubmit)="save()">
  <div class="grid grid-cols-2 gap-4 mb-4">
    <div class="col-span-2">
      <label class="block text-sm font-medium text-gray-700 mb-1">Descricao <span class="text-red-500">*</span></label>
      <input formControlName="description" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Ex: Aluguel Escritorio" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Valor (R$) <span class="text-red-500">*</span></label>
      <input formControlName="amount" type="number" step="0.01" min="0.01"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Vencimento <span class="text-red-500">*</span></label>
      <input formControlName="dueDate" type="date"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
    </div>

    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Categoria <span class="text-red-500">*</span></label>
      <select formControlName="categoryId"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
        <option value="">Selecione...</option>
        @for (group of groups(); track group.id) {
          <optgroup [label]="group.name">
            @for (cat of group.categories; track cat.id) {
              <option [value]="cat.id">{{ cat.name }}</option>
            }
          </optgroup>
        }
      </select>
    </div>

    @if (isPayable) {
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Fornecedor <span class="text-red-500">*</span></label>
        <select formControlName="supplierId"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">Selecione...</option>
          @for (s of suppliers(); track s.id) {
            <option [value]="s.id">{{ s.name }}</option>
          }
        </select>
      </div>
    } @else {
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Cliente <span class="text-red-500">*</span></label>
        <select formControlName="clientId"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="">Selecione...</option>
          @for (c of clients(); track c.id) {
            <option [value]="c.id">{{ c.name }}</option>
          }
        </select>
      </div>
    }

    <div class="col-span-2">
      <label class="block text-sm font-medium text-gray-700 mb-1">Observacoes</label>
      <textarea formControlName="notes" rows="2"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Informacoes adicionais..."></textarea>
    </div>
  </div>

  @if (!isEdit()) {
    <div class="border-t border-gray-200 pt-4 mb-4">
      <button type="button" (click)="toggleRecurrence()"
        class="text-sm text-blue-600 hover:text-blue-700 font-medium">
        {{ showRecurrence() ? '- Remover recorrencia' : '+ Adicionar recorrencia' }}
      </button>
      @if (showRecurrence()) {
        <div class="mt-3 grid grid-cols-3 gap-3" [formGroup]="recurrenceForm">
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Frequencia</label>
            <select formControlName="frequency"
              class="w-full px-2 py-1.5 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
              <option value="MONTHLY">Mensal</option>
              <option value="WEEKLY">Semanal</option>
              <option value="BIWEEKLY">Quinzenal</option>
              <option value="YEARLY">Anual</option>
            </select>
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Data final</label>
            <input formControlName="endDate" type="date"
              class="w-full px-2 py-1.5 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Parcelas</label>
            <input formControlName="maxOccurrences" type="number" min="1" max="60"
              class="w-full px-2 py-1.5 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
        </div>
      }
    </div>
  }

  <div class="flex items-center gap-3 pt-2 border-t border-gray-100">
    <button type="submit" [disabled]="form.invalid || loading()"
      class="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150">
      {{ loading() ? 'Salvando...' : 'Salvar' }}
    </button>
    <button type="button" (click)="cancel()"
      class="px-5 py-2 border border-gray-200 text-sm text-gray-700 font-medium rounded-md hover:bg-gray-50 transition-colors duration-150">
      Cancelar
    </button>
  </div>
</form>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/accounts/components/
git commit -m "feat(accounts): add AccountDrawerFormComponent"
```

---

## Task 3: Update AccountListComponent to use the drawer

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.html`

**Step 1: Replace the TypeScript file**

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, OnInit, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { AccountType, AccountStatus } from '../../models/account.model';
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
    this.loading.set(true);
    const activeTabObj = this.tabs.find((t) => t.key === this.activeTab());
    const filters = activeTabObj?.statuses ? { status: activeTabObj.statuses } : undefined;
    this.accountService.loadAccounts(this.type, filters, page).subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar contas'); },
    });
  }
}
```

**Step 2: Replace the HTML template**

Replace the full HTML. Key changes: `<a routerLink>` buttons become `<button (click)>`, add drawer at the bottom.

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">{{ pageTitle() }}</h1>
    <p class="mt-1 text-sm text-gray-500">{{ pageSubtitle() }}</p>
  </div>
  <button
    (click)="openNew()"
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    <i class="pi pi-plus text-xs"></i>
    {{ newButtonLabel() }}
  </button>
</div>

@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

<!-- Tabs -->
<div class="flex gap-2 mb-6">
  @for (tab of tabs; track tab.key) {
    <button
      (click)="setTab(tab.key)"
      class="px-4 py-1.5 text-sm rounded-full font-medium transition-colors duration-150"
      [class.bg-blue-600]="activeTab() === tab.key"
      [class.text-white]="activeTab() === tab.key"
      [class.bg-gray-100]="activeTab() !== tab.key"
      [class.text-gray-600]="activeTab() !== tab.key"
    >
      {{ tab.label }}
    </button>
  }
</div>

@if (loading()) {
  <div class="text-sm text-gray-500">Carregando...</div>
} @else if (accounts().length === 0) {
  <div class="text-center py-12">
    <p class="text-gray-500 text-sm">Nenhuma conta encontrada.</p>
    <button (click)="openNew()" class="text-blue-600 hover:text-blue-700 text-sm font-medium mt-2 inline-block">
      Criar primeira conta
    </button>
  </div>
} @else {
  <div class="border border-gray-200 rounded-lg overflow-hidden">
    <table class="w-full text-sm">
      <thead class="bg-gray-50 border-b border-gray-200">
        <tr>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ entityLabel() }}</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Descricao</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Vencimento</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Categoria</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
          <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
          <th class="px-4 py-3"></th>
        </tr>
      </thead>
      <tbody>
        @for (account of accounts(); track account.id) {
          <tr class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-gray-900">
              {{ isPayable() ? (account.supplier?.name || '—') : (account.client?.name || '—') }}
            </td>
            <td class="px-4 py-3 text-gray-700">{{ account.description }}</td>
            <td class="px-4 py-3 text-gray-500">{{ formatDate(account.dueDate) }}</td>
            <td class="px-4 py-3">
              <span class="text-blue-600 text-xs font-medium">{{ account.category?.name || '—' }}</span>
            </td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full"
                [class.bg-amber-50]="account.status === 'PENDING'"
                [class.text-amber-700]="account.status === 'PENDING'"
                [class.bg-red-50]="account.status === 'OVERDUE'"
                [class.text-red-700]="account.status === 'OVERDUE'"
                [class.bg-emerald-50]="account.status === 'PAID' || account.status === 'RECEIVED'"
                [class.text-emerald-700]="account.status === 'PAID' || account.status === 'RECEIVED'"
                [class.bg-blue-50]="account.status === 'PARTIAL'"
                [class.text-blue-700]="account.status === 'PARTIAL'">
                <span class="w-1.5 h-1.5 rounded-full"
                  [class.bg-amber-500]="account.status === 'PENDING'"
                  [class.bg-red-500]="account.status === 'OVERDUE'"
                  [class.bg-emerald-500]="account.status === 'PAID' || account.status === 'RECEIVED'"
                  [class.bg-blue-500]="account.status === 'PARTIAL'"></span>
                {{ getStatusLabel(account.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-right font-mono"
              [class.text-red-600]="isPayable()"
              [class.text-emerald-600]="!isPayable()">
              {{ formatCurrency(account.amount) }}
            </td>
            <td class="px-4 py-3 text-right">
              <button (click)="openEdit(account.id)" class="text-gray-400 hover:text-gray-600 mr-2">
                <i class="pi pi-pencil text-sm"></i>
              </button>
              <button (click)="deleteAccount(account.id)" class="text-gray-400 hover:text-red-600">
                <i class="pi pi-trash text-sm"></i>
              </button>
            </td>
          </tr>
        }
      </tbody>
    </table>
  </div>

  @if (totalPages() > 1) {
    <div class="flex items-center justify-between mt-4">
      <p class="text-xs text-gray-500">{{ totalElements() }} resultado(s)</p>
      <div class="flex gap-1">
        @for (p of [].constructor(totalPages()); track $index) {
          <button (click)="goToPage($index)"
            class="w-8 h-8 text-xs rounded-md"
            [class.bg-blue-600]="currentPage() === $index"
            [class.text-white]="currentPage() === $index"
            [class.bg-gray-100]="currentPage() !== $index"
            [class.text-gray-600]="currentPage() !== $index">
            {{ $index + 1 }}
          </button>
        }
      </div>
    </div>
  }
}

<!-- Drawer -->
<app-drawer [open]="drawerOpen()" [title]="drawerTitle()" (closed)="closeDrawer()">
  <app-account-drawer-form
    [type]="type"
    [accountId]="editingId()"
    (saved)="onSaved()"
    (cancelled)="closeDrawer()"
  />
</app-drawer>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/
git commit -m "feat(accounts): integrate drawer into AccountListComponent"
```

---

## Task 4: Simplify accounts routes + remove old AccountFormComponent

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/accounts-payable/accounts-payable.routes.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/accounts-receivable/accounts-receivable.routes.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.html`

**Step 1: Replace accounts-payable.routes.ts**

```typescript
import { Routes } from '@angular/router';

export const accountsPayableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../accounts/pages/account-list/account-list.component').then(
        (m) => m.AccountListComponent,
      ),
    data: { type: 'PAYABLE' },
  },
];
```

**Step 2: Replace accounts-receivable.routes.ts**

```typescript
import { Routes } from '@angular/router';

export const accountsReceivableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('../accounts/pages/account-list/account-list.component').then(
        (m) => m.AccountListComponent,
      ),
    data: { type: 'RECEIVABLE' },
  },
];
```

**Step 3: Delete old AccountFormComponent files**

```bash
rm gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.ts
rm gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.html
```

**Step 4: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds with no references to AccountFormComponent

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/accounts-payable/accounts-payable.routes.ts
git add gestao-empresaial-frontend/src/app/features/accounts-receivable/accounts-receivable.routes.ts
git rm gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.ts
git rm gestao-empresaial-frontend/src/app/features/accounts/pages/account-form/account-form.component.html
git commit -m "refactor(accounts): remove form page routes, drawer handles create/edit"
```

---

## Task 5: SupplierDrawerFormComponent

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/components/supplier-drawer-form/supplier-drawer-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/suppliers/components/supplier-drawer-form/supplier-drawer-form.component.html`

**Step 1: Create TypeScript**

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { SupplierService } from '../../services/supplier.service';

@Component({
  selector: 'app-supplier-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './supplier-drawer-form.component.html',
})
export class SupplierDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly supplierService = inject(SupplierService);

  readonly supplierId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  constructor() {
    effect(() => {
      const id = this.supplierId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.supplierService.getById(id).subscribe({
          next: (s) => {
            this.form.patchValue({ name: s.name, document: s.document || '', email: s.email || '', phone: s.phone || '' });
            this.loading.set(false);
          },
          error: () => { this.error.set('Erro ao carregar fornecedor'); this.loading.set(false); },
        });
      } else {
        this.isEdit.set(false);
        this.form.reset({ name: '', document: '', email: '', phone: '' });
        this.error.set(null);
      }
    });
  }

  protected cancel() { this.cancelled.emit(); }

  protected save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { name, document, email, phone } = this.form.getRawValue();
    const data = { name, document: document || undefined, email: email || undefined, phone: phone || undefined };
    const request$ = this.isEdit()
      ? this.supplierService.update(this.supplierId()!, data)
      : this.supplierService.create(data);
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar fornecedor'); },
    });
  }
}
```

**Step 2: Create HTML**

```html
@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">{{ error() }}</div>
}

<form [formGroup]="form" (ngSubmit)="save()">
  <div class="flex flex-col gap-4 mb-6">
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Nome <span class="text-red-500">*</span></label>
      <input formControlName="name" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Nome do fornecedor" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">CNPJ / CPF</label>
      <input formControlName="document" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="00.000.000/0000-00" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
      <input formControlName="email" type="email"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="contato@empresa.com" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
      <input formControlName="phone" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="(11) 99999-9999" />
    </div>
  </div>

  <div class="flex items-center gap-3 pt-2 border-t border-gray-100">
    <button type="submit" [disabled]="form.invalid || loading()"
      class="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150">
      {{ loading() ? 'Salvando...' : 'Salvar' }}
    </button>
    <button type="button" (click)="cancel()"
      class="px-5 py-2 border border-gray-200 text-sm text-gray-700 font-medium rounded-md hover:bg-gray-50 transition-colors duration-150">
      Cancelar
    </button>
  </div>
</form>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/suppliers/components/
git commit -m "feat(suppliers): add SupplierDrawerFormComponent"
```

---

## Task 6: Update SupplierListComponent + routes

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-list/supplier-list.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-list/supplier-list.component.html`
- Modify: `gestao-empresaial-frontend/src/app/features/suppliers/suppliers.routes.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.html`

**Step 1: Replace supplier-list.component.ts**

```typescript
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
```

**Step 2: Update supplier-list.component.html**

Read the existing HTML first to preserve layout. The key changes:
- Replace `<a routerLink="/fornecedores/novo">` button with `<button (click)="openNew()">`
- Replace pencil `<a routerLink>` icons with `<button (click)="openEdit(supplier.id)">`
- Remove `RouterLink` usage
- Add `<app-drawer>` and `<app-supplier-drawer-form>` at the bottom

Find the "Novo Fornecedor" button in the existing HTML and change it:
```html
<!-- FROM: -->
<a routerLink="/fornecedores/novo" class="...">Novo Fornecedor</a>

<!-- TO: -->
<button (click)="openNew()" class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150">
  <i class="pi pi-plus text-xs"></i>
  Novo Fornecedor
</button>
```

Find edit links and change to buttons:
```html
<!-- FROM: -->
<a [routerLink]="['/fornecedores', supplier.id, 'editar']" class="..."><i class="pi pi-pencil"></i></a>

<!-- TO: -->
<button (click)="openEdit(supplier.id)" class="text-gray-400 hover:text-gray-600 mr-2">
  <i class="pi pi-pencil text-sm"></i>
</button>
```

Add at the very end of the HTML:
```html
<app-drawer [open]="drawerOpen()" [title]="drawerTitle()" (closed)="closeDrawer()">
  <app-supplier-drawer-form
    [supplierId]="editingId()"
    (saved)="onSaved()"
    (cancelled)="closeDrawer()"
  />
</app-drawer>
```

**Step 3: Simplify suppliers.routes.ts**

```typescript
import { Routes } from '@angular/router';

export const suppliersRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/supplier-list/supplier-list.component').then((m) => m.SupplierListComponent),
  },
];
```

**Step 4: Delete old SupplierFormComponent**

```bash
rm gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.ts
rm gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.html
```

**Step 5: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 6: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/suppliers/
git rm gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.ts
git rm gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-form/supplier-form.component.html
git commit -m "feat(suppliers): integrate drawer, remove form page route"
```

---

## Task 7: ClientDrawerFormComponent

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/clients/components/client-drawer-form/client-drawer-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/clients/components/client-drawer-form/client-drawer-form.component.html`

**Step 1: Create TypeScript**

Same pattern as SupplierDrawerFormComponent, using `ClientService`:

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ClientService } from '../../services/client.service';

@Component({
  selector: 'app-client-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './client-drawer-form.component.html',
})
export class ClientDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientService = inject(ClientService);

  readonly clientId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    document: [''],
    email: [''],
    phone: [''],
  });

  constructor() {
    effect(() => {
      const id = this.clientId();
      if (id) {
        this.isEdit.set(true);
        this.loading.set(true);
        this.clientService.getById(id).subscribe({
          next: (c) => {
            this.form.patchValue({ name: c.name, document: c.document || '', email: c.email || '', phone: c.phone || '' });
            this.loading.set(false);
          },
          error: () => { this.error.set('Erro ao carregar cliente'); this.loading.set(false); },
        });
      } else {
        this.isEdit.set(false);
        this.form.reset({ name: '', document: '', email: '', phone: '' });
        this.error.set(null);
      }
    });
  }

  protected cancel() { this.cancelled.emit(); }

  protected save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { name, document, email, phone } = this.form.getRawValue();
    const data = { name, document: document || undefined, email: email || undefined, phone: phone || undefined };
    const request$ = this.isEdit()
      ? this.clientService.update(this.clientId()!, data)
      : this.clientService.create(data);
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar cliente'); },
    });
  }
}
```

**Step 2: Create HTML** — identical to supplier-drawer-form.component.html but with "cliente" labels. Replace "fornecedor" → "cliente" and "Novo Fornecedor" placeholders accordingly.

```html
@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">{{ error() }}</div>
}

<form [formGroup]="form" (ngSubmit)="save()">
  <div class="flex flex-col gap-4 mb-6">
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Nome <span class="text-red-500">*</span></label>
      <input formControlName="name" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Nome do cliente" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">CNPJ / CPF</label>
      <input formControlName="document" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="00.000.000/0000-00" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
      <input formControlName="email" type="email"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="contato@empresa.com" />
    </div>
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Telefone</label>
      <input formControlName="phone" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="(11) 99999-9999" />
    </div>
  </div>
  <div class="flex items-center gap-3 pt-2 border-t border-gray-100">
    <button type="submit" [disabled]="form.invalid || loading()"
      class="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150">
      {{ loading() ? 'Salvando...' : 'Salvar' }}
    </button>
    <button type="button" (click)="cancel()"
      class="px-5 py-2 border border-gray-200 text-sm text-gray-700 font-medium rounded-md hover:bg-gray-50 transition-colors duration-150">
      Cancelar
    </button>
  </div>
</form>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/clients/components/
git commit -m "feat(clients): add ClientDrawerFormComponent"
```

---

## Task 8: Update ClientListComponent + routes

Same pattern as Task 6 (Suppliers), applied to Clients.

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/clients/pages/client-list/client-list.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/clients/pages/client-list/client-list.component.html`
- Modify: `gestao-empresaial-frontend/src/app/features/clients/clients.routes.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.ts`
- Delete: `gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.html`

**Step 1: Replace client-list.component.ts**

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
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
  protected readonly drawerTitle = () => this.editingId() ? 'Editar Cliente' : 'Novo Cliente';

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
```

**Step 2: Update client-list.component.html** — same changes as supplier list:
- "Novo Cliente" `<a routerLink>` → `<button (click)="openNew()">`
- Edit `<a routerLink>` → `<button (click)="openEdit(client.id)">`
- Add `<app-drawer>` + `<app-client-drawer-form>` at the bottom

**Step 3: Simplify clients.routes.ts**

```typescript
import { Routes } from '@angular/router';

export const clientsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/client-list/client-list.component').then((m) => m.ClientListComponent),
  },
];
```

**Step 4: Delete old ClientFormComponent**

```bash
rm gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.ts
rm gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.html
```

**Step 5: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 6: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/clients/
git rm gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.ts
git rm gestao-empresaial-frontend/src/app/features/clients/pages/client-form/client-form.component.html
git commit -m "feat(clients): integrate drawer, remove form page route"
```

---

## Task 9: CategoryDrawerFormComponent

The drawer form handles creating/editing a **category group** (the top-level concept in the UI, labelled "Categoria").

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/categories/components/category-drawer-form/category-drawer-form.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/categories/components/category-drawer-form/category-drawer-form.component.html`

**Step 1: Create TypeScript**

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, input, output, effect } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-category-drawer-form',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-drawer-form.component.html',
})
export class CategoryDrawerFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly categoryService = inject(CategoryService);

  readonly groupId = input<string | null>(null);
  readonly saved = output<void>();
  readonly cancelled = output<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly isEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    type: ['EXPENSE' as 'REVENUE' | 'EXPENSE', [Validators.required]],
  });

  constructor() {
    effect(() => {
      const id = this.groupId();
      if (id) {
        this.isEdit.set(true);
        const group = this.categoryService.groups().find((g) => g.id === id);
        if (group) {
          this.form.patchValue({ name: group.name, type: group.type });
          this.form.get('type')?.disable();
        }
      } else {
        this.isEdit.set(false);
        this.form.reset({ name: '', type: 'EXPENSE' });
        this.form.get('type')?.enable();
        this.error.set(null);
      }
    });
  }

  protected cancel() { this.cancelled.emit(); }

  protected save() {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);
    const { name, type } = this.form.getRawValue();
    const request$ = this.isEdit()
      ? this.categoryService.updateGroup(this.groupId()!, { name })
      : this.categoryService.createGroup({ name, type });
    request$.subscribe({
      next: () => { this.loading.set(false); this.saved.emit(); },
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao salvar categoria'); },
    });
  }
}
```

**Step 2: Create HTML**

```html
@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">{{ error() }}</div>
}

<form [formGroup]="form" (ngSubmit)="save()">
  <div class="flex flex-col gap-4 mb-6">
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Nome do grupo <span class="text-red-500">*</span></label>
      <input formControlName="name" type="text"
        class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Ex: Despesas Operacionais" />
    </div>
    @if (!isEdit()) {
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Tipo <span class="text-red-500">*</span></label>
        <select formControlName="type"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500">
          <option value="EXPENSE">Despesa</option>
          <option value="REVENUE">Receita</option>
        </select>
      </div>
    }
  </div>
  <div class="flex items-center gap-3 pt-2 border-t border-gray-100">
    <button type="submit" [disabled]="form.invalid || loading()"
      class="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150">
      {{ loading() ? 'Salvando...' : 'Salvar' }}
    </button>
    <button type="button" (click)="cancel()"
      class="px-5 py-2 border border-gray-200 text-sm text-gray-700 font-medium rounded-md hover:bg-gray-50 transition-colors duration-150">
      Cancelar
    </button>
  </div>
</form>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/categories/components/
git commit -m "feat(categories): add CategoryDrawerFormComponent"
```

---

## Task 10: Update CategoryManagementComponent + routes

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.html`

**Step 1: Update category-management.component.ts**

Add drawer signals and remove `showNewGroupForm` / `groupForm` (the drawer handles this now):

```typescript
import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { CategoryService } from '../../services/category.service';
import { DrawerComponent } from '../../../../shared/ui/drawer/drawer.component';
import { CategoryDrawerFormComponent } from '../../components/category-drawer-form/category-drawer-form.component';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule, DrawerComponent, CategoryDrawerFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  protected readonly categoryService = inject(CategoryService);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly addingCategoryToGroup = signal<string | null>(null);
  protected readonly drawerOpen = signal(false);
  protected readonly editingGroupId = signal<string | null>(null);
  protected readonly drawerTitle = () => this.editingGroupId() ? 'Editar Categoria' : 'Nova Categoria';

  ngOnInit() {
    this.loadData();
  }

  private loadData() {
    this.categoryService.loadGroups().subscribe({
      next: () => this.loading.set(false),
      error: (err) => { this.loading.set(false); this.error.set(err.error?.message || 'Erro ao carregar categorias'); },
    });
  }

  protected openNew() { this.editingGroupId.set(null); this.drawerOpen.set(true); }
  protected openEdit(groupId: string) { this.editingGroupId.set(groupId); this.drawerOpen.set(true); }
  protected closeDrawer() { this.drawerOpen.set(false); this.editingGroupId.set(null); }

  protected onSaved() {
    this.closeDrawer();
    this.loadData();
  }

  protected deleteGroup(groupId: string) {
    if (!confirm('Excluir este grupo?')) return;
    this.categoryService.deleteGroup(groupId).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir grupo'),
    });
  }

  protected startAddCategory(groupId: string) { this.addingCategoryToGroup.set(groupId); }
  protected cancelAddCategory() { this.addingCategoryToGroup.set(null); }

  protected createCategory(groupId: string, name: string) {
    if (!name.trim()) return;
    this.categoryService.createCategory({ groupId, name: name.trim() }).subscribe({
      next: () => { this.addingCategoryToGroup.set(null); this.loadData(); },
      error: (err) => this.error.set(err.error?.message || 'Erro ao criar categoria'),
    });
  }

  protected deleteCategory(categoryId: string) {
    this.categoryService.deleteCategory(categoryId).subscribe({
      next: () => this.loadData(),
      error: (err) => this.error.set(err.error?.message || 'Erro ao excluir categoria'),
    });
  }

  protected typeLabel(type: string): string {
    return type === 'REVENUE' ? 'Receita' : 'Despesa';
  }
}
```

**Step 2: Update category-management.component.html**

Read the existing HTML. Key changes:
- Remove the inline "Nova Categoria" form section (the `showNewGroupForm` block)
- Replace "Nova Categoria" button with `<button (click)="openNew()">`
- Add edit button (pencil) per group row: `<button (click)="openEdit(group.id)">`
- Add drawer at the bottom

```html
<!-- Add to group header action buttons: -->
<button (click)="openEdit(group.id)" class="text-gray-400 hover:text-gray-600">
  <i class="pi pi-pencil text-sm"></i>
</button>

<!-- Add at end of file: -->
<app-drawer [open]="drawerOpen()" [title]="drawerTitle()" (closed)="closeDrawer()">
  <app-category-drawer-form
    [groupId]="editingGroupId()"
    (saved)="onSaved()"
    (cancelled)="closeDrawer()"
  />
</app-drawer>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/categories/
git commit -m "feat(categories): integrate drawer into CategoryManagementComponent"
```

---

## Task 11: Final build + test verification

**Step 1: Run full frontend build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds, no bundle size warnings

**Step 2: Run frontend tests**

Run: `cd gestao-empresaial-frontend && npm test`
Expected: All tests pass

**Step 3: Run backend tests** (no backend changes but verify nothing broken)

Run: `cd gestao-empresarial-backend && JAVA_HOME=/home/danilo/.jdks/openjdk-25.0.2 ./mvnw test`
Expected: 65 tests pass

**Step 4: Final commit if any cleanup needed**

```bash
git add -A
git commit -m "chore: final drawer refactoring cleanup"
```

---

## Summary

| Task | Description | New Files | Modified | Deleted |
|------|-------------|-----------|----------|---------|
| 1 | DrawerComponent | 1 | — | — |
| 2 | AccountDrawerFormComponent | 2 | — | — |
| 3 | AccountListComponent drawer integration | — | 2 | — |
| 4 | Accounts routes simplification | — | 2 | 2 |
| 5 | SupplierDrawerFormComponent | 2 | — | — |
| 6 | SupplierListComponent + routes | — | 3 | 2 |
| 7 | ClientDrawerFormComponent | 2 | — | — |
| 8 | ClientListComponent + routes | — | 3 | 2 |
| 9 | CategoryDrawerFormComponent | 2 | — | — |
| 10 | CategoryManagementComponent | — | 2 | — |
| 11 | Final verification | — | — | — |
