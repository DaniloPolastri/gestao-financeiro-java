# Status Automatico no Import + Pagamento em Lote — Plano de Implementacao

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Corrigir status de transacoes importadas de extratos (marcar como PAID/RECEIVED se data < hoje) e adicionar funcionalidade de pagamento em lote na UI.

**Architecture:** Modificar o `confirm()` do BankImportService para setar status correto baseado na data. Criar endpoint batch-pay no backend. Adicionar checkboxes + barra de acoes + dialog no frontend.

**Tech Stack:** Spring Boot 4 (Java 17), Angular 21 (Signals, standalone components, Tailwind CSS 4)

**Spec:** `docs/specs/2026-02-19-status-autopay-design.md`

---

## Task 1: Backend — Status automatico no confirm() do import

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/BankImportServiceImpl.java:181-193`
- Test: `gestao-empresarial-backend/src/test/java/com/findash/service/impl/BankImportServiceImplTest.java`

### Step 1: Write failing tests

- [x] Adicionar teste `confirm_pastDateItem_createsAccountAsPaid` no `BankImportServiceImplTest.java`:

```java
@Test
void confirm_pastDateItem_createsAccountAsPaid() {
    UUID importId = UUID.randomUUID();
    UUID supplierId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
    bankImport.setId(importId);

    LocalDate pastDate = LocalDate.now().minusDays(5);
    BankImportItem item = new BankImportItem(importId, pastDate, "Pix enviado",
        new BigDecimal("100.00"), BankImportItemType.DEBIT, AccountType.PAYABLE);
    item.setId(UUID.randomUUID());
    item.setSupplierId(supplierId);
    item.setCategoryId(categoryId);

    when(importRepository.findByIdAndCompanyId(importId, companyId))
        .thenReturn(Optional.of(bankImport));
    when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
    when(supplierRepository.existsById(supplierId)).thenReturn(true);
    when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
        .thenReturn(Optional.empty());
    when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    service.confirm(companyId, importId);

    verify(accountRepository).save(argThat(account ->
        account.getStatus() == AccountStatus.PAID &&
        account.getPaymentDate() != null &&
        account.getPaymentDate().equals(pastDate)
    ));
}

@Test
void confirm_futureDateItem_createsAccountAsPending() {
    UUID importId = UUID.randomUUID();
    UUID supplierId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
    bankImport.setId(importId);

    LocalDate futureDate = LocalDate.now().plusDays(5);
    BankImportItem item = new BankImportItem(importId, futureDate, "Agendamento",
        new BigDecimal("200.00"), BankImportItemType.DEBIT, AccountType.PAYABLE);
    item.setId(UUID.randomUUID());
    item.setSupplierId(supplierId);
    item.setCategoryId(categoryId);

    when(importRepository.findByIdAndCompanyId(importId, companyId))
        .thenReturn(Optional.of(bankImport));
    when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
    when(supplierRepository.existsById(supplierId)).thenReturn(true);
    when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
        .thenReturn(Optional.empty());
    when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    service.confirm(companyId, importId);

    verify(accountRepository).save(argThat(account ->
        account.getStatus() == AccountStatus.PENDING &&
        account.getPaymentDate() == null
    ));
}

@Test
void confirm_pastDateReceivable_createsAccountAsReceived() {
    UUID importId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    BankImport bankImport = new BankImport(companyId, "f.ofx", BankImportFileType.OFX, userId);
    bankImport.setId(importId);

    LocalDate pastDate = LocalDate.now().minusDays(3);
    BankImportItem item = new BankImportItem(importId, pastDate, "Recebimento",
        new BigDecimal("500.00"), BankImportItemType.CREDIT, AccountType.RECEIVABLE);
    item.setId(UUID.randomUUID());
    item.setSupplierId(clientId);
    item.setCategoryId(categoryId);

    when(importRepository.findByIdAndCompanyId(importId, companyId))
        .thenReturn(Optional.of(bankImport));
    when(itemRepository.findByImportId(importId)).thenReturn(List.of(item));
    when(supplierRepository.existsById(clientId)).thenReturn(false);
    when(matchRuleRepository.findByCompanyIdAndPattern(any(), any()))
        .thenReturn(Optional.empty());
    when(matchRuleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

    service.confirm(companyId, importId);

    verify(accountRepository).save(argThat(account ->
        account.getStatus() == AccountStatus.RECEIVED &&
        account.getPaymentDate() != null
    ));
}
```

### Step 2: Run tests to verify they fail

- [x] Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=BankImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected: 3 novos testes FAIL (status sera PENDING em vez de PAID/RECEIVED)

### Step 3: Implement the fix

- [x] Modificar `BankImportServiceImpl.java`, metodo `confirm()`. Dentro do loop `for (BankImportItem item : items)`, apos criar o Account e setar supplier/client, adicionar verificacao de data:

```java
// Apos a linha: account.setClientId(counterpartyId);
// Adicionar:
if (item.getDate().isBefore(LocalDate.now())) {
    account.setStatus(account.getType() == AccountType.PAYABLE
        ? AccountStatus.PAID : AccountStatus.RECEIVED);
    account.setPaymentDate(item.getDate());
}
```

### Step 4: Run tests to verify they pass

- [x] Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=BankImportServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected: ALL PASS

### Step 5: Commit

- [x] `git add` dos arquivos modificados e `git commit -m "feat: auto-set PAID/RECEIVED status for past-date imported transactions"`

---

## Task 2: Backend — DTO e interface do batchPay

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/BatchPayRequestDTO.java`
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/AccountService.java`

### Step 1: Create BatchPayRequestDTO

- [x]Criar o DTO:

```java
package com.findash.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BatchPayRequestDTO(
    @NotEmpty(message = "Lista de contas e obrigatoria")
    List<UUID> accountIds,
    @NotNull(message = "Data de pagamento e obrigatoria")
    LocalDate paymentDate
) {}
```

### Step 2: Add interface method

- [x]Adicionar em `AccountService.java`:

```java
List<AccountResponseDTO> batchPay(UUID companyId, BatchPayRequestDTO request);
```

### Step 3: Commit

- [x]`git commit -m "feat: add BatchPayRequestDTO and batchPay interface method"`

---

## Task 3: Backend — Implementar batchPay no service

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/service/impl/AccountServiceImpl.java`
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/repository/AccountRepository.java`
- Test: `gestao-empresarial-backend/src/test/java/com/findash/service/impl/AccountServiceImplTest.java`

### Step 1: Add repository method

- [x]Adicionar em `AccountRepository.java`:

```java
List<Account> findByIdInAndCompanyId(List<UUID> ids, UUID companyId);
```

### Step 2: Write failing tests

- [x]Adicionar testes no `AccountServiceImplTest.java`:

```java
@Test
void batchPay_marksPendingAccountsAsPaid() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Account acc1 = createMockAccount(id1, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("100.00"));
    Account acc2 = createMockAccount(id2, AccountType.PAYABLE, AccountStatus.OVERDUE, new BigDecimal("200.00"));
    var request = new BatchPayRequestDTO(List.of(id1, id2), LocalDate.of(2026, 2, 19));

    when(accountRepository.findByIdInAndCompanyId(List.of(id1, id2), companyId))
        .thenReturn(List.of(acc1, acc2));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    when(accountMapper.toResponse(any(), any(), any(), any()))
        .thenReturn(createMockResponse("PAYABLE", "PAID"));

    List<AccountResponseDTO> result = accountService.batchPay(companyId, request);

    assertEquals(2, result.size());
    assertEquals(AccountStatus.PAID, acc1.getStatus());
    assertEquals(AccountStatus.PAID, acc2.getStatus());
    assertEquals(LocalDate.of(2026, 2, 19), acc1.getPaymentDate());
    verify(accountRepository, times(2)).save(any(Account.class));
}

@Test
void batchPay_skipsAlreadyPaidAccounts() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    Account acc1 = createMockAccount(id1, AccountType.PAYABLE, AccountStatus.PAID, new BigDecimal("100.00"));
    Account acc2 = createMockAccount(id2, AccountType.PAYABLE, AccountStatus.PENDING, new BigDecimal("200.00"));
    var request = new BatchPayRequestDTO(List.of(id1, id2), LocalDate.of(2026, 2, 19));

    when(accountRepository.findByIdInAndCompanyId(List.of(id1, id2), companyId))
        .thenReturn(List.of(acc1, acc2));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    when(accountMapper.toResponse(any(), any(), any(), any()))
        .thenReturn(createMockResponse("PAYABLE", "PAID"));

    List<AccountResponseDTO> result = accountService.batchPay(companyId, request);

    assertEquals(1, result.size());
    assertEquals(AccountStatus.PAID, acc1.getStatus()); // unchanged
    verify(accountRepository, times(1)).save(any(Account.class)); // only acc2
}

@Test
void batchPay_receivable_setsReceivedStatus() {
    UUID id1 = UUID.randomUUID();
    Account acc1 = createMockAccount(id1, AccountType.RECEIVABLE, AccountStatus.PENDING, new BigDecimal("500.00"));
    var request = new BatchPayRequestDTO(List.of(id1), LocalDate.of(2026, 2, 19));

    when(accountRepository.findByIdInAndCompanyId(List.of(id1), companyId))
        .thenReturn(List.of(acc1));
    when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
    when(accountMapper.toResponse(any(), any(), any(), any()))
        .thenReturn(createMockResponse("RECEIVABLE", "RECEIVED"));

    accountService.batchPay(companyId, request);

    assertEquals(AccountStatus.RECEIVED, acc1.getStatus());
}
```

### Step 3: Run tests to verify they fail

- [x]Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=AccountServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected: FAIL (metodo batchPay nao existe ainda)

### Step 4: Implement batchPay in AccountServiceImpl

- [x]Adicionar metodo em `AccountServiceImpl.java`:

```java
@Override
public List<AccountResponseDTO> batchPay(UUID companyId, BatchPayRequestDTO request) {
    List<Account> accounts = accountRepository.findByIdInAndCompanyId(request.accountIds(), companyId);

    List<AccountResponseDTO> result = new ArrayList<>();
    for (Account account : accounts) {
        if (account.getStatus() == AccountStatus.PAID || account.getStatus() == AccountStatus.RECEIVED) {
            continue;
        }
        account.setPaymentDate(request.paymentDate());
        account.setStatus(account.getType() == AccountType.PAYABLE ? AccountStatus.PAID : AccountStatus.RECEIVED);
        accountRepository.save(account);
        result.add(toResponseWithRelations(account));
    }
    return result;
}
```

### Step 5: Run tests to verify they pass

- [x]Run: `cd gestao-empresarial-backend && ./mvnw test -pl . -Dtest=AccountServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false`
- Expected: ALL PASS

### Step 6: Commit

- [x]`git commit -m "feat: implement batchPay service method with repository query"`

---

## Task 4: Backend — Endpoint batch-pay no controller

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/controller/AccountController.java`

### Step 1: Add endpoint

- [x]Adicionar no `AccountController.java`:

```java
@PostMapping("/batch-pay")
public ResponseEntity<List<AccountResponseDTO>> batchPay(
        @Valid @RequestBody BatchPayRequestDTO request) {
    UUID companyId = CompanyContextHolder.get();
    return ResponseEntity.ok(accountService.batchPay(companyId, request));
}
```

Import necessario: `import com.findash.dto.BatchPayRequestDTO;` e `import java.util.List;`

### Step 2: Run full backend tests

- [x]Run: `cd gestao-empresarial-backend && ./mvnw test`
- Expected: ALL PASS

### Step 3: Commit

- [x]`git commit -m "feat: add POST /api/accounts/batch-pay endpoint"`

---

## Task 5: Frontend — Model e service do batchPay

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/models/account.model.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/services/account.service.ts`

### Step 1: Add BatchPayRequest interface

- [x]Adicionar no final de `account.model.ts`:

```typescript
export interface BatchPayRequest {
  accountIds: string[];
  paymentDate: string;
}
```

### Step 2: Add batchPay method to service

- [x]Adicionar no `account.service.ts`, importar `BatchPayRequest` e adicionar metodo:

```typescript
batchPay(data: BatchPayRequest): Observable<AccountResponse[]> {
  return this.http.post<AccountResponse[]>(`${this.API_URL}/batch-pay`, data);
}
```

### Step 3: Commit

- [x]`git commit -m "feat: add batchPay method to frontend account service"`

---

## Task 6: Frontend — Checkboxes e selecao na tabela

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.html`

### Step 1: Add selection state to component

- [x]Adicionar signals e metodos de selecao no `account-list.component.ts`:

```typescript
// Novos signals (junto dos existentes)
protected readonly selectedIds = signal<Set<string>>(new Set());
protected readonly hasSelection = computed(() => this.selectedIds().size > 0);
protected readonly selectableAccounts = computed(() =>
  this.accounts().filter(a => a.status !== 'PAID' && a.status !== 'RECEIVED')
);
protected readonly allSelectableSelected = computed(() => {
  const selectable = this.selectableAccounts();
  return selectable.length > 0 && selectable.every(a => this.selectedIds().has(a.id));
});

// Metodos de selecao
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
```

Adicionar import de `AccountResponse` no topo do arquivo (se nao estiver).

### Step 2: Add checkboxes to template

- [x]No `account-list.component.html`, adicionar coluna de checkbox no `<thead>`:

Apos a tag `<tr>` no thead, antes da primeira `<th>`, adicionar:
```html
<th class="px-4 py-3 w-10">
  <input type="checkbox"
    [checked]="allSelectableSelected()"
    (change)="toggleSelectAll()"
    class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
</th>
```

No `<tbody>`, dentro do `<tr>` de cada account, antes da primeira `<td>`, adicionar:
```html
<td class="px-4 py-3 w-10">
  @if (isSelectable(account)) {
    <input type="checkbox"
      [checked]="selectedIds().has(account.id)"
      (change)="toggleSelect(account.id)"
      class="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500" />
  }
</td>
```

### Step 3: Clear selection on data reload

- [x]No metodo `loadData()`, adicionar `this.clearSelection();` no inicio (antes de `this.loading.set(true)`).

### Step 4: Verify visually

- [x]Run: `cd gestao-empresaial-frontend && npm start`
- Verificar que checkboxes aparecem na tabela
- Contas PAID/RECEIVED nao devem ter checkbox
- "Selecionar todos" so seleciona contas elegiveis

### Step 5: Commit

- [x]`git commit -m "feat: add selection checkboxes to account list table"`

---

## Task 7: Frontend — Barra de acoes e dialog de pagamento

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/accounts/pages/account-list/account-list.component.html`

### Step 1: Add pay dialog state and method

- [x]Adicionar signals e metodo no `account-list.component.ts`:

```typescript
// Novos signals
protected readonly showPayDialog = signal(false);
protected readonly paymentDate = signal(new Date().toISOString().split('T')[0]);
protected readonly batchPayLoading = signal(false);

// Metodo de pagamento em lote
protected openPayDialog() {
  this.paymentDate.set(new Date().toISOString().split('T')[0]);
  this.showPayDialog.set(true);
}

protected closePayDialog() {
  this.showPayDialog.set(false);
}

protected confirmBatchPay() {
  this.batchPayLoading.set(true);
  this.accountService.batchPay({
    accountIds: Array.from(this.selectedIds()),
    paymentDate: this.paymentDate(),
  }).subscribe({
    next: () => {
      this.batchPayLoading.set(false);
      this.closePayDialog();
      this.clearSelection();
      this.loadData();
    },
    error: (err) => {
      this.batchPayLoading.set(false);
      this.error.set(err.error?.message || 'Erro ao marcar como pago');
    },
  });
}
```

Adicionar import de `BatchPayRequest` no model import (se necessario).

### Step 2: Add action bar and dialog to template

- [x]No `account-list.component.html`, apos o bloco de paginacao (antes do `<!-- Drawer -->`), adicionar:

```html
<!-- Barra de acoes em lote -->
@if (hasSelection()) {
  <div class="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 px-5 py-3 bg-white border border-gray-200 rounded-lg shadow-lg">
    <span class="text-sm text-gray-600 font-medium">
      {{ selectedIds().size }} conta(s) selecionada(s)
    </span>
    <button
      (click)="openPayDialog()"
      class="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 text-white text-sm font-medium rounded-md hover:bg-emerald-700 transition-colors duration-150">
      <i class="pi pi-check text-xs"></i>
      {{ isPayable() ? 'Marcar como Pago' : 'Marcar como Recebido' }}
    </button>
    <button
      (click)="clearSelection()"
      class="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700 transition-colors duration-150">
      Cancelar
    </button>
  </div>
}

<!-- Dialog de pagamento -->
@if (showPayDialog()) {
  <div class="fixed inset-0 z-50 flex items-center justify-center">
    <div class="absolute inset-0 bg-black/40" (click)="closePayDialog()"></div>
    <div class="relative bg-white rounded-lg shadow-xl p-6 w-full max-w-sm">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">
        {{ isPayable() ? 'Marcar como Pago' : 'Marcar como Recebido' }}
      </h3>
      <p class="text-sm text-gray-500 mb-4">
        {{ selectedIds().size }} conta(s) serao marcadas.
      </p>
      <label class="block text-sm font-medium text-gray-700 mb-1">Data do pagamento</label>
      <input
        type="date"
        [value]="paymentDate()"
        (input)="paymentDate.set($any($event.target).value)"
        class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-blue-500 focus:border-blue-500 mb-4" />
      <div class="flex justify-end gap-2">
        <button
          (click)="closePayDialog()"
          class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors duration-150">
          Cancelar
        </button>
        <button
          (click)="confirmBatchPay()"
          [disabled]="batchPayLoading()"
          class="px-4 py-2 bg-emerald-600 text-white text-sm font-medium rounded-md hover:bg-emerald-700 disabled:opacity-50 transition-colors duration-150">
          {{ batchPayLoading() ? 'Processando...' : 'Confirmar' }}
        </button>
      </div>
    </div>
  </div>
}
```

### Step 3: Verify visually

- [x]Verificar no browser:
  - Selecionar contas → barra flutuante aparece na parte inferior
  - Clicar "Marcar como Pago" → dialog abre com data de hoje
  - Alterar data → funciona
  - Confirmar → contas sao atualizadas, selecao limpa, lista recarrega

### Step 4: Commit

- [x]`git commit -m "feat: add batch pay action bar and confirmation dialog"`

---

## Task 8: Run full test suite and verify

### Step 1: Backend tests

- [x]Run: `cd gestao-empresarial-backend && ./mvnw test`
- Expected: ALL PASS

### Step 2: Frontend tests

- [x]Run: `cd gestao-empresaial-frontend && npm test`
- Expected: ALL PASS (ou nenhuma regressao)

### Step 3: Final commit (if any fixes needed)

- [x]Fix e commit de qualquer ajuste necessario

---

## Resumo de Arquivos

| Arquivo | Acao |
|---------|------|
| `BankImportServiceImpl.java` | Modificar confirm() — status auto |
| `BankImportServiceImplTest.java` | 3 novos testes |
| `BatchPayRequestDTO.java` | Criar |
| `AccountService.java` (interface) | Adicionar batchPay() |
| `AccountServiceImpl.java` | Implementar batchPay() |
| `AccountServiceImplTest.java` | 3 novos testes |
| `AccountRepository.java` | Adicionar findByIdInAndCompanyId() |
| `AccountController.java` | Adicionar POST /batch-pay |
| `account.model.ts` | Adicionar BatchPayRequest |
| `account.service.ts` | Adicionar batchPay() |
| `account-list.component.ts` | Checkboxes, selecao, dialog, acoes |
| `account-list.component.html` | Template com checkboxes, barra, dialog |
