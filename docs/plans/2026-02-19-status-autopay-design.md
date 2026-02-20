# Design: Status Automático no Import + Pagamento em Lote

**Data**: 2026-02-19
**Status**: Aprovado

## Problema

Transações importadas de extratos bancários (OFX/CSV/PDF) são registros de movimentações que já ocorreram. No entanto, o sistema cria todas as contas com status `PENDING`, fazendo com que pagamentos já efetuados (Pix, débitos automáticos) apareçam como pendentes ou atrasados. Além disso, não existe interface para o usuário marcar manualmente contas como pagas.

## Solução

Duas funcionalidades complementares:

### 1. Status automático no import

No método `confirm()` do `BankImportServiceImpl`, ao criar cada `Account` a partir de um `BankImportItem`, verificar a data da transação:

- **data < hoje**: status = `PAID` (PAYABLE) ou `RECEIVED` (RECEIVABLE), com `paymentDate = item.getDate()`
- **data >= hoje**: status = `PENDING` (comportamento atual, cobre agendamentos futuros)

**Arquivo**: `BankImportServiceImpl.java`, método `confirm()`, linhas 181-193.

**Sem migration de dados retroativa** — contas existentes com status incorreto serão corrigidas pelo usuário com a funcionalidade de pagamento em lote (item 2).

### 2. Pagamento em lote (batch pay)

#### Backend

**Novo DTO**: `BatchPayRequestDTO`
```java
record BatchPayRequestDTO(
    @NotEmpty List<UUID> accountIds,
    @NotNull LocalDate paymentDate
)
```

**Novo endpoint**: `POST /api/accounts/batch-pay`
- Recebe lista de IDs + data de pagamento
- Busca contas pelo companyId + IDs
- Ignora silenciosamente contas já PAID/RECEIVED
- Seta status PAID/RECEIVED e paymentDate para cada conta elegível
- Retorna lista de contas atualizadas

**Novo método no service**: `batchPay(UUID companyId, BatchPayRequestDTO request) -> List<AccountResponseDTO>`

#### Frontend

**Mudanças no `AccountListComponent`**:

1. **Checkboxes na tabela**: Coluna extra no início com checkboxes por linha + checkbox no header para selecionar todos da página. Contas já PAID/RECEIVED não mostram checkbox.

2. **Barra de ações flutuante**: Aparece quando há seleção, exibindo:
   - Quantidade selecionada (ex: "3 contas selecionadas")
   - Botão "Marcar como Pago" / "Marcar como Recebido"
   - Botão "Cancelar seleção"

3. **Dialog de confirmação**: Ao clicar no botão de pagar:
   - Campo de data com default = hoje, editável pelo usuário
   - Botões "Confirmar" e "Cancelar"
   - Após confirmar, chama `POST /api/accounts/batch-pay` e recarrega a lista

**Novos signals**:
- `selectedIds: Signal<Set<string>>`
- `showPayDialog: Signal<boolean>`
- `paymentDate: Signal<string>` (default: hoje)

**Novo método no `AccountService`**:
- `batchPay(data: { accountIds: string[], paymentDate: string }): Observable<AccountResponse[]>`

## Arquivos impactados

### Backend
- `BankImportServiceImpl.java` — lógica de status no `confirm()`
- `AccountServiceImpl.java` — novo método `batchPay()`
- `AccountController.java` — novo endpoint `POST /batch-pay`
- `AccountService.java` (interface) — assinatura do `batchPay()`
- Novo: `BatchPayRequestDTO.java`

### Frontend
- `account-list.component.ts` — checkboxes, seleção, dialog, ações
- `account-list.component.html` — template com checkboxes, barra de ações, dialog
- `account.service.ts` — método `batchPay()`
- `account.model.ts` — interface `BatchPayRequest`
