# UI Refactoring Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor all existing frontend pages to match the target design mockups in `docs/UI/`.

**Architecture:** Pure frontend refactoring — no backend changes. Existing component structure stays the same. Sidebar gets dark theme, all pages get consistent header pattern with subtitle, supplier list switches from table to card grid, and three new placeholder pages are added for routes that exist in the sidebar but have no implementation yet.

**Tech Stack:** Angular 21 (standalone components, signals, OnPush), Tailwind CSS 4, PrimeIcons

**Frontend directory:** `gestao-empresaial-frontend/` (note: typo in folder name is intentional)

**Angular conventions:** `inject()` for DI, `signal()` for state, `ChangeDetectionStrategy.OnPush`, native control flow (`@if`, `@for`), `protected readonly` for template members, host bindings in `host` object.

---

## Design Reference Summary (from `docs/UI/` screenshots)

**Sidebar:** Dark background (~`#1E1E2D`), white text, blue "F" logo, section headers (FINANCEIRO, CADASTROS, CONFIGURACOES), user avatar+name+email at bottom.

**Page pattern:** Title (text-2xl bold) + subtitle (text-sm gray-500) on left, primary action button (blue-600 + icon) on right. Content below in white cards with borders.

**Footer:** "© 2024 FinDash Tecnologia. Todos os direitos reservados." centered, gray-400 text.

**Key pages:**
- **Dashboard:** 4 KPI cards (Saldo, Receitas, Despesas, A Receber) + bar chart + quick actions
- **Fornecedores:** Card grid (2 cols) with avatar initial, name, segment, email/phone icons
- **Clientes:** Table with Nome, Contato, Status (badge), Receita Total (monospace)
- **Categorias:** Flat card grid with icon + name + edit button (no group accordion)
- **Empresa:** 2x2 form grid (Razao Social, Nome Fantasia, CNPJ, E-mail Financeiro)
- **Usuarios:** Avatar list with initials circle, name, email, role badge, settings icon
- **Contas a Pagar:** Table with tabs (Todos, Pendentes, Pagos, Atrasados) + status badges
- **Contas a Receber:** Table with status badges + positive values
- **Importacao OFX:** Upload area + recent imports list

---

### Task 1: Sidebar — Dark Theme with Sections and User Info

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/layout/sidebar/sidebar.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/layout/sidebar/sidebar.component.html`

**Step 1: Rewrite sidebar component TypeScript**

Replace the full content of `sidebar.component.ts` with:

```typescript
import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block w-64 min-h-screen bg-[#1E1E2D] flex flex-col' },
  templateUrl: './sidebar.component.html',
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  protected readonly sections: NavSection[] = [
    {
      title: 'FINANCEIRO',
      items: [
        { label: 'Dashboard', icon: 'pi pi-chart-bar', route: '/dashboard' },
        { label: 'Contas a Pagar', icon: 'pi pi-arrow-up-right', route: '/contas-a-pagar' },
        { label: 'Contas a Receber', icon: 'pi pi-arrow-down-left', route: '/contas-a-receber' },
        { label: 'Importacao OFX', icon: 'pi pi-upload', route: '/importacao' },
      ],
    },
    {
      title: 'CADASTROS',
      items: [
        { label: 'Categorias', icon: 'pi pi-tags', route: '/categorias' },
        { label: 'Fornecedores', icon: 'pi pi-building', route: '/fornecedores' },
        { label: 'Clientes', icon: 'pi pi-users', route: '/clientes' },
      ],
    },
    {
      title: 'CONFIGURACOES',
      items: [
        { label: 'Empresa', icon: 'pi pi-cog', route: '/configuracoes' },
        { label: 'Usuarios', icon: 'pi pi-user-edit', route: '/configuracoes/usuarios' },
      ],
    },
  ];

  protected get userName(): string {
    return this.authService.userName ?? 'Usuario';
  }

  protected get userEmail(): string {
    return this.authService.userEmail ?? '';
  }

  protected get userInitials(): string {
    const name = this.userName;
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }
}
```

**Step 2: Rewrite sidebar template**

Replace the full content of `sidebar.component.html` with:

```html
<!-- Logo -->
<div class="p-6 flex items-center gap-3">
  <div class="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
    <span class="text-white font-bold text-sm">F</span>
  </div>
  <span class="text-white text-lg font-bold">FinDash</span>
</div>

<!-- Navigation -->
<nav class="flex-1 px-3 space-y-6">
  @for (section of sections; track section.title) {
    <div>
      <p class="px-3 mb-2 text-xs font-semibold tracking-wider text-gray-500">
        {{ section.title }}
      </p>
      @for (item of section.items; track item.route) {
        <a
          [routerLink]="item.route"
          routerLinkActive="bg-white/10 text-white"
          [routerLinkActiveOptions]="{ exact: item.route === '/dashboard' }"
          class="flex items-center gap-3 px-3 py-2 rounded-md text-sm text-gray-400 hover:bg-white/5 hover:text-gray-200 mb-0.5 transition-colors duration-150"
        >
          <i [class]="item.icon + ' text-base'"></i>
          {{ item.label }}
        </a>
      }
    </div>
  }
</nav>

<!-- User Info -->
<div class="p-4 border-t border-white/10">
  <div class="flex items-center gap-3">
    <div class="w-9 h-9 rounded-full bg-gray-600 flex items-center justify-center">
      <span class="text-white text-xs font-medium">{{ userInitials }}</span>
    </div>
    <div class="min-w-0">
      <p class="text-sm text-white font-medium truncate">{{ userName }}</p>
      <p class="text-xs text-gray-500 truncate">{{ userEmail }}</p>
    </div>
  </div>
</div>
```

**Step 3: Add `userName` and `userEmail` getters to AuthService (if missing)**

Check `gestao-empresaial-frontend/src/app/core/services/auth.service.ts` for `userName` and `userEmail` properties. If they don't exist, add computed getters that read from the stored user data. The sidebar needs these to display user info at the bottom.

Look for how user data is stored (likely a signal or localStorage). Add:

```typescript
get userName(): string | null {
  return this._user()?.name ?? localStorage.getItem('userName');
}

get userEmail(): string | null {
  return this._user()?.email ?? localStorage.getItem('userEmail');
}
```

Adapt to the actual AuthService structure found in the file.

**Step 4: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds with no errors.

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/layout/sidebar/ gestao-empresaial-frontend/src/app/core/services/auth.service.ts
git commit -m "refactor(sidebar): dark theme with section headers and user info"
```

---

### Task 2: Footer + Main Layout Update

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/layout/main-layout/main-layout.component.html`
- Modify: `gestao-empresaial-frontend/src/app/layout/main-layout/main-layout.component.ts`

**Step 1: Add footer to main layout template**

Replace `main-layout.component.html` with:

```html
<div class="flex min-h-screen">
  <app-sidebar />
  <div class="flex-1 flex flex-col bg-white">
    <app-header />
    <main class="flex-1 p-8">
      <div class="max-w-7xl mx-auto">
        <router-outlet />
      </div>
    </main>
    <footer class="py-6 text-center text-sm text-gray-400">
      &copy; 2024 FinDash Tecnologia. Todos os direitos reservados.
    </footer>
  </div>
</div>
```

No changes needed in the TypeScript file.

**Step 2: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/layout/main-layout/
git commit -m "refactor(layout): add footer and white background to content area"
```

---

### Task 3: Dashboard Redesign

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/dashboard/pages/dashboard/dashboard.component.html`
- Modify: `gestao-empresaial-frontend/src/app/features/dashboard/pages/dashboard/dashboard.component.ts`

**Step 1: Rewrite dashboard component**

Replace `dashboard.component.ts` with:

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  protected readonly kpis = [
    { label: 'Saldo Atual', value: 'R$ 124.500', icon: 'pi pi-wallet', change: '+12%', positive: true },
    { label: 'Receitas', value: 'R$ 45.230', icon: 'pi pi-file', change: '+8.1%', positive: true },
    { label: 'Despesas', value: 'R$ 18.400', icon: 'pi pi-credit-card', change: '+2.3%', positive: false },
    { label: 'A Receber', value: 'R$ 8.150', icon: 'pi pi-clock', change: '7 dias', positive: null },
  ];

  protected readonly months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun'];
}
```

**Step 2: Rewrite dashboard template**

Replace `dashboard.component.html` with:

```html
<!-- Page Header -->
<div class="mb-8">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Visao Geral</h1>
      <p class="mt-1 text-sm text-gray-500">Resumo financeiro da empresa em tempo real.</p>
    </div>
    <select class="px-3 py-2 border border-gray-200 rounded-md text-sm text-gray-700 bg-white">
      <option>Este Mes</option>
      <option>Ultimo Mes</option>
      <option>Ultimos 3 Meses</option>
    </select>
  </div>
</div>

<!-- KPI Cards -->
<div class="grid grid-cols-4 gap-4 mb-8">
  @for (kpi of kpis; track kpi.label) {
    <div class="border border-gray-200 rounded-lg p-5">
      <div class="flex items-center justify-between mb-3">
        <div class="w-10 h-10 bg-gray-50 rounded-lg flex items-center justify-center">
          <i [class]="kpi.icon + ' text-gray-500'"></i>
        </div>
        @if (kpi.positive !== null) {
          <span
            class="text-xs font-medium"
            [class.text-emerald-600]="kpi.positive"
            [class.text-red-600]="!kpi.positive"
          >
            {{ kpi.change }}
          </span>
        } @else {
          <span class="text-xs text-gray-400">{{ kpi.change }}</span>
        }
      </div>
      <p class="text-xs text-gray-500 mb-1">{{ kpi.label }}</p>
      <p class="text-xl font-bold text-gray-900 font-mono">{{ kpi.value }}</p>
    </div>
  }
</div>

<!-- Content Grid -->
<div class="grid grid-cols-3 gap-6">
  <!-- Cash Flow Chart -->
  <div class="col-span-2 border border-gray-200 rounded-lg p-6">
    <h2 class="text-base font-semibold text-gray-900 mb-6">Fluxo de Caixa</h2>
    <div class="flex items-end gap-4 h-48">
      @for (month of months; track month) {
        <div class="flex-1 flex flex-col items-center gap-2">
          <div class="w-full bg-blue-600/80 rounded-t-sm" [style.height.px]="60 + ($index * 20)"></div>
          <span class="text-xs text-gray-500">{{ month }}</span>
        </div>
      }
    </div>
  </div>

  <!-- Quick Actions -->
  <div class="border border-gray-200 rounded-lg p-6">
    <h2 class="text-base font-semibold text-gray-900 mb-6">Acoes Rapidas</h2>
    <div class="space-y-3">
      <button
        class="w-full flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors duration-150 text-left"
      >
        <div class="w-10 h-10 bg-emerald-50 rounded-lg flex items-center justify-center">
          <i class="pi pi-plus text-emerald-600"></i>
        </div>
        <div>
          <p class="text-sm font-medium text-gray-900">Nova Receita</p>
          <p class="text-xs text-gray-500">Registrar entrada manual</p>
        </div>
      </button>
      <button
        class="w-full flex items-center gap-3 p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors duration-150 text-left"
      >
        <div class="w-10 h-10 bg-red-50 rounded-lg flex items-center justify-center">
          <i class="pi pi-plus text-red-600"></i>
        </div>
        <div>
          <p class="text-sm font-medium text-gray-900">Nova Despesa</p>
          <p class="text-xs text-gray-500">Registrar saida manual</p>
        </div>
      </button>
    </div>
  </div>
</div>
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/dashboard/
git commit -m "refactor(dashboard): add KPI cards, chart placeholder, and quick actions"
```

---

### Task 4: Fornecedores List — Card Grid Layout

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/suppliers/pages/supplier-list/supplier-list.component.html`

**Step 1: Rewrite supplier list template**

Replace `supplier-list.component.html` with a card grid layout:

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Fornecedores</h1>
    <p class="mt-1 text-sm text-gray-500">Base de dados de parceiros comerciais.</p>
  </div>
  <a
    routerLink="novo"
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    Novo Fornecedor
  </a>
</div>

@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

@if (loading()) {
  <div class="text-sm text-gray-500">Carregando...</div>
} @else if (suppliers().length === 0) {
  <div class="text-center py-12">
    <p class="text-gray-500 text-sm">Nenhum fornecedor cadastrado.</p>
    <a routerLink="novo" class="text-blue-600 hover:text-blue-700 text-sm font-medium mt-2 inline-block">
      Cadastrar primeiro fornecedor
    </a>
  </div>
} @else {
  <div class="grid grid-cols-2 gap-4">
    @for (supplier of suppliers(); track supplier.id) {
      <div class="border border-gray-200 rounded-lg p-5 hover:border-gray-300 transition-colors duration-150">
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
              <span class="text-sm font-semibold text-gray-600">
                {{ supplier.name.charAt(0).toUpperCase() }}
              </span>
            </div>
            <div>
              <p class="text-sm font-semibold text-gray-900">{{ supplier.name }}</p>
              @if (supplier.segment) {
                <p class="text-xs text-gray-500">{{ supplier.segment }}</p>
              }
            </div>
          </div>
          <div class="relative">
            <button
              (click)="toggleMenu(supplier.id)"
              class="p-1 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-600"
            >
              <i class="pi pi-ellipsis-h text-sm"></i>
            </button>
            @if (openMenuId() === supplier.id) {
              <div class="absolute right-0 mt-1 w-32 bg-white border border-gray-200 rounded-md shadow-lg z-10">
                <a
                  [routerLink]="[supplier.id, 'editar']"
                  class="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
                >
                  Editar
                </a>
                <button
                  (click)="deleteSupplier(supplier)"
                  class="block w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                >
                  Excluir
                </button>
              </div>
            }
          </div>
        </div>
        <div class="space-y-1.5">
          @if (supplier.email) {
            <div class="flex items-center gap-2 text-xs text-gray-500">
              <i class="pi pi-envelope text-xs"></i>
              <span>{{ supplier.email }}</span>
            </div>
          }
          @if (supplier.phone) {
            <div class="flex items-center gap-2 text-xs text-gray-500">
              <i class="pi pi-phone text-xs"></i>
              <span>{{ supplier.phone }}</span>
            </div>
          }
        </div>
      </div>
    }
  </div>
}
```

**Step 2: Add menu toggle signal to supplier-list component**

In `supplier-list.component.ts`, add:

```typescript
protected readonly openMenuId = signal<string | null>(null);

protected toggleMenu(id: string) {
  this.openMenuId.update((current) => (current === id ? null : id));
}
```

Make sure `signal` is imported from `@angular/core`.

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/suppliers/
git commit -m "refactor(suppliers): switch list from table to card grid layout"
```

---

### Task 5: Clientes List — Updated Table Columns

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/clients/pages/client-list/client-list.component.html`

**Step 1: Rewrite client list template**

Replace `client-list.component.html` with:

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Clientes</h1>
    <p class="mt-1 text-sm text-gray-500">Gestao de carteira de clientes.</p>
  </div>
  <a
    routerLink="novo"
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    Novo Cliente
  </a>
</div>

@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

@if (loading()) {
  <div class="text-sm text-gray-500">Carregando...</div>
} @else if (clients().length === 0) {
  <div class="text-center py-12">
    <p class="text-gray-500 text-sm">Nenhum cliente cadastrado.</p>
    <a routerLink="novo" class="text-blue-600 hover:text-blue-700 text-sm font-medium mt-2 inline-block">
      Cadastrar primeiro cliente
    </a>
  </div>
} @else {
  <div class="border border-gray-200 rounded-lg overflow-hidden">
    <table class="w-full text-sm">
      <thead class="bg-gray-50 border-b border-gray-200">
        <tr>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Nome</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Contato</th>
          <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
          <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Receita Total</th>
          <th class="px-4 py-3"></th>
        </tr>
      </thead>
      <tbody>
        @for (client of clients(); track client.id) {
          <tr class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-gray-900">{{ client.name }}</td>
            <td class="px-4 py-3 text-gray-500">{{ client.email || '—' }}</td>
            <td class="px-4 py-3">
              <span class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full bg-emerald-50 text-emerald-700">
                <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                Ativo
              </span>
            </td>
            <td class="px-4 py-3 text-right font-mono text-gray-900">R$ 0,00</td>
            <td class="px-4 py-3 text-right">
              <a [routerLink]="[client.id, 'editar']" class="text-gray-400 hover:text-gray-600 mr-2">
                <i class="pi pi-pencil text-sm"></i>
              </a>
              <button (click)="deleteClient(client)" class="text-gray-400 hover:text-red-600">
                <i class="pi pi-trash text-sm"></i>
              </button>
            </td>
          </tr>
        }
      </tbody>
    </table>
  </div>
}
```

**Step 2: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/clients/
git commit -m "refactor(clients): update table columns to match design (contato, status, receita)"
```

---

### Task 6: Categorias — Flat Card Grid

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.html`
- Modify: `gestao-empresaial-frontend/src/app/features/categories/pages/category-management/category-management.component.ts`

**Step 1: Update component TypeScript**

In `category-management.component.ts`, ensure the component has a signal for showing the new category form:

Check existing signals. The component needs:
- `showNewCategoryForm` signal
- `newCategoryName` signal or form control
- `newCategoryGroupId` signal (to pick which group the category belongs to)

Keep existing service injection and group loading logic. Add a `selectedGroupForNew` signal if needed.

**Step 2: Rewrite categories template**

Replace `category-management.component.html` with:

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Categorias</h1>
    <p class="mt-1 text-sm text-gray-500">Organize suas transacoes por tipo.</p>
  </div>
  <button
    (click)="toggleNewGroupForm()"
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    Nova Categoria
  </button>
</div>

@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

<!-- New Group Form (inline) -->
@if (showNewGroupForm()) {
  <div class="mb-6 border border-gray-200 rounded-lg p-4">
    <form [formGroup]="groupForm" (ngSubmit)="createGroup()" class="flex items-end gap-3">
      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">Nome do grupo</label>
        <input
          formControlName="name"
          type="text"
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          placeholder="Ex: Infraestrutura"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Tipo</label>
        <select
          formControlName="type"
          class="px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="EXPENSE">Despesa</option>
          <option value="REVENUE">Receita</option>
        </select>
      </div>
      <button
        type="submit"
        [disabled]="groupForm.invalid"
        class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50"
      >
        Criar
      </button>
      <button
        type="button"
        (click)="toggleNewGroupForm()"
        class="px-4 py-2 border border-gray-200 text-sm text-gray-700 rounded-md hover:bg-gray-50"
      >
        Cancelar
      </button>
    </form>
  </div>
}

@if (loading()) {
  <div class="text-sm text-gray-500">Carregando...</div>
} @else if (categoryService.groups().length === 0) {
  <div class="text-center py-12">
    <p class="text-gray-500 text-sm">Nenhuma categoria cadastrada.</p>
  </div>
} @else {
  <!-- Groups as sections with category cards -->
  @for (group of categoryService.groups(); track group.id) {
    <div class="mb-8">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <h2 class="text-sm font-semibold text-gray-900">{{ group.name }}</h2>
          <span
            class="text-xs px-2 py-0.5 rounded-full font-medium"
            [class.bg-emerald-50]="group.type === 'REVENUE'"
            [class.text-emerald-700]="group.type === 'REVENUE'"
            [class.bg-red-50]="group.type === 'EXPENSE'"
            [class.text-red-700]="group.type === 'EXPENSE'"
          >
            {{ group.type === 'REVENUE' ? 'Receita' : 'Despesa' }}
          </span>
        </div>
        <div class="flex items-center gap-2">
          <button
            (click)="startAddCategory(group.id)"
            class="text-xs text-blue-600 hover:text-blue-700 font-medium"
          >
            + Adicionar
          </button>
          <button
            (click)="deleteGroup(group.id)"
            class="text-xs text-gray-400 hover:text-red-600"
          >
            <i class="pi pi-trash"></i>
          </button>
        </div>
      </div>

      <!-- Inline add category -->
      @if (addingCategoryToGroup() === group.id) {
        <div class="mb-3 flex items-center gap-2">
          <input
            #categoryInput
            type="text"
            (keyup.enter)="createCategory(group.id, categoryInput.value); categoryInput.value = ''"
            (keyup.escape)="cancelAddCategory()"
            class="px-3 py-1.5 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Nome da categoria"
          />
          <button
            (click)="createCategory(group.id, categoryInput.value); categoryInput.value = ''"
            class="px-3 py-1.5 bg-blue-600 text-white text-xs rounded-md hover:bg-blue-700"
          >
            Criar
          </button>
          <button
            (click)="cancelAddCategory()"
            class="px-3 py-1.5 text-xs text-gray-500 hover:text-gray-700"
          >
            Cancelar
          </button>
        </div>
      }

      <!-- Category cards grid -->
      @if (group.categories.length === 0) {
        <p class="text-xs text-gray-400">Nenhuma categoria neste grupo.</p>
      } @else {
        <div class="grid grid-cols-3 gap-3">
          @for (category of group.categories; track category.id) {
            <div class="flex items-center justify-between border border-gray-200 rounded-lg px-4 py-3 hover:border-gray-300 transition-colors duration-150">
              <div class="flex items-center gap-3">
                <div
                  class="w-8 h-8 rounded-lg flex items-center justify-center"
                  [class.bg-emerald-50]="group.type === 'REVENUE'"
                  [class.bg-red-50]="group.type === 'EXPENSE'"
                >
                  <i
                    class="pi pi-tag text-sm"
                    [class.text-emerald-600]="group.type === 'REVENUE'"
                    [class.text-red-600]="group.type === 'EXPENSE'"
                  ></i>
                </div>
                <span class="text-sm text-gray-900">{{ category.name }}</span>
              </div>
              <button
                (click)="deleteCategory(category.id)"
                class="text-gray-300 hover:text-red-500 transition-colors duration-150"
              >
                <i class="pi pi-pencil text-sm"></i>
              </button>
            </div>
          }
        </div>
      }
    </div>
  }
}
```

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/categories/
git commit -m "refactor(categories): switch to card grid layout with grouped sections"
```

---

### Task 7: Empresa Settings — Updated Layout

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/company/pages/company-settings/company-settings.component.html`

**Step 1: Rewrite company settings template**

Replace `company-settings.component.html` with a 2-column grid form matching the design:

```html
<!-- Page Header -->
<div class="mb-6">
  <h1 class="text-2xl font-bold text-gray-900">Configuracoes da Empresa</h1>
  <p class="mt-1 text-sm text-gray-500">Dados cadastrais da sua organizacao.</p>
</div>

@if (success()) {
  <div class="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-md text-sm text-emerald-600">
    Alteracoes salvas com sucesso.
  </div>
}
@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}

<form [formGroup]="form" (ngSubmit)="save()" class="border border-gray-200 rounded-lg p-6">
  <div class="grid grid-cols-2 gap-6 mb-6">
    <div>
      <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">Razao Social</label>
      <input
        formControlName="name"
        type="text"
        class="w-full px-3 py-2.5 border border-gray-200 rounded-md text-sm text-gray-900 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent focus:bg-white"
      />
    </div>
    <div>
      <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">Nome Fantasia</label>
      <input
        formControlName="segment"
        type="text"
        class="w-full px-3 py-2.5 border border-gray-200 rounded-md text-sm text-gray-900 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent focus:bg-white"
      />
    </div>
    <div>
      <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">CNPJ</label>
      <input
        formControlName="cnpj"
        type="text"
        placeholder="00.000.000/0000-00"
        class="w-full px-3 py-2.5 border border-gray-200 rounded-md text-sm text-gray-900 bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent focus:bg-white"
      />
    </div>
    <div>
      <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">E-mail Financeiro</label>
      <input
        type="email"
        disabled
        placeholder="Em breve"
        class="w-full px-3 py-2.5 border border-gray-200 rounded-md text-sm text-gray-400 bg-gray-50 cursor-not-allowed"
      />
    </div>
  </div>

  <div class="flex justify-center">
    <button
      type="submit"
      [disabled]="form.invalid || loading()"
      class="px-6 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-150"
    >
      {{ loading() ? 'Salvando...' : 'Salvar Alteracoes' }}
    </button>
  </div>
</form>
```

**Step 2: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/company/pages/company-settings/
git commit -m "refactor(company): update settings page to 2x2 grid layout"
```

---

### Task 8: Usuarios — Avatar List Layout

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/company/pages/user-management/user-management.component.html`

**Step 1: Rewrite user management template**

Replace `user-management.component.html` with:

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Usuarios</h1>
    <p class="mt-1 text-sm text-gray-500">Gerencie o acesso ao sistema.</p>
  </div>
  <button
    (click)="toggleInviteForm()"
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    Convidar Usuario
  </button>
</div>

@if (error()) {
  <div class="mb-4 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-600">
    {{ error() }}
  </div>
}
@if (success()) {
  <div class="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-md text-sm text-emerald-600">
    {{ success() }}
  </div>
}

<!-- Invite Form (collapsible) -->
@if (showInviteForm()) {
  <div class="mb-6 border border-gray-200 rounded-lg p-4">
    <form (ngSubmit)="inviteMember()" class="flex items-end gap-3">
      <div class="flex-1">
        <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
        <input
          [(ngModel)]="inviteEmail"
          name="email"
          type="email"
          required
          class="w-full px-3 py-2 border border-gray-200 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="email@exemplo.com"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Papel</label>
        <select
          [(ngModel)]="inviteRole"
          name="role"
          class="px-3 py-2 border border-gray-200 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="EDITOR">Editor</option>
          <option value="VIEWER">Visualizador</option>
        </select>
      </div>
      <button
        type="submit"
        class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700"
      >
        Convidar
      </button>
      <button
        type="button"
        (click)="toggleInviteForm()"
        class="px-4 py-2 border border-gray-200 text-sm text-gray-700 rounded-md hover:bg-gray-50"
      >
        Cancelar
      </button>
    </form>
  </div>
}

<!-- Member List -->
@if (loading()) {
  <div class="text-sm text-gray-500">Carregando...</div>
} @else {
  <div class="border border-gray-200 rounded-lg divide-y divide-gray-100">
    @for (member of members(); track member.email) {
      <div class="flex items-center justify-between px-5 py-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center">
            <span class="text-sm font-semibold text-gray-600">
              {{ getInitials(member.name ?? member.email) }}
            </span>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">{{ member.name ?? member.email }}</p>
            <p class="text-xs text-gray-500">{{ member.email }}</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span
            class="text-xs font-medium px-2.5 py-1 rounded-full"
            [class.bg-blue-50]="member.role === 'ADMIN'"
            [class.text-blue-700]="member.role === 'ADMIN'"
            [class.bg-gray-100]="member.role !== 'ADMIN'"
            [class.text-gray-600]="member.role !== 'ADMIN'"
          >
            {{ getRoleLabel(member.role) }}
          </span>
          <button class="text-gray-300 hover:text-gray-500">
            <i class="pi pi-cog text-sm"></i>
          </button>
        </div>
      </div>
    }
  </div>
}
```

**Step 2: Add helper methods to user-management component**

In `user-management.component.ts`, ensure these methods exist:

```typescript
protected readonly showInviteForm = signal(false);

protected toggleInviteForm() {
  this.showInviteForm.update((v) => !v);
}

protected getInitials(name: string): string {
  const parts = name.split(' ');
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

protected getRoleLabel(role: string | null): string {
  switch (role) {
    case 'ADMIN': return 'Administrador';
    case 'EDITOR': return 'Editor';
    case 'VIEWER': return 'Visualizador';
    default: return 'Convidado';
  }
}
```

Adapt to the existing component structure (check what signals/methods already exist).

**Step 3: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds.

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/company/pages/user-management/
git commit -m "refactor(users): switch to avatar list layout with role badges"
```

---

### Task 9: Contas a Pagar — Placeholder Page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/accounts-payable/pages/accounts-payable-list/accounts-payable-list.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/accounts-payable/pages/accounts-payable-list/accounts-payable-list.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/accounts-payable/accounts-payable.routes.ts`

**Step 1: Create component TypeScript**

```typescript
import { Component, ChangeDetectionStrategy, signal } from '@angular/core';

interface MockPayable {
  supplier: string;
  dueDate: string;
  category: string;
  status: 'Pendente' | 'Atrasado' | 'Pago';
  value: string;
}

@Component({
  selector: 'app-accounts-payable-list',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './accounts-payable-list.component.html',
})
export class AccountsPayableListComponent {
  protected readonly activeTab = signal('todos');

  protected readonly tabs = [
    { key: 'todos', label: 'Todos' },
    { key: 'pendentes', label: 'Pendentes' },
    { key: 'pagos', label: 'Pagos' },
    { key: 'atrasados', label: 'Atrasados' },
  ];

  protected readonly mockData: MockPayable[] = [
    { supplier: 'AWS Cloud Services', dueDate: '15 Jun 2024', category: 'Infraestrutura', status: 'Pendente', value: '- R$ 350,00' },
    { supplier: 'Aluguel Escritorio', dueDate: '10 Jun 2024', category: 'Operacional', status: 'Atrasado', value: '- R$ 2.500,00' },
  ];

  protected setTab(tab: string) {
    this.activeTab.set(tab);
  }
}
```

**Step 2: Create component template**

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Contas a Pagar</h1>
    <p class="mt-1 text-sm text-gray-500">Gerencie seus compromissos financeiros e vencimentos.</p>
  </div>
  <button
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    <i class="pi pi-plus text-xs"></i>
    Nova Conta
  </button>
</div>

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
      [class.hover:bg-gray-200]="activeTab() !== tab.key"
    >
      {{ tab.label }}
    </button>
  }
</div>

<!-- Table -->
<div class="border border-gray-200 rounded-lg overflow-hidden">
  <table class="w-full text-sm">
    <thead class="bg-gray-50 border-b border-gray-200">
      <tr>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Fornecedor</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Vencimento</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Categoria</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
        <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
        <th class="px-4 py-3"></th>
      </tr>
    </thead>
    <tbody>
      @for (item of mockData; track item.supplier) {
        <tr class="border-b border-gray-100 hover:bg-gray-50">
          <td class="px-4 py-3 font-medium text-gray-900">{{ item.supplier }}</td>
          <td class="px-4 py-3 text-gray-500">{{ item.dueDate }}</td>
          <td class="px-4 py-3">
            <span class="text-blue-600 text-xs font-medium">{{ item.category }}</span>
          </td>
          <td class="px-4 py-3">
            <span
              class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full"
              [class.bg-amber-50]="item.status === 'Pendente'"
              [class.text-amber-700]="item.status === 'Pendente'"
              [class.bg-red-50]="item.status === 'Atrasado'"
              [class.text-red-700]="item.status === 'Atrasado'"
              [class.bg-emerald-50]="item.status === 'Pago'"
              [class.text-emerald-700]="item.status === 'Pago'"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                [class.bg-amber-500]="item.status === 'Pendente'"
                [class.bg-red-500]="item.status === 'Atrasado'"
                [class.bg-emerald-500]="item.status === 'Pago'"
              ></span>
              {{ item.status }}
            </span>
          </td>
          <td class="px-4 py-3 text-right font-mono text-red-600">{{ item.value }}</td>
          <td class="px-4 py-3 text-right">
            <button class="text-gray-400 hover:text-gray-600">
              <i class="pi pi-ellipsis-h text-sm"></i>
            </button>
          </td>
        </tr>
      }
    </tbody>
  </table>
</div>
```

**Step 3: Create routes file**

```typescript
import { Routes } from '@angular/router';

export const accountsPayableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/accounts-payable-list/accounts-payable-list.component').then(
        (m) => m.AccountsPayableListComponent,
      ),
  },
];
```

**Step 4: Verify build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds (routes not yet registered — that's Task 12).

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/accounts-payable/
git commit -m "feat(accounts-payable): add placeholder page with mock data"
```

---

### Task 10: Contas a Receber — Placeholder Page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/accounts-receivable/pages/accounts-receivable-list/accounts-receivable-list.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/accounts-receivable/pages/accounts-receivable-list/accounts-receivable-list.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/accounts-receivable/accounts-receivable.routes.ts`

**Step 1: Create component TypeScript**

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

interface MockReceivable {
  client: string;
  date: string;
  category: string;
  status: 'Recebido' | 'Agendado' | 'Pendente';
  value: string;
}

@Component({
  selector: 'app-accounts-receivable-list',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './accounts-receivable-list.component.html',
})
export class AccountsReceivableListComponent {
  protected readonly mockData: MockReceivable[] = [
    { client: 'Tech Solutions Ltda', date: 'Hoje', category: 'Consultoria', status: 'Recebido', value: '+ R$ 4.500,00' },
    { client: 'Design Studio A', date: 'Amanha', category: 'Projeto', status: 'Agendado', value: '+ R$ 2.100,00' },
  ];
}
```

**Step 2: Create component template**

```html
<!-- Page Header -->
<div class="flex items-center justify-between mb-6">
  <div>
    <h1 class="text-2xl font-bold text-gray-900">Contas a Receber</h1>
    <p class="mt-1 text-sm text-gray-500">Acompanhe as entradas e faturamentos.</p>
  </div>
  <button
    class="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 transition-colors duration-150"
  >
    <i class="pi pi-plus text-xs"></i>
    Novo Recebimento
  </button>
</div>

<!-- Table -->
<div class="border border-gray-200 rounded-lg overflow-hidden">
  <table class="w-full text-sm">
    <thead class="bg-gray-50 border-b border-gray-200">
      <tr>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Cliente</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Data</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Categoria</th>
        <th class="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
        <th class="text-right px-4 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
        <th class="px-4 py-3"></th>
      </tr>
    </thead>
    <tbody>
      @for (item of mockData; track item.client) {
        <tr class="border-b border-gray-100 hover:bg-gray-50">
          <td class="px-4 py-3 font-medium text-gray-900">{{ item.client }}</td>
          <td class="px-4 py-3 text-gray-500">{{ item.date }}</td>
          <td class="px-4 py-3">
            <span class="text-blue-600 text-xs font-medium">{{ item.category }}</span>
          </td>
          <td class="px-4 py-3">
            <span
              class="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full"
              [class.bg-emerald-50]="item.status === 'Recebido'"
              [class.text-emerald-700]="item.status === 'Recebido'"
              [class.bg-blue-50]="item.status === 'Agendado'"
              [class.text-blue-700]="item.status === 'Agendado'"
              [class.bg-amber-50]="item.status === 'Pendente'"
              [class.text-amber-700]="item.status === 'Pendente'"
            >
              <span
                class="w-1.5 h-1.5 rounded-full"
                [class.bg-emerald-500]="item.status === 'Recebido'"
                [class.bg-blue-500]="item.status === 'Agendado'"
                [class.bg-amber-500]="item.status === 'Pendente'"
              ></span>
              {{ item.status }}
            </span>
          </td>
          <td class="px-4 py-3 text-right font-mono text-emerald-600">{{ item.value }}</td>
          <td class="px-4 py-3 text-right">
            <button class="text-gray-400 hover:text-gray-600">
              <i class="pi pi-ellipsis-h text-sm"></i>
            </button>
          </td>
        </tr>
      }
    </tbody>
  </table>
</div>
```

**Step 3: Create routes file**

```typescript
import { Routes } from '@angular/router';

export const accountsReceivableRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/accounts-receivable-list/accounts-receivable-list.component').then(
        (m) => m.AccountsReceivableListComponent,
      ),
  },
];
```

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/accounts-receivable/
git commit -m "feat(accounts-receivable): add placeholder page with mock data"
```

---

### Task 11: Importacao OFX — Placeholder Page

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/import/pages/import-page/import-page.component.ts`
- Create: `gestao-empresaial-frontend/src/app/features/import/pages/import-page/import-page.component.html`
- Create: `gestao-empresaial-frontend/src/app/features/import/import.routes.ts`

**Step 1: Create component TypeScript**

```typescript
import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-import-page',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './import-page.component.html',
})
export class ImportPageComponent {
  protected readonly recentImports = [
    { name: 'extrato_nubank_junho.ofx', date: 'Importado em 01/06/2024 as 10:30', status: 'Processado' },
  ];
}
```

**Step 2: Create component template**

```html
<!-- Page Header -->
<div class="mb-6">
  <h1 class="text-2xl font-bold text-gray-900">Importacao OFX</h1>
  <p class="mt-1 text-sm text-gray-500">Importe extratos bancarios para conciliacao automatica.</p>
</div>

<!-- Upload Area -->
<div class="border-2 border-dashed border-gray-200 rounded-lg p-12 text-center mb-8 hover:border-gray-300 transition-colors duration-150 cursor-pointer">
  <div class="flex flex-col items-center">
    <i class="pi pi-cloud-upload text-3xl text-blue-500 mb-3"></i>
    <p class="text-sm font-medium text-gray-900 mb-1">Clique para fazer upload</p>
    <p class="text-xs text-gray-500">Arraste seu arquivo OFX ou clique aqui.</p>
    <p class="text-xs text-gray-500">Tamanho maximo 10MB.</p>
  </div>
</div>

<!-- Recent Imports -->
<div>
  <h2 class="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-4">Ultimas Importacoes</h2>
  <div class="border border-gray-200 rounded-lg divide-y divide-gray-100">
    @for (item of recentImports; track item.name) {
      <div class="flex items-center justify-between px-5 py-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-gray-100 rounded-lg flex items-center justify-center">
            <i class="pi pi-file text-gray-500"></i>
          </div>
          <div>
            <p class="text-sm font-medium text-gray-900">{{ item.name }}</p>
            <p class="text-xs text-gray-500">{{ item.date }}</p>
          </div>
        </div>
        <span class="text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full">
          {{ item.status }}
        </span>
      </div>
    }
  </div>
</div>
```

**Step 3: Create routes file**

```typescript
import { Routes } from '@angular/router';

export const importRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/import-page/import-page.component').then((m) => m.ImportPageComponent),
  },
];
```

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/
git commit -m "feat(import): add OFX import placeholder page"
```

---

### Task 12: Register New Routes + Build Verification

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/app.routes.ts`

**Step 1: Add the 3 new feature routes to app.routes.ts**

In the `children` array of the MainLayoutComponent route, add the following entries (before the existing `fornecedores`, `clientes`, `categorias` entries):

```typescript
{
  path: 'contas-a-pagar',
  loadChildren: () =>
    import('./features/accounts-payable/accounts-payable.routes').then(
      (m) => m.accountsPayableRoutes,
    ),
},
{
  path: 'contas-a-receber',
  loadChildren: () =>
    import('./features/accounts-receivable/accounts-receivable.routes').then(
      (m) => m.accountsReceivableRoutes,
    ),
},
{
  path: 'importacao',
  loadChildren: () =>
    import('./features/import/import.routes').then((m) => m.importRoutes),
},
```

**Step 2: Verify full build**

Run: `cd gestao-empresaial-frontend && npm run build`
Expected: Build succeeds with all chunks generated, initial bundle under 500kB.

**Step 3: Verify backend tests still pass**

Run: `cd gestao-empresarial-backend && JAVA_HOME=/home/danilo/.jdks/openjdk-25.0.2 ./mvnw test -q`
Expected: All tests pass (backend is unchanged in this plan).

**Step 4: Commit**

```bash
git add gestao-empresaial-frontend/src/app/app.routes.ts
git commit -m "feat(routes): register accounts-payable, accounts-receivable, import routes"
```

---

## Summary of Changes

| Task | Component | Change |
|------|-----------|--------|
| 1 | Sidebar | Dark theme, section headers, user avatar at bottom |
| 2 | Main Layout | Add footer with copyright |
| 3 | Dashboard | KPI cards, chart placeholder, quick actions |
| 4 | Fornecedores List | Table → Card grid with avatars |
| 5 | Clientes List | Updated columns: Contato, Status badge, Receita Total |
| 6 | Categorias | Card grid per group with colored icons |
| 7 | Empresa Settings | 2x2 grid form layout |
| 8 | Usuarios | Avatar list with role badges |
| 9 | Contas a Pagar | New placeholder page with tabs + status table |
| 10 | Contas a Receber | New placeholder page with status table |
| 11 | Importacao OFX | New placeholder page with upload area |
| 12 | Routes | Register 3 new routes + build verification |
