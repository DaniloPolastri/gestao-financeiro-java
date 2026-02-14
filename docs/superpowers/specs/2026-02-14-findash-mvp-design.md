# FinDash MVP — Design Spec

**Data:** 14/02/2026
**Status:** Approved

---

## Resumo

FinDash e um dashboard financeiro para PMEs brasileiras. Centraliza contas a pagar/receber, importacao bancaria (OFX/CSV) com vinculo a fornecedores, e visualizacao de fluxo de caixa. Arquitetura de microservices com Angular 21 + Spring Boot 4 + PostgreSQL.

---

## Decisoes Tecnicas

| Decisao | Escolha |
|---------|---------|
| Arquitetura | Microservices (3 servicos + API Gateway) |
| Java | 21 (LTS) |
| Inter-service communication | Spring Cloud OpenFeign |
| Database | PostgreSQL unico, schema por servico |
| Migrations | Flyway |
| Service discovery | Spring Cloud Gateway + URLs fixas (sem Eureka) |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Local dev | Docker Compose |
| DTO mapping | MapStruct |
| Frontend state | Signals (sem NgRx) |
| Frontend styling | Tailwind CSS 4 + PrimeNG |
| Frontend testing | Vitest |
| Backend testing | JUnit 5 + Mockito + Testcontainers |

---

## Git Workflow

- Cada fase de implementacao e desenvolvida em uma **branch separada** a partir da `master`
- Naming: `feature/<nome>` (ex: `feature/phase1-infra-auth`, `feature/phase2-company-service`)
- Commits frequentes durante o desenvolvimento
- Ao finalizar a fase, **criar Pull Request para `master`** para revisao antes do merge
- Nunca fazer merge direto na master sem PR aprovado

---

## Topologia dos Microservices

```
[Angular SPA :4200] → [API Gateway :8080]
                            ↓
           ┌────────────────┼────────────────┐
           ↓                ↓                ↓
    [Auth :8081]    [Company :8082]   [Financial :8083]
           └────────────────┼────────────────┘
                            ↓
                    [PostgreSQL :5432]
                    ├── auth_schema
                    ├── company_schema
                    └── financial_schema
```

- Gateway roteia por path prefix (`/api/auth/**` → :8081, etc.)
- JWT token propagado pelo Gateway para todos os servicos
- Financial/Company chamam Auth via OpenFeign para validacao
- Sem Eureka — URLs fixas configuradas no Gateway

---

## Estrutura do Repositorio

```
gestao-empresarial-microservico/
├── docs/
├── docker-compose.yml
├── gestao-empresaial-frontend/        # Angular SPA
├── gestao-empresarial-backend/        # Parent Maven POM
│   ├── pom.xml                        # Parent POM
│   ├── api-gateway/
│   ├── auth-service/
│   ├── company-service/
│   ├── financial-service/
│   └── shared/                        # Modulo compartilhado
```

### Modulo Shared

Codigo reutilizado por todos os servicos:

- `JwtAuthenticationFilter` — valida JWT (reusado em todos os servicos)
- `CompanyContextFilter` — extrai X-Company-Id via ThreadLocal
- `GlobalExceptionHandler` — @ControllerAdvice padronizado
- `ApiErrorResponse` — DTO de erro `{ status, message, errors[], timestamp }`
- `BaseEntity` — classe base com id, createdAt, updatedAt
- `UserContext` — DTO com dados do usuario extraidos do JWT

---

## Backend — Hexagonal Architecture

Cada servico segue a mesma estrutura:

```
{service}/src/main/java/com/findash/{service}/
├── domain/
│   ├── model/          # Entidades e Value Objects (nao sao @Entity JPA)
│   ├── port/
│   │   ├── in/         # Use case interfaces (entrada)
│   │   └── out/        # Repository interfaces (saida)
│   └── service/        # Implementacao dos use cases
├── adapter/
│   ├── in/web/         # Controllers REST
│   │   ├── dto/request/
│   │   ├── dto/response/
│   │   └── mapper/     # MapStruct mappers
│   └── out/
│       ├── persistence/
│       │   ├── entity/     # Entidades JPA (@Entity)
│       │   ├── repository/ # Spring Data repositories
│       │   └── adapter/    # Implementa port.out interfaces
│       └── security/       # JWT, etc.
└── config/              # Spring configs
```

**Regras:**
- `domain/` nunca importa de `adapter/`
- `domain/model/` != entidades JPA (separacao explicita)
- `adapter/out/persistence/adapter/` implementa `domain/port/out/` interfaces

---

## Modelo de Dados

### Auth Schema

**users:** id (UUID PK), name, email (UNIQUE), password (BCrypt), active, created_at, updated_at

**user_roles:** id (UUID PK), user_id (FK), company_id (UUID ref externa), role (ENUM: ADMIN/EDITOR/VIEWER), created_at

### Company Schema

**companies:** id (UUID PK), name, cnpj (UNIQUE nullable), segment, owner_id (UUID ref externa), active, created_at, updated_at

**company_members:** id (UUID PK), company_id (FK), user_id (UUID ref externa), status (ENUM: ACTIVE/INVITED/REMOVED), invited_at, joined_at

### Financial Schema

**category_groups:** id (UUID PK), company_id, name, type (ENUM: REVENUE/EXPENSE), is_default, created_at, updated_at

**categories:** id (UUID PK), group_id (FK), company_id, name, created_at, updated_at

**suppliers:** id (UUID PK), company_id, name, cnpj_cpf, email, phone, created_at, updated_at

**clients:** id (UUID PK), company_id, name, cnpj_cpf, email, phone, created_at, updated_at

**accounts:** id (UUID PK), company_id, type (ENUM: PAYABLE/RECEIVABLE), description, amount (DECIMAL 15,2), due_date, payment_date (nullable), status (ENUM: PENDING/PAID/RECEIVED/OVERDUE/PARTIAL), category_id (FK), supplier_id (FK nullable), client_id (FK nullable), recurrence_id (FK nullable), import_id (FK nullable), notes (nullable), created_by (UUID ref externa), created_at, updated_at

**recurrences:** id (UUID PK), company_id, frequency (ENUM: MONTHLY/WEEKLY/BIWEEKLY/YEARLY), start_date, end_date (nullable), max_occurrences (nullable), created_at

**bank_imports:** id (UUID PK), company_id, file_name, file_type (ENUM: OFX/CSV), status (ENUM: PENDING_REVIEW/COMPLETED/CANCELLED), total_records, imported_by (UUID ref externa), created_at

**bank_import_items:** id (UUID PK), import_id (FK), date, description, amount (DECIMAL 15,2), type (ENUM: CREDIT/DEBIT), supplier_id (FK nullable), category_id (FK nullable), matched (BOOLEAN), original_data (JSONB), created_at

**supplier_match_rules:** id (UUID PK), company_id, pattern, supplier_id (FK), category_id (FK nullable), confidence (DECIMAL 3,2), created_at

**Decisoes-chave:**
- `company_id` em todas as tabelas para multi-tenancy
- `accounts` unificada para pagar/receber (diferenciada por `type`)
- `supplier_match_rules` para sugestao automatica de fornecedor na importacao
- `bank_import_items.original_data` como JSONB para auditoria
- Referencias cross-schema por UUID sem FK real

---

## API Endpoints

### Auth Service (/api/auth)

- `POST /api/auth/register` — Criar conta (publico)
- `POST /api/auth/login` — Login, retorna JWT + refresh (publico)
- `POST /api/auth/refresh` — Refresh token (autenticado)
- `POST /api/auth/logout` — Invalida refresh (autenticado)
- `GET /api/auth/me` — Dados do usuario logado
- `PUT /api/auth/me` — Atualizar perfil
- `GET /api/auth/users` — Listar usuarios da empresa (ADMIN)
- `POST /api/auth/users/invite` — Convidar usuario (ADMIN)
- `PUT /api/auth/users/{id}/role` — Alterar role (ADMIN)
- `DELETE /api/auth/users/{id}` — Remover usuario (ADMIN)

### Company Service (/api/companies)

- `POST /api/companies` — Criar empresa (autenticado)
- `GET /api/companies` — Listar empresas do usuario
- `GET /api/companies/{id}` — Detalhe (membro)
- `PUT /api/companies/{id}` — Atualizar (ADMIN)
- `GET /api/companies/{id}/members` — Listar membros (ADMIN/EDITOR)
- `POST /api/companies/{id}/members/invite` — Convidar (ADMIN)
- `DELETE /api/companies/{id}/members/{userId}` — Remover (ADMIN)

### Financial Service (/api/financial)

**Contas:**
- `GET /api/financial/accounts` — Listar com filtros (EDITOR+)
- `GET /api/financial/accounts/{id}` — Detalhe (EDITOR+)
- `POST /api/financial/accounts` — Criar (EDITOR+)
- `PUT /api/financial/accounts/{id}` — Atualizar (EDITOR+)
- `PATCH /api/financial/accounts/{id}/pay` — Marcar como paga/recebida (EDITOR+)
- `DELETE /api/financial/accounts/{id}` — Excluir (EDITOR+)

**Categorias:**
- `GET /api/financial/category-groups` — Listar grupos (VIEWER+)
- `POST /api/financial/category-groups` — Criar grupo (ADMIN)
- `PUT /api/financial/category-groups/{id}` — Renomear (ADMIN)
- `DELETE /api/financial/category-groups/{id}` — Excluir (ADMIN)
- `GET /api/financial/category-groups/{id}/categories` — Listar categorias (VIEWER+)
- `POST /api/financial/category-groups/{id}/categories` — Criar categoria (ADMIN/EDITOR)
- `PUT /api/financial/categories/{id}` — Atualizar (ADMIN/EDITOR)
- `DELETE /api/financial/categories/{id}` — Excluir (ADMIN/EDITOR)

**Fornecedores/Clientes:**
- `GET/POST /api/financial/suppliers` — Listar/Criar
- `PUT/DELETE /api/financial/suppliers/{id}` — Atualizar/Excluir
- `GET/POST /api/financial/clients` — Listar/Criar
- `PUT/DELETE /api/financial/clients/{id}` — Atualizar/Excluir

**Importacao:**
- `POST /api/financial/imports/upload` — Upload OFX/CSV (EDITOR+)
- `GET /api/financial/imports/{id}` — Detalhe com items (EDITOR+)
- `PUT /api/financial/imports/{id}/items/{itemId}` — Atualizar item (EDITOR+)
- `POST /api/financial/imports/{id}/confirm` — Confirmar (EDITOR+)
- `POST /api/financial/imports/{id}/cancel` — Cancelar (EDITOR+)
- `GET /api/financial/imports/{id}/suggestions` — Sugestoes de fornecedor (EDITOR+)

**Dashboard:**
- `GET /api/financial/dashboard/summary` — Cards resumo (VIEWER+)
- `GET /api/financial/dashboard/cash-flow` — Grafico fluxo de caixa (VIEWER+)
- `GET /api/financial/dashboard/revenue-expense` — Grafico receita x despesa (VIEWER+)
- `GET /api/financial/dashboard/monthly-evolution` — Grafico evolucao mensal (VIEWER+)

Todos os endpoints do Financial Service recebem `company_id` via header `X-Company-Id`.

---

## Frontend Architecture

### Rotas (lazy-loaded)

```
/                           → redirect /dashboard
/login                      → LoginComponent
/register                   → RegisterComponent
/dashboard                  → DashboardComponent
/contas-a-pagar             → AccountsPayableListComponent
/contas-a-pagar/novo        → AccountsPayableFormComponent
/contas-a-pagar/:id         → AccountsPayableDetailComponent
/contas-a-pagar/:id/editar  → AccountsPayableFormComponent
/contas-a-receber           → (mesma estrutura)
/importacao                 → BankImportListComponent
/importacao/nova            → BankImportUploadComponent
/importacao/:id/revisao     → BankImportReviewComponent
/categorias                 → CategoriesComponent
/fornecedores               → SuppliersListComponent
/fornecedores/novo          → SupplierFormComponent
/fornecedores/:id/editar    → SupplierFormComponent
/clientes                   → (mesma estrutura)
/configuracoes              → CompanySettingsComponent
/configuracoes/usuarios     → UserManagementComponent
```

### Layout

- Header (64px): logo, company selector, user menu
- Sidebar (256px fixa): navegacao principal
- Content: max-w-7xl, centered

### Shared Components

- PageHeaderComponent, DataTableComponent, DateRangePickerComponent
- StatusTagComponent, CurrencyDisplayComponent, EmptyStateComponent
- ConfirmDialogComponent, FormFieldComponent

### Core Services

- AuthService: login, register, JWT, refresh
- CompanyService: empresa ativa (signal global), troca
- HttpInterceptor: injeta JWT + X-Company-Id
- AuthGuard / RoleGuard

### State: Signals para tudo, computed() para derivados. Sem NgRx.

---

## Autenticacao e Multi-tenancy

### JWT Flow

1. Login retorna accessToken (JWT, 15min) + refreshToken (UUID, 30 dias)
2. accessToken em memoria (signal), refreshToken em httpOnly cookie
3. HttpInterceptor adiciona `Authorization: Bearer {token}` + `X-Company-Id`
4. Token expirado (401) → interceptor chama refresh automaticamente
5. Cada servico valida JWT independentemente (shared secret)

### Multi-tenancy

1. Login → GET /api/companies → lista empresas do usuario
2. Primeira empresa (ou ultima usada) setada como ativa
3. Seletor no header para trocar
4. Todo request carrega X-Company-Id
5. Todo servico filtra por company_id obrigatoriamente

### RBAC

| Role | Ver | Criar/Editar | Gerenciar usuarios | Configurar empresa |
|------|-----|-------------|-------------------|-------------------|
| VIEWER | Sim | Nao | Nao | Nao |
| EDITOR | Sim | Sim | Nao | Nao |
| ADMIN | Sim | Sim | Sim | Sim |

---

## Error Handling

Formato padrao de erro:

```json
{
  "status": 400,
  "message": "Validacao falhou",
  "errors": [
    { "field": "amount", "message": "Valor deve ser maior que zero" }
  ],
  "timestamp": "2026-02-14T10:30:00Z"
}
```

Status codes: 400 (validacao), 401 (nao autenticado), 403 (sem permissao), 404 (nao encontrado), 409 (conflito), 422 (regra de negocio).

---

## Docker Compose

PostgreSQL 17 + 4 servicos Java + init-schemas.sql para criar os 3 schemas.

---

## Fases de Implementacao (Fatias Verticais)

### Fase 1: Infraestrutura + Auth Service
Backend: parent POM, Docker Compose, Gateway, Auth Service (register/login/JWT/RBAC), Flyway
Frontend: layout shell (sidebar/header), login/register, AuthService, interceptor, guard
Entregavel: usuario cria conta, faz login, ve layout vazio

### Fase 2: Company Service + Multi-empresa
Backend: Company Service (CRUD empresas/membros/convites), OpenFeign para Auth
Frontend: criar empresa, company selector, config empresa, gestao de usuarios
Entregavel: usuario cria empresa, convida membros, alterna entre empresas

### Fase 3: Financial Core (Categorias, Fornecedores, Clientes)
Backend: Financial Service scaffold, CRUD categorias/grupos (com seed), CRUD fornecedores, CRUD clientes
Frontend: tela categorias, tela fornecedores, tela clientes, shared components
Entregavel: usuario gerencia dados de referencia

### Fase 4: Contas a Pagar e Receber
Backend: CRUD accounts com filtros, recorrencia, marcar como pago, validacoes
Frontend: listagem + formulario contas a pagar/receber, filtros, status tags
Entregavel: analista cria, edita, filtra e paga contas

### Fase 5: Importacao OFX/CSV
Backend: parsing OFX (ofx4j) e CSV (Commons CSV), deteccao duplicados, sugestao fornecedor, confirmacao
Frontend: upload, tela revisao, seletores fornecedor/categoria, historico
Entregavel: analista importa extratos e vincula a fornecedores

### Fase 6: Dashboard
Backend: endpoints aggregation (summary, cash-flow, revenue-expense, monthly-evolution)
Frontend: cards resumo, 3 graficos (Chart.js/PrimeNG), DateRangePicker com presets
Entregavel: dashboard completo. MVP pronto.

---

## Testing Strategy

| Camada | Ferramenta | Escopo |
|--------|-----------|--------|
| Domain Services | JUnit 5 + Mockito | Logica de negocio (mocks nos ports) |
| Controllers | @WebMvcTest | Serializacao, validacao, status codes |
| Repositories | @DataJpaTest | Queries customizadas, migrations |
| Integration | @SpringBootTest + Testcontainers | Fluxos end-to-end com PostgreSQL real |
| Frontend Components | Vitest | Rendering, signals, interacoes |
| Frontend Services | Vitest | HTTP mockado, transformacoes |
