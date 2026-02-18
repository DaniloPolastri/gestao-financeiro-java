# Design: Right-Side Drawer Pattern for Create/Edit Forms

**Date:** 2026-02-18
**Status:** Approved
**Scope:** Accounts (Payable/Receivable), Suppliers, Clients, Categories

---

## Problem

Currently, "Nova Conta", "Novo Recebimento", "Novo Fornecedor", "Nova Categoria", "Novo Cliente", and all edit actions navigate to a separate page. This breaks the user's context and forces a full page transition for simple form interactions.

## Solution

Replace all create/edit page navigations with a reusable right-side drawer (slide-in panel) that overlays the current list, keeping the user in context.

---

## Architecture

### Shared `DrawerComponent`

A single reusable component in `shared/ui/drawer/` that:
- Accepts a `title` input
- Projects form content via `<ng-content>`
- Handles open/close animation, overlay, X button, ESC key
- Emits `closed` output

```
src/app/shared/ui/drawer/
  drawer.component.ts
  drawer.component.html
```

### Integration Pattern

Each list component (AccountList, SupplierList, ClientList, CategoryList):
- Holds a `drawerOpen` signal and `editingId` signal
- Renders `<app-drawer>` with the appropriate form inside `<ng-content>`
- Passes `editingId` to the form component as an input
- Form component emits `saved` and `cancelled` outputs

---

## Visual Design

```
┌─────────────────────────────┬──────────────────────┐
│                             │  ╳  Nova Conta a Pagar│
│   List / table content      │  ─────────────────────│
│   (dimmed by overlay)       │  Descrição *          │
│                             │  [________________]   │
│                             │                       │
│                             │  Valor     Vencimento │
│                             │  [______] [________]  │
│                             │                       │
│                             │  Categoria *          │
│                             │  [______________▼]    │
│                             │                       │
│                             │  Fornecedor *         │
│                             │  [______________▼]    │
│                             │                       │
│                             │  + Adicionar recorrên.│
│                             │  ─────────────────────│
│                             │  [Salvar] [Cancelar]  │
└─────────────────────────────┴──────────────────────┘
```

### Drawer Specs
- **Width:** `480px` fixed
- **Height:** 100vh, fixed position, right edge
- **Overlay:** `bg-black/30` covering the list — clicking overlay does nothing
- **Animation:** `translate-x-full` → `translate-x-0`, `duration-200 ease-out`
- **Close triggers:** X button, "Cancelar" button, ESC key
- **No close on overlay click** (explicit action required)

---

## Affected Features & Triggers

| Feature | Open Trigger | Drawer Title |
|---|---|---|
| Contas a Pagar | "Nova Conta" button | Nova Conta a Pagar |
| Contas a Pagar | Pencil icon on row | Editar Conta a Pagar |
| Contas a Receber | "Novo Recebimento" button | Nova Conta a Receber |
| Contas a Receber | Pencil icon on row | Editar Conta a Receber |
| Fornecedores | "Novo Fornecedor" button | Novo Fornecedor |
| Fornecedores | Pencil icon on row | Editar Fornecedor |
| Clientes | "Novo Cliente" button | Novo Cliente |
| Clientes | Pencil icon on row | Editar Cliente |
| Categorias | "Nova Categoria" button | Nova Categoria |
| Categorias | Pencil icon on row | Editar Categoria |

---

## Form Components

Each feature gets a dedicated form component (output-based, no routing):

| Component | Path |
|---|---|
| `AccountDrawerFormComponent` | `features/accounts/components/account-drawer-form/` |
| `SupplierDrawerFormComponent` | `features/suppliers/components/supplier-drawer-form/` |
| `ClientDrawerFormComponent` | `features/clients/components/client-drawer-form/` |
| `CategoryDrawerFormComponent` | `features/categories/components/category-drawer-form/` |

All emit `saved: EventEmitter<void>` and `cancelled: EventEmitter<void>`.
All accept `accountId?: string` (or supplierId, etc.) — when set, loads existing data for edit mode.

---

## Route Cleanup

The following routes become unused and are removed:
- `/contas-a-pagar/novo`, `/contas-a-pagar/:id/editar`
- `/contas-a-receber/novo`, `/contas-a-receber/:id/editar`
- Supplier, Client, Category create/edit routes (if any exist as pages)

`AccountFormComponent` (page) is deleted. Routes simplify to list-only.

---

## Behavior on Save

1. Form calls service (`create` or `update`)
2. On success: emits `saved` → list component closes drawer and reloads list
3. On error: shows inline error inside drawer, drawer stays open

---

## Constraints

- No PrimeNG (not yet installed) — pure Tailwind CSS drawer
- `ChangeDetectionStrategy.OnPush` on all components
- `input()` / `output()` functions (Angular 21 style)
- No new routes added
