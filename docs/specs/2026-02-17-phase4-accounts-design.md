# Phase 4: Contas a Pagar e Receber — Design Spec

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:writing-plans to create the implementation plan from this design.

**Data:** 17/02/2026
**Status:** Approved

---

## Resumo

Implementar CRUD completo de contas a pagar e receber com filtros, paginacao, recorrencia, marcacao de pagamento (total/parcial), e deteccao automatica de atraso. A tabela `accounts` e unificada, diferenciada por `type` (PAYABLE/RECEIVABLE). Frontend compartilha componentes entre pagar e receber, diferenciando apenas labels e cores.

---

## Data Model

### accounts (financial_schema)

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| company_id | UUID NOT NULL | Multi-tenancy |
| type | ENUM('PAYABLE','RECEIVABLE') | |
| description | VARCHAR(255) NOT NULL | |
| amount | DECIMAL(15,2) NOT NULL | Always positive |
| due_date | DATE NOT NULL | |
| payment_date | DATE nullable | Set when paid/received |
| status | ENUM('PENDING','PAID','RECEIVED','OVERDUE','PARTIAL') | |
| category_id | UUID FK → categories | Required |
| supplier_id | UUID FK → suppliers | Required for PAYABLE, null for RECEIVABLE |
| client_id | UUID FK → clients | Required for RECEIVABLE, null for PAYABLE |
| recurrence_id | UUID FK → recurrences | nullable |
| notes | TEXT nullable | |
| active | BOOLEAN DEFAULT true | Soft delete |
| created_by | UUID | User who created |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### recurrences (financial_schema)

| Column | Type |
|--------|------|
| id | UUID PK |
| company_id | UUID NOT NULL |
| frequency | ENUM('MONTHLY','WEEKLY','BIWEEKLY','YEARLY') |
| start_date | DATE |
| end_date | DATE nullable |
| max_occurrences | INT nullable |
| created_at | TIMESTAMP |

### Key Decisions

- On creation with recurrence, generate **all entries upfront** (capped at 60 entries)
- Scheduled job runs daily at 00:30 to mark PENDING → OVERDUE when `due_date < today`
- `PAID` for PAYABLE, `RECEIVED` for RECEIVABLE
- `PARTIAL` when `amountPaid < amount`
- Soft delete via `active = false`

---

## API Endpoints

All under `/api/financial/accounts`. All require `X-Company-Id` header.

### Accounts CRUD

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/accounts` | List with filters + pagination |
| `GET` | `/accounts/{id}` | Detail |
| `POST` | `/accounts` | Create (+ generate recurrence entries) |
| `PUT` | `/accounts/{id}` | Update |
| `PATCH` | `/accounts/{id}/pay` | Mark as PAID/RECEIVED/PARTIAL |
| `DELETE` | `/accounts/{id}` | Soft delete |

### Query Filters (GET /accounts)

- `type` — PAYABLE or RECEIVABLE (required)
- `status` — comma-separated: PENDING, PAID, RECEIVED, OVERDUE, PARTIAL
- `categoryId` — UUID
- `supplierId` — UUID (for PAYABLE)
- `clientId` — UUID (for RECEIVABLE)
- `dueDateFrom` / `dueDateTo` — date range (ISO format)
- `search` — text search on description
- `page` / `size` / `sort` — pagination (default: size=20, sort=dueDate,asc)

### Request/Response DTOs

**CreateAccountRequestDTO:**
```json
{
  "type": "PAYABLE",
  "description": "Aluguel Escritorio",
  "amount": 2500.00,
  "dueDate": "2026-03-10",
  "categoryId": "uuid",
  "supplierId": "uuid",
  "clientId": null,
  "notes": "Ref marco 2026",
  "recurrence": {
    "frequency": "MONTHLY",
    "endDate": "2026-12-10",
    "maxOccurrences": null
  }
}
```

**PayAccountRequestDTO:**
```json
{
  "paymentDate": "2026-02-17",
  "amountPaid": 1500.00
}
```

If `amountPaid` is null or `>= amount` → status = PAID/RECEIVED.
If `amountPaid < amount` → status = PARTIAL.

**AccountResponseDTO:**
```json
{
  "id": "uuid",
  "type": "PAYABLE",
  "description": "Aluguel Escritorio",
  "amount": 2500.00,
  "dueDate": "2026-03-10",
  "paymentDate": null,
  "status": "PENDING",
  "category": { "id": "uuid", "name": "Operacional" },
  "supplier": { "id": "uuid", "name": "Imobiliaria XYZ" },
  "client": null,
  "notes": "Ref marco 2026",
  "recurrenceId": "uuid",
  "createdAt": "2026-02-17T10:00:00Z"
}
```

---

## Backend Architecture

Follows existing monolith patterns (`com.findash` package).

### Entities

- `Account.java` — extends `BaseEntity`, `@Enumerated` for type/status, `@ManyToOne(LAZY)` for category/supplier/client/recurrence
- `Recurrence.java` — extends `BaseEntity`
- `AccountType` enum — PAYABLE, RECEIVABLE
- `AccountStatus` enum — PENDING, PAID, RECEIVED, OVERDUE, PARTIAL

### Repositories

- `AccountRepository.java` — extends `JpaRepository` + `JpaSpecificationExecutor<Account>` for dynamic filtering with `Specification` + `Page` return
- `RecurrenceRepository.java` — basic CRUD

### Service

- `AccountService.java` (interface)
- `AccountServiceImpl.java`:
  - **create:** validates supplier required for PAYABLE, client required for RECEIVABLE. If recurrence provided, creates Recurrence entity + generates all Account entries (capped at 60)
  - **update:** updates mutable fields (description, amount, dueDate, categoryId, supplierId, clientId, notes)
  - **pay:** if `amountPaid` is null or `>= account.amount` → PAID (PAYABLE) or RECEIVED (RECEIVABLE). If `amountPaid < amount` → PARTIAL. Sets `paymentDate`. Cannot pay already PAID/RECEIVED account.
  - **delete:** soft delete (`active = false`)
  - **list:** uses `Specification` for dynamic filtering + `Pageable`

### Scheduled Job

- `AccountOverdueScheduler.java` — `@Scheduled(cron = "0 30 0 * * *")`
- Bulk updates: `UPDATE accounts SET status = 'OVERDUE' WHERE status = 'PENDING' AND due_date < CURRENT_DATE AND active = true`

### Specifications

- `AccountSpecifications.java` — static methods returning `Specification<Account>`:
  - `hasCompanyId(UUID)`, `hasType(AccountType)`, `hasStatus(List<AccountStatus>)`
  - `hasCategoryId(UUID)`, `hasSupplierId(UUID)`, `hasClientId(UUID)`
  - `dueDateBetween(LocalDate, LocalDate)`, `descriptionContains(String)`
  - `isActive()`

### DTOs

- `CreateAccountRequestDTO` (record) — with nested `RecurrenceRequestDTO` (optional)
- `UpdateAccountRequestDTO` (record)
- `PayAccountRequestDTO` (record) — paymentDate, amountPaid (optional)
- `AccountResponseDTO` (record) — with nested category/supplier/client name references
- `AccountFilterDTO` — query params object

### Mapper

- `AccountMapper.java` — MapStruct mapper for Entity ↔ DTO

### Tests (TDD)

- `AccountServiceImplTest.java` — Mockito unit tests:
  - Create payable requires supplier (fails without)
  - Create receivable requires client (fails without)
  - Create with recurrence generates correct number of entries
  - Recurrence capped at 60 entries
  - Pay sets PAID for PAYABLE, RECEIVED for RECEIVABLE
  - Partial payment sets PARTIAL status
  - Cannot pay already PAID/RECEIVED account
  - Delete sets active=false
  - List filters by type, status, date range

---

## Frontend Architecture

### Shared Components (both PAYABLE and RECEIVABLE)

Components are shared, differentiated by `type` route data.

**AccountListComponent:**
- Tab filters: Todos, Pendentes, Pagos/Recebidos, Atrasados
- Table: Fornecedor/Cliente, Descricao, Vencimento, Categoria, Status (badge), Valor
- Pagination controls
- Search input + date range filter
- "Nova Conta" button → form route

**AccountFormComponent:**
- Reactive form: Descricao, Valor (R$ format), Vencimento, Categoria (dropdown grouped by group), Fornecedor/Cliente (dropdown, conditionally shown), Observacoes
- Recurrence section (collapsible): Frequencia dropdown, Data final or Num. parcelas
- Edit mode: loads existing account, hides recurrence

**AccountDetailComponent:**
- Full account info display
- "Marcar como Pago/Recebido" button → pay dialog
- Pay dialog: date picker + optional partial amount input

### Service

- `AccountService` — signals-based state + HTTP client
- `listAccounts(type, filters, page)` → paginated response
- `getById(id)`, `create(dto)`, `update(id, dto)`, `pay(id, dto)`, `delete(id)`

### Routes

```
/contas-a-pagar             → AccountListComponent (data: { type: 'PAYABLE' })
/contas-a-pagar/novo        → AccountFormComponent (data: { type: 'PAYABLE' })
/contas-a-pagar/:id         → AccountDetailComponent
/contas-a-pagar/:id/editar  → AccountFormComponent (data: { type: 'PAYABLE' })
/contas-a-receber           → same structure (data: { type: 'RECEIVABLE' })
```

### UI Patterns

- Status badges: Pendente (amber), Pago/Recebido (emerald), Atrasado (red), Parcial (blue)
- Values: PAYABLE in red mono (`- R$ 2.500,00`), RECEIVABLE in emerald mono (`+ R$ 4.500,00`)
- Category shown as blue text tag
- Replaces current placeholder mock data with real API calls

---

## Flyway Migration

`V4__create_accounts_tables.sql`:
- Creates `recurrences` table
- Creates `accounts` table with FKs to categories, suppliers, clients, recurrences
- Indices on (company_id, type, status), (company_id, due_date), (company_id, supplier_id), (company_id, client_id)

---

## Validation Rules

- `amount > 0`
- `dueDate` required
- `categoryId` must exist and belong to same company
- PAYABLE requires `supplierId` (must exist, same company, active)
- RECEIVABLE requires `clientId` (must exist, same company, active)
- Recurrence: `endDate` or `maxOccurrences` required (at least one), max 60 generated entries
- Pay: cannot pay PAID/RECEIVED account, `paymentDate` required, `amountPaid > 0` if provided
