# Phase 6: Dashboard — Design Spec

**Data:** 19/02/2026
**Status:** Approved

---

## Resumo

Implementar o dashboard financeiro com 4 cards de resumo, 3 gráficos interativos (PrimeNG Charts/Chart.js) e filtro global de período (presets + customizado). Todos os widgets respondem ao mesmo filtro. Último passo do MVP.

---

## Decisões de Design

| Decisão | Escolha |
|---------|---------|
| Biblioteca de charts | PrimeNG Charts (wrapper Chart.js — já no stack) |
| Filtro de período | Presets rápidos + date range customizado |
| Cards de resumo | 4 fixos: A Pagar, A Receber, Receitas, Despesas |
| Granularidade dos gráficos | Sempre mensal (agrupado por mês) |
| Filtros adicionais | Nenhum — apenas período |
| Arquitetura backend | 4 endpoints separados chamados em paralelo |
| Período default | 1º dia do mês corrente até hoje |

---

## Backend

### Endpoints

Todos: `GET /api/dashboard/**`, autenticados, `X-Company-Id` obrigatório.
Query params: `?from=yyyy-MM-dd&to=yyyy-MM-dd` (ambos opcionais — default: mês corrente).

| Endpoint | Descrição |
|----------|-----------|
| `GET /api/dashboard/summary` | 4 KPIs totalizadores |
| `GET /api/dashboard/cash-flow` | Entradas vs saídas por mês |
| `GET /api/dashboard/revenue-expense` | Breakdown por categoria |
| `GET /api/dashboard/monthly-evolution` | Evolução mensal com saldo acumulado |

### DTOs de Resposta

```java
// Summary
record DashboardSummaryDTO(
    BigDecimal totalPayable,       // contas PAYABLE com status PENDING ou OVERDUE
    BigDecimal totalReceivable,    // contas RECEIVABLE com status PENDING
    BigDecimal totalRevenue,       // contas RECEIVABLE com status RECEIVED no período
    BigDecimal totalExpenses       // contas PAYABLE com status PAID no período
)

// Cash Flow
record CashFlowPointDTO(String month, BigDecimal revenue, BigDecimal expense)
// Retorna: List<CashFlowPointDTO> — um item por mês no período

// Revenue vs Expense
record RevenueExpenseItemDTO(String categoryName, String groupName, BigDecimal total, String type)
// Retorna: List<RevenueExpenseItemDTO> — um item por categoria com lançamentos no período

// Monthly Evolution
record MonthlyEvolutionPointDTO(String month, BigDecimal revenue, BigDecimal expense, BigDecimal balance)
// Retorna: List<MonthlyEvolutionPointDTO> — saldo = revenue - expense acumulado
```

### Queries

Todas filtram por `company_id`, `due_date BETWEEN :from AND :to`, `active = true`.

**summary** — 4 queries `SUM(amount)` com filtros de `type` e `status`:
- `totalPayable`: `type=PAYABLE AND status IN ('PENDING','OVERDUE')`
- `totalReceivable`: `type=RECEIVABLE AND status IN ('PENDING')`
- `totalRevenue`: `type=RECEIVABLE AND status='RECEIVED'`
- `totalExpenses`: `type=PAYABLE AND status='PAID'`

**cash-flow** — JPQL `GROUP BY FUNCTION('DATE_TRUNC', 'month', a.dueDate), a.type`:
- Agrupa por mês e tipo, soma `amount`
- Combina PAYABLE→expense e RECEIVABLE→revenue por mês

**revenue-expense** — JOIN com `Category` e `CategoryGroup`:
- `GROUP BY a.categoryId` com `SUM(amount)`
- Apenas lançamentos PAID/RECEIVED no período

**monthly-evolution** — mesmo que cash-flow + cálculo de saldo acumulado em memória no service

### Serviço

`DashboardService` + `DashboardServiceImpl` — 4 métodos públicos, todos `@Transactional(readOnly = true)`.
`DashboardController` — 4 endpoints mapeados em `/api/dashboard`.

---

## Frontend

### Layout

```
┌─────────────────────────────────────────────────────┐
│ Dashboard                   [Filtro de Período]      │
├─────────────┬─────────────┬─────────────┬───────────┤
│  A Pagar    │  A Receber  │  Receitas   │  Despesas  │
│  R$ X.XXX   │  R$ X.XXX   │  R$ X.XXX   │  R$ X.XXX  │
├─────────────┴─────────────┴─────────────┴───────────┤
│              Fluxo de Caixa (linha)                  │
│              receitas vs despesas por mês            │
├────────────────────────┬────────────────────────────┤
│  Receita x Despesa     │  Evolução Mensal            │
│  (barras horizontais   │  (barras agrupadas          │
│   por categoria)       │   revenue/expense/balance)  │
└────────────────────────┴────────────────────────────┘
```

### Filtro de Período

Presets: `Hoje | Esta semana | Este mês | Este trimestre | Este ano | Personalizado`

Ao selecionar "Personalizado": dois campos de data (início/fim) aparecem inline.
Signal `period: { from: string, to: string }` no `DashboardComponent` propagado via `input()`.

### Componentes

| Componente | Localização |
|-----------|-------------|
| `DashboardComponent` | `features/dashboard/pages/dashboard/` (já existe — substituir) |
| `DashboardFilterComponent` | `features/dashboard/components/dashboard-filter/` |
| `SummaryCardsComponent` | `features/dashboard/components/summary-cards/` |
| `CashFlowChartComponent` | `features/dashboard/components/cash-flow-chart/` |
| `RevenueExpenseChartComponent` | `features/dashboard/components/revenue-expense-chart/` |
| `MonthlyEvolutionChartComponent` | `features/dashboard/components/monthly-evolution-chart/` |
| `DashboardService` | `features/dashboard/services/dashboard.service.ts` |

### State Flow

```
DashboardComponent
  signal period = { from, to }   ← DashboardFilterComponent emite via output()
       │
       ├─ forkJoin([summary$, cashFlow$, revenueExpense$, monthlyEvolution$])
       │    chamado no init E sempre que period muda (effect())
       │
       ├─► SummaryCardsComponent       input(summary)
       ├─► CashFlowChartComponent      input(data)
       ├─► RevenueExpenseChartComponent input(data)
       └─► MonthlyEvolutionChartComponent input(data)
```

### Gráficos (PrimeNG `<p-chart>`)

| Gráfico | Tipo | Eixos |
|---------|------|-------|
| Fluxo de Caixa | `line` | X: meses, Y: R$, 2 datasets (Receitas/Despesas) |
| Receita x Despesa | `bar` (horizontal) | X: R$, Y: categorias, 1 dataset colorido por grupo |
| Evolução Mensal | `bar` | X: meses, Y: R$, 3 datasets (Receitas/Despesas/Saldo) |

### Estados

- **Carregando**: skeleton nos cards, spinner nos gráficos
- **Sem dados**: mensagem "Nenhum lançamento neste período" centralizada no lugar do gráfico
- **Erro**: toast de erro, botão "Tentar novamente"

---

## Edge Cases

| Caso | Comportamento |
|------|---------------|
| Período sem lançamentos | Todos os cards mostram R$ 0,00, gráficos mostram estado vazio |
| `from > to` no customizado | Frontend bloqueia — botão confirmar desabilitado |
| Empresa nova sem dados | Dashboard mostra estado vazio com orientação "Crie seu primeiro lançamento" |
| Muitas categorias no revenue-expense | Gráfico mostra top 10, resto agrupado em "Outros" |
