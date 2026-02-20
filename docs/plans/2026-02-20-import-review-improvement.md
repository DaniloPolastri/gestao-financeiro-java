# Melhoria na Tela de Revisão de Importação — Plano de Implementação

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Melhorar a tela de revisão de importação para que o usuário possa editar o tipo (A Pagar/A Receber) por item, navegar com paginação, e confirmar que revisou tudo antes de importar.

**Architecture:** Todas as mudanças são frontend-only no componente `bank-import-review`. O backend já suporta `accountType` nos endpoints de update. Paginação é client-side (itens já carregados). Checkbox de revisão é puramente local.

**Tech Stack:** Angular 21, Signals, Tailwind CSS 4, FormsModule (ngModel)

**Design spec:** `docs/specs/2026-02-20-import-review-improvement-design.md`

---

### Task 1: Coluna "Tipo" editável — Dropdown por item

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.html:113-123`

**Step 1: Substituir badge estático por select**

No template, localizar a coluna "Tipo" (linhas 113-123) que contém o badge estático:

```html
<td class="px-4 py-3 text-center">
  <span
    class="text-xs px-2 py-1 rounded-full whitespace-nowrap"
    [class]="
      item.accountType === 'PAYABLE'
        ? 'bg-red-100 text-red-700'
        : 'bg-emerald-100 text-emerald-700'
    "
  >
    {{ item.accountType === 'PAYABLE' ? 'A Pagar' : 'A Receber' }}
  </span>
</td>
```

Substituir por:

```html
<td class="px-4 py-3">
  <select
    class="w-full text-sm border border-gray-300 rounded px-2 py-1"
    [ngModel]="item.accountType"
    (ngModelChange)="updateItemField(item, 'accountType', $event)"
  >
    <option value="PAYABLE">A Pagar</option>
    <option value="RECEIVABLE">A Receber</option>
  </select>
</td>
```

**Step 2: Verificar manualmente no browser**

Run: `npm start` (se não estiver rodando)
- Acessar tela de revisão de uma importação existente
- A coluna Tipo deve mostrar dropdown com valor correto
- Alterar o tipo deve chamar o endpoint PATCH e atualizar o item

**Step 3: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.html
git commit -m "feat: make account type column editable in import review"
```

---

### Task 2: Dropdown "Tipo" na bulk action bar

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.html:36-70`
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.ts:99-111`

**Step 1: Adicionar select de Tipo na barra de ações em lote**

No template, dentro do bloco `@if (selectedIds().size > 0)` (linha 37-70), adicionar um select de Tipo **antes** do select de Fornecedor/Cliente:

```html
<select class="text-sm border border-gray-300 rounded px-2 py-1" #bulkType>
  <option value="">Tipo...</option>
  <option value="PAYABLE">A Pagar</option>
  <option value="RECEIVABLE">A Receber</option>
</select>
```

**Step 2: Atualizar o botão "Aplicar" para incluir accountType**

O botão "Aplicar" (linha 63-68) chama `applyBulk(bulkSupplier.value, bulkCategory.value)`. Adicionar o parâmetro de tipo:

```html
<button
  (click)="applyBulk(bulkType.value || null, bulkSupplier.value || null, bulkCategory.value || null)"
  class="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
>
  Aplicar
</button>
```

**Step 3: Atualizar o método `applyBulk` no componente**

No `.ts`, o método `applyBulk` (linha 99-111) precisa receber `accountType` como primeiro parâmetro:

```typescript
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
```

**Step 4: Verificar manualmente no browser**

- Selecionar 2+ itens via checkbox
- Barra de ações em lote deve mostrar dropdown "Tipo..." ao lado dos outros
- Selecionar "A Receber" e clicar "Aplicar" deve alterar todos os selecionados

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/
git commit -m "feat: add account type to bulk action bar in import review"
```

---

### Task 3: Paginação client-side

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.html`

**Step 1: Adicionar signals e computeds de paginação no componente**

No `.ts`, adicionar após os signals existentes (após linha 36):

```typescript
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
```

Adicionar método de navegação:

```typescript
protected goToReviewPage(page: number): void {
  this.currentReviewPage.set(page);
}
```

**Step 2: Atualizar template para usar `pagedItems()`**

No `.html`, trocar `bankImport()!.items` por `pagedItems()` no `@for` da tabela (linha 89):

```html
@for (item of pagedItems(); track item.id) {
```

**Step 3: Adicionar componente de paginação no rodapé da tabela**

Após o fechamento da `</div>` da tabela (linha 166), antes do modal de cancelar, adicionar:

```html
@if (totalReviewPages() > 1) {
  <div class="flex items-center justify-between mt-4">
    <p class="text-xs text-gray-500">{{ allItems().length }} item(ns)</p>
    <div class="flex gap-1">
      @for (p of reviewPages(); track $index) {
        <button
          (click)="goToReviewPage($index)"
          class="w-8 h-8 text-xs rounded-md"
          [class.bg-blue-600]="currentReviewPage() === $index"
          [class.text-white]="currentReviewPage() === $index"
          [class.bg-gray-100]="currentReviewPage() !== $index"
          [class.text-gray-600]="currentReviewPage() !== $index"
        >
          {{ $index + 1 }}
        </button>
      }
    </div>
  </div>
}
```

**Step 4: Resetar página ao mudar dados**

No método `replaceItem` (`.ts`), verificar se a página atual ainda é válida após alteração de dados. Não é estritamente necessário (os dados não mudam de tamanho), mas é boa prática. Sem mudança necessária.

**Step 5: Verificar manualmente no browser**

- Importar extrato com >25 itens
- Deve mostrar apenas 25 itens por página
- Paginação no rodapé com botões numerados
- Navegar entre páginas deve funcionar
- Contadores "X de Y prontos" devem refletir TODOS os itens (não apenas a página atual)

**Step 6: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/
git commit -m "feat: add client-side pagination to import review (25 items/page)"
```

---

### Task 4: Checkbox "Revisei todos os itens"

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/bank-import-review.component.html`

**Step 1: Adicionar signal `reviewed` no componente**

No `.ts`, adicionar após os outros signals:

```typescript
protected readonly reviewed = signal(false);
```

**Step 2: Atualizar condição do botão "Confirmar Importação"**

No `.html`, o botão de confirmar (linha 22-33) tem `[disabled]="!allReady() || confirming()"`. Adicionar `!reviewed()`:

```html
[disabled]="!allReady() || !reviewed() || confirming()"
[title]="!allReady() ? 'Preencha fornecedor e categoria em todos os itens' : !reviewed() ? 'Marque que revisou todos os itens' : ''"
```

**Step 3: Adicionar checkbox no template**

Após a paginação (ou após a tabela se não houver paginação), antes do modal de cancelar, adicionar:

```html
<div class="mt-4 p-3 bg-gray-50 border border-gray-200 rounded-lg">
  <label class="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
    <input
      type="checkbox"
      [checked]="reviewed()"
      (change)="reviewed.set(!reviewed())"
      class="rounded border-gray-300"
    />
    Revisei todos os itens e confirmo que os tipos (A Pagar / A Receber) estão corretos
  </label>
</div>
```

**Step 4: Verificar manualmente no browser**

- Botão "Confirmar Importação" deve ficar desabilitado mesmo com todos os itens preenchidos
- Marcar o checkbox deve habilitar o botão (se todos os itens estiverem prontos)
- Tooltip deve indicar o motivo quando desabilitado

**Step 5: Commit**

```bash
git add gestao-empresaial-frontend/src/app/features/import/pages/bank-import-review/
git commit -m "feat: add 'reviewed all items' checkbox before import confirmation"
```

---

### Task 5: Build e verificação final

**Step 1: Rodar build de produção**

```bash
cd gestao-empresaial-frontend && npm run build
```

Esperado: build sem erros.

**Step 2: Rodar testes existentes**

```bash
cd gestao-empresaial-frontend && npm test
```

Esperado: testes passando (ignorar falha pré-existente em `app.spec.ts` se houver).

**Step 3: Commit final se houver ajustes**

Se algum ajuste for necessário após build/testes, commitar.

**Step 4: Marcar tarefas como concluídas no plano**

Atualizar os checkboxes `[ ]` para `[x]` neste arquivo.
