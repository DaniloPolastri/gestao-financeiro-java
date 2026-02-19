# Phase 6: Dashboard — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implementar o dashboard financeiro completo com 4 KPI cards, 3 gráficos interativos (PrimeNG Charts) e filtro global de período (presets + customizado), completando o MVP.

**Architecture:** 4 endpoints `GET /api/dashboard/**` independentes chamados em paralelo via `forkJoin`. Filtro de período global no `DashboardComponent` propagado via `input()` para cada widget filho. Sem filtros adicionais além do período.

**Tech Stack:** Java 17, Spring Boot 4, JPQL aggregation queries, Angular 21, PrimeNG 21 Charts, Chart.js, Signals, `forkJoin`.

**Design Spec:** `docs/specs/2026-02-19-phase6-dashboard-design.md`

---

## Checklist de Progresso

- [ ] Task 1: Branch + instalar chart.js
- [ ] Task 2: Backend DTOs do dashboard
- [ ] Task 3: Queries JPQL no AccountRepository
- [ ] Task 4: DashboardService — summary e cash-flow
- [ ] Task 5: DashboardService — revenue-expense e monthly-evolution
- [ ] Task 6: DashboardController
- [ ] Task 7: Testes do DashboardService
- [ ] Task 8: Frontend — models e DashboardService
- [ ] Task 9: Frontend — DashboardFilterComponent
- [ ] Task 10: Frontend — SummaryCardsComponent
- [ ] Task 11: Frontend — CashFlowChartComponent
- [ ] Task 12: Frontend — RevenueExpenseChartComponent
- [ ] Task 13: Frontend — MonthlyEvolutionChartComponent
- [ ] Task 14: Frontend — DashboardComponent (orquestrador)
- [ ] Task 15: Commit final, push e PR

---

## Task 1: Branch + instalar chart.js

**Files:**
- Modify: `gestao-empresaial-frontend/package.json`

- [ ] Criar branch a partir da master:
  ```bash
  git checkout master && git pull origin master
  git checkout -b feature/phase6-dashboard
  ```

- [ ] Instalar `chart.js` (peer dependency do `<p-chart>` do PrimeNG):
  ```bash
  cd gestao-empresaial-frontend && npm install chart.js
  ```

- [ ] Verificar que o frontend compila:
  ```bash
  npm run build -- --configuration development 2>&1 | tail -5
  ```
  Esperado: `Build at:` sem erros.

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/package.json gestao-empresaial-frontend/package-lock.json
  git commit -m "build: install chart.js for PrimeNG Charts"
  ```

---

## Task 2: Backend DTOs do dashboard

**Files:**
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/dashboard/DashboardSummaryDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/dashboard/CashFlowPointDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/dashboard/RevenueExpenseItemDTO.java`
- Create: `gestao-empresarial-backend/src/main/java/com/findash/dto/dashboard/MonthlyEvolutionPointDTO.java`

- [ ] Criar `dto/dashboard/DashboardSummaryDTO.java`:
  ```java
  package com.findash.dto.dashboard;

  import java.math.BigDecimal;

  public record DashboardSummaryDTO(
      BigDecimal totalPayable,
      BigDecimal totalReceivable,
      BigDecimal totalRevenue,
      BigDecimal totalExpenses
  ) {}
  ```

- [ ] Criar `dto/dashboard/CashFlowPointDTO.java`:
  ```java
  package com.findash.dto.dashboard;

  import java.math.BigDecimal;

  public record CashFlowPointDTO(
      String month,
      BigDecimal revenue,
      BigDecimal expense
  ) {}
  ```

- [ ] Criar `dto/dashboard/RevenueExpenseItemDTO.java`:
  ```java
  package com.findash.dto.dashboard;

  import java.math.BigDecimal;

  public record RevenueExpenseItemDTO(
      String categoryName,
      String groupName,
      BigDecimal total,
      String type
  ) {}
  ```

- [ ] Criar `dto/dashboard/MonthlyEvolutionPointDTO.java`:
  ```java
  package com.findash.dto.dashboard;

  import java.math.BigDecimal;

  public record MonthlyEvolutionPointDTO(
      String month,
      BigDecimal revenue,
      BigDecimal expense,
      BigDecimal balance
  ) {}
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```
  Esperado: `BUILD SUCCESS`

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/dto/dashboard/
  git commit -m "feat(dto): add dashboard DTOs for summary, cash-flow, revenue-expense, monthly-evolution"
  ```

---

## Task 3: Queries JPQL no AccountRepository

**Files:**
- Modify: `gestao-empresarial-backend/src/main/java/com/findash/repository/AccountRepository.java`

Adicionar 4 queries ao `AccountRepository`. Todas filtram por `company_id`, `dueDate BETWEEN from AND to`, `active = true`.

- [ ] Adicionar query para summary — 4 somas separadas:
  ```java
  @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Account a " +
         "WHERE a.companyId = :companyId AND a.type = :type " +
         "AND a.status IN :statuses AND a.active = true " +
         "AND a.dueDate BETWEEN :from AND :to")
  BigDecimal sumByTypeAndStatuses(
      @Param("companyId") UUID companyId,
      @Param("type") AccountType type,
      @Param("statuses") List<AccountStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
  ```

- [ ] Adicionar query para cash-flow (agrupado por mês e tipo):
  ```java
  @Query("SELECT FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM') as month, " +
         "a.type as type, COALESCE(SUM(a.amount), 0) as total " +
         "FROM Account a " +
         "WHERE a.companyId = :companyId AND a.active = true " +
         "AND a.dueDate BETWEEN :from AND :to " +
         "AND a.status IN :statuses " +
         "GROUP BY FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM'), a.type " +
         "ORDER BY FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM')")
  List<Object[]> findMonthlyTotalsByType(
      @Param("companyId") UUID companyId,
      @Param("statuses") List<AccountStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
  ```

- [ ] Adicionar query para revenue-expense por categoria:
  ```java
  @Query("SELECT c.name as categoryName, g.name as groupName, " +
         "a.type as type, COALESCE(SUM(a.amount), 0) as total " +
         "FROM Account a " +
         "JOIN Category c ON c.id = a.categoryId " +
         "JOIN CategoryGroup g ON g.id = c.groupId " +
         "WHERE a.companyId = :companyId AND a.active = true " +
         "AND a.dueDate BETWEEN :from AND :to " +
         "AND a.status IN :statuses " +
         "GROUP BY c.name, g.name, a.type " +
         "ORDER BY total DESC")
  List<Object[]> findRevenueExpenseByCategory(
      @Param("companyId") UUID companyId,
      @Param("statuses") List<AccountStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);
  ```

- [ ] Adicionar os imports necessários no topo do arquivo:
  ```java
  import com.findash.entity.AccountStatus;
  import com.findash.entity.AccountType;
  import java.math.BigDecimal;
  import java.util.List;
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/repository/AccountRepository.java
  git commit -m "feat(repository): add dashboard aggregation queries to AccountRepository"
  ```

---

## Task 4: DashboardService — summary e cash-flow

**Files:**
- Create: `...service/DashboardService.java`
- Create: `...service/impl/DashboardServiceImpl.java`

- [ ] Criar `service/DashboardService.java`:
  ```java
  package com.findash.service;

  import com.findash.dto.dashboard.*;
  import java.time.LocalDate;
  import java.util.List;
  import java.util.UUID;

  public interface DashboardService {
      DashboardSummaryDTO getSummary(UUID companyId, LocalDate from, LocalDate to);
      List<CashFlowPointDTO> getCashFlow(UUID companyId, LocalDate from, LocalDate to);
      List<RevenueExpenseItemDTO> getRevenueExpense(UUID companyId, LocalDate from, LocalDate to);
      List<MonthlyEvolutionPointDTO> getMonthlyEvolution(UUID companyId, LocalDate from, LocalDate to);
  }
  ```

- [ ] Criar `service/impl/DashboardServiceImpl.java` com `getSummary` e `getCashFlow`:
  ```java
  package com.findash.service.impl;

  import com.findash.dto.dashboard.*;
  import com.findash.entity.AccountStatus;
  import com.findash.entity.AccountType;
  import com.findash.repository.AccountRepository;
  import com.findash.service.DashboardService;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.time.format.DateTimeFormatter;
  import java.util.*;

  @Service
  @Transactional(readOnly = true)
  public class DashboardServiceImpl implements DashboardService {

      private static final List<AccountStatus> PENDING_STATUSES =
          List.of(AccountStatus.PENDING, AccountStatus.OVERDUE);
      private static final List<AccountStatus> PAID_STATUSES =
          List.of(AccountStatus.PAID, AccountStatus.RECEIVED, AccountStatus.PARTIAL);
      private static final DateTimeFormatter MONTH_FMT =
          DateTimeFormatter.ofPattern("yyyy-MM");

      private final AccountRepository accountRepository;

      public DashboardServiceImpl(AccountRepository accountRepository) {
          this.accountRepository = accountRepository;
      }

      @Override
      public DashboardSummaryDTO getSummary(UUID companyId, LocalDate from, LocalDate to) {
          BigDecimal totalPayable = accountRepository.sumByTypeAndStatuses(
              companyId, AccountType.PAYABLE, PENDING_STATUSES, from, to);
          BigDecimal totalReceivable = accountRepository.sumByTypeAndStatuses(
              companyId, AccountType.RECEIVABLE, List.of(AccountStatus.PENDING), from, to);
          BigDecimal totalRevenue = accountRepository.sumByTypeAndStatuses(
              companyId, AccountType.RECEIVABLE, List.of(AccountStatus.RECEIVED), from, to);
          BigDecimal totalExpenses = accountRepository.sumByTypeAndStatuses(
              companyId, AccountType.PAYABLE, List.of(AccountStatus.PAID), from, to);

          return new DashboardSummaryDTO(totalPayable, totalReceivable, totalRevenue, totalExpenses);
      }

      @Override
      public List<CashFlowPointDTO> getCashFlow(UUID companyId, LocalDate from, LocalDate to) {
          List<AccountStatus> statuses = new ArrayList<>(PENDING_STATUSES);
          statuses.addAll(PAID_STATUSES);

          List<Object[]> rows = accountRepository.findMonthlyTotalsByType(
              companyId, statuses, from, to);

          // Agrupar por mês
          Map<String, BigDecimal[]> byMonth = new LinkedHashMap<>();
          for (Object[] row : rows) {
              String month = (String) row[0];
              AccountType type = (AccountType) row[1];
              BigDecimal total = (BigDecimal) row[2];
              byMonth.putIfAbsent(month, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
              if (type == AccountType.RECEIVABLE) {
                  byMonth.get(month)[0] = total; // revenue
              } else {
                  byMonth.get(month)[1] = total; // expense
              }
          }

          return byMonth.entrySet().stream()
              .map(e -> new CashFlowPointDTO(e.getKey(), e.getValue()[0], e.getValue()[1]))
              .toList();
      }

      // Implementados na Task 5
      @Override
      public List<RevenueExpenseItemDTO> getRevenueExpense(UUID companyId, LocalDate from, LocalDate to) {
          return List.of();
      }

      @Override
      public List<MonthlyEvolutionPointDTO> getMonthlyEvolution(UUID companyId, LocalDate from, LocalDate to) {
          return List.of();
      }
  }
  ```

- [ ] Compilar:
  ```bash
  cd gestao-empresarial-backend && ./mvnw compile -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/DashboardService.java
  git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/DashboardServiceImpl.java
  git commit -m "feat(service): add DashboardService with getSummary and getCashFlow"
  ```

---

## Task 5: DashboardService — revenue-expense e monthly-evolution

**Files:**
- Modify: `...service/impl/DashboardServiceImpl.java`

- [ ] Implementar `getRevenueExpense`:
  ```java
  @Override
  public List<RevenueExpenseItemDTO> getRevenueExpense(UUID companyId, LocalDate from, LocalDate to) {
      List<Object[]> rows = accountRepository.findRevenueExpenseByCategory(
          companyId, PAID_STATUSES, from, to);

      List<RevenueExpenseItemDTO> result = rows.stream()
          .map(row -> new RevenueExpenseItemDTO(
              (String) row[0],
              (String) row[1],
              (AccountType) row[2] == AccountType.RECEIVABLE ? "REVENUE" : "EXPENSE",
              (BigDecimal) row[3]
          ))
          .toList();

      // Top 10 + agrupar resto em "Outros"
      if (result.size() <= 10) return result;

      List<RevenueExpenseItemDTO> top10 = result.subList(0, 10);
      BigDecimal othersTotal = result.subList(10, result.size()).stream()
          .map(RevenueExpenseItemDTO::total)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      List<RevenueExpenseItemDTO> final_ = new ArrayList<>(top10);
      final_.add(new RevenueExpenseItemDTO("Outros", "—", "OTHER", othersTotal));
      return final_;
  }
  ```

  **Atenção:** o record `RevenueExpenseItemDTO` tem os campos `(categoryName, groupName, total, type)` — ajustar a ordem correta:
  ```java
  // Correto (categoryName, groupName, total, type):
  new RevenueExpenseItemDTO(
      (String) row[0],          // categoryName
      (String) row[1],          // groupName
      (BigDecimal) row[3],      // total
      (AccountType) row[2] == AccountType.RECEIVABLE ? "REVENUE" : "EXPENSE"  // type
  )
  ```

- [ ] Implementar `getMonthlyEvolution`:
  ```java
  @Override
  public List<MonthlyEvolutionPointDTO> getMonthlyEvolution(UUID companyId, LocalDate from, LocalDate to) {
      // Reutiliza o mesmo agrupamento do cashFlow mas calcula saldo acumulado
      List<CashFlowPointDTO> cashFlow = getCashFlow(companyId, from, to);

      BigDecimal accumulated = BigDecimal.ZERO;
      List<MonthlyEvolutionPointDTO> result = new ArrayList<>();
      for (CashFlowPointDTO point : cashFlow) {
          BigDecimal balance = point.revenue().subtract(point.expense());
          accumulated = accumulated.add(balance);
          result.add(new MonthlyEvolutionPointDTO(
              point.month(), point.revenue(), point.expense(), accumulated));
      }
      return result;
  }
  ```

- [ ] Compilar e rodar todos os testes existentes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```
  Esperado: `BUILD SUCCESS`

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/service/impl/DashboardServiceImpl.java
  git commit -m "feat(service): implement getRevenueExpense and getMonthlyEvolution in DashboardService"
  ```

---

## Task 6: DashboardController

**Files:**
- Create: `...controller/DashboardController.java`

- [ ] Criar `controller/DashboardController.java`:
  ```java
  package com.findash.controller;

  import com.findash.dto.dashboard.*;
  import com.findash.security.CompanyContextHolder;
  import com.findash.service.DashboardService;
  import org.springframework.format.annotation.DateTimeFormat;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.*;

  import java.time.LocalDate;
  import java.util.List;
  import java.util.UUID;

  @RestController
  @RequestMapping("/api/dashboard")
  public class DashboardController {

      private final DashboardService dashboardService;

      public DashboardController(DashboardService dashboardService) {
          this.dashboardService = dashboardService;
      }

      @GetMapping("/summary")
      public ResponseEntity<DashboardSummaryDTO> getSummary(
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
          UUID companyId = CompanyContextHolder.get();
          LocalDate[] range = resolveRange(from, to);
          return ResponseEntity.ok(dashboardService.getSummary(companyId, range[0], range[1]));
      }

      @GetMapping("/cash-flow")
      public ResponseEntity<List<CashFlowPointDTO>> getCashFlow(
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
          UUID companyId = CompanyContextHolder.get();
          LocalDate[] range = resolveRange(from, to);
          return ResponseEntity.ok(dashboardService.getCashFlow(companyId, range[0], range[1]));
      }

      @GetMapping("/revenue-expense")
      public ResponseEntity<List<RevenueExpenseItemDTO>> getRevenueExpense(
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
          UUID companyId = CompanyContextHolder.get();
          LocalDate[] range = resolveRange(from, to);
          return ResponseEntity.ok(dashboardService.getRevenueExpense(companyId, range[0], range[1]));
      }

      @GetMapping("/monthly-evolution")
      public ResponseEntity<List<MonthlyEvolutionPointDTO>> getMonthlyEvolution(
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
          UUID companyId = CompanyContextHolder.get();
          LocalDate[] range = resolveRange(from, to);
          return ResponseEntity.ok(dashboardService.getMonthlyEvolution(companyId, range[0], range[1]));
      }

      /** Se não informado, usa o mês corrente (dia 1 até hoje). */
      private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
          LocalDate today = LocalDate.now();
          LocalDate resolvedFrom = from != null ? from : today.withDayOfMonth(1);
          LocalDate resolvedTo = to != null ? to : today;
          return new LocalDate[]{resolvedFrom, resolvedTo};
      }
  }
  ```

- [ ] Compilar e rodar todos os testes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/main/java/com/findash/controller/DashboardController.java
  git commit -m "feat(controller): add DashboardController with 4 endpoints"
  ```

---

## Task 7: Testes do DashboardService

**Files:**
- Create: `...test/java/com/findash/service/impl/DashboardServiceImplTest.java`

- [ ] Criar `DashboardServiceImplTest.java`:
  ```java
  package com.findash.service.impl;

  import com.findash.dto.dashboard.*;
  import com.findash.entity.AccountStatus;
  import com.findash.entity.AccountType;
  import com.findash.repository.AccountRepository;
  import org.junit.jupiter.api.BeforeEach;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;

  import java.math.BigDecimal;
  import java.time.LocalDate;
  import java.util.List;
  import java.util.UUID;

  import static org.junit.jupiter.api.Assertions.*;
  import static org.mockito.ArgumentMatchers.*;
  import static org.mockito.Mockito.*;

  @ExtendWith(MockitoExtension.class)
  class DashboardServiceImplTest {

      @Mock
      private AccountRepository accountRepository;

      private DashboardServiceImpl service;
      private UUID companyId;
      private LocalDate from;
      private LocalDate to;

      @BeforeEach
      void setUp() {
          service = new DashboardServiceImpl(accountRepository);
          companyId = UUID.randomUUID();
          from = LocalDate.of(2026, 1, 1);
          to = LocalDate.of(2026, 1, 31);
      }

      // --- SUMMARY ---

      @Test
      void getSummary_returnsAggregatedValues() {
          when(accountRepository.sumByTypeAndStatuses(eq(companyId), eq(AccountType.PAYABLE), anyList(), eq(from), eq(to)))
              .thenReturn(new BigDecimal("5000.00"));
          when(accountRepository.sumByTypeAndStatuses(eq(companyId), eq(AccountType.RECEIVABLE), eq(List.of(AccountStatus.PENDING)), eq(from), eq(to)))
              .thenReturn(new BigDecimal("3000.00"));
          when(accountRepository.sumByTypeAndStatuses(eq(companyId), eq(AccountType.RECEIVABLE), eq(List.of(AccountStatus.RECEIVED)), eq(from), eq(to)))
              .thenReturn(new BigDecimal("8000.00"));
          when(accountRepository.sumByTypeAndStatuses(eq(companyId), eq(AccountType.PAYABLE), eq(List.of(AccountStatus.PAID)), eq(from), eq(to)))
              .thenReturn(new BigDecimal("2000.00"));

          DashboardSummaryDTO result = service.getSummary(companyId, from, to);

          assertEquals(new BigDecimal("5000.00"), result.totalPayable());
          assertEquals(new BigDecimal("3000.00"), result.totalReceivable());
          assertEquals(new BigDecimal("8000.00"), result.totalRevenue());
          assertEquals(new BigDecimal("2000.00"), result.totalExpenses());
      }

      @Test
      void getSummary_noAccounts_returnsZeros() {
          when(accountRepository.sumByTypeAndStatuses(any(), any(), anyList(), any(), any()))
              .thenReturn(BigDecimal.ZERO);

          DashboardSummaryDTO result = service.getSummary(companyId, from, to);

          assertEquals(BigDecimal.ZERO, result.totalPayable());
          assertEquals(BigDecimal.ZERO, result.totalRevenue());
      }

      // --- CASH FLOW ---

      @Test
      void getCashFlow_groupsByMonthAndType() {
          List<Object[]> mockRows = List.of(
              new Object[]{"2026-01", AccountType.RECEIVABLE, new BigDecimal("3000")},
              new Object[]{"2026-01", AccountType.PAYABLE, new BigDecimal("1500")},
              new Object[]{"2026-02", AccountType.RECEIVABLE, new BigDecimal("4000")}
          );
          when(accountRepository.findMonthlyTotalsByType(eq(companyId), anyList(), eq(from), eq(to)))
              .thenReturn(mockRows);

          List<CashFlowPointDTO> result = service.getCashFlow(companyId, from, to);

          assertEquals(2, result.size());
          CashFlowPointDTO jan = result.stream().filter(p -> "2026-01".equals(p.month())).findFirst().orElseThrow();
          assertEquals(new BigDecimal("3000"), jan.revenue());
          assertEquals(new BigDecimal("1500"), jan.expense());
      }

      @Test
      void getCashFlow_noData_returnsEmptyList() {
          when(accountRepository.findMonthlyTotalsByType(any(), anyList(), any(), any()))
              .thenReturn(List.of());

          List<CashFlowPointDTO> result = service.getCashFlow(companyId, from, to);

          assertTrue(result.isEmpty());
      }

      // --- MONTHLY EVOLUTION ---

      @Test
      void getMonthlyEvolution_calculatesAccumulatedBalance() {
          List<Object[]> mockRows = List.of(
              new Object[]{"2026-01", AccountType.RECEIVABLE, new BigDecimal("3000")},
              new Object[]{"2026-01", AccountType.PAYABLE, new BigDecimal("1000")},
              new Object[]{"2026-02", AccountType.RECEIVABLE, new BigDecimal("2000")},
              new Object[]{"2026-02", AccountType.PAYABLE, new BigDecimal("500")}
          );
          when(accountRepository.findMonthlyTotalsByType(any(), anyList(), any(), any()))
              .thenReturn(mockRows);

          List<MonthlyEvolutionPointDTO> result = service.getMonthlyEvolution(companyId, from, to);

          assertEquals(2, result.size());
          // Jan: revenue=3000, expense=1000, balance=2000
          assertEquals(new BigDecimal("2000"), result.get(0).balance());
          // Feb: revenue=2000, expense=500, accumulated=2000+1500=3500
          assertEquals(new BigDecimal("3500"), result.get(1).balance());
      }

      // --- REVENUE EXPENSE ---

      @Test
      void getRevenueExpense_top10_returnsAllIfLessOrEqual10() {
          List<Object[]> mockRows = List.of(
              new Object[]{"Aluguel", "Despesas Fixas", AccountType.PAYABLE, new BigDecimal("2000")},
              new Object[]{"Vendas", "Receitas", AccountType.RECEIVABLE, new BigDecimal("5000")}
          );
          when(accountRepository.findRevenueExpenseByCategory(any(), anyList(), any(), any()))
              .thenReturn(mockRows);

          List<RevenueExpenseItemDTO> result = service.getRevenueExpense(companyId, from, to);

          assertEquals(2, result.size());
      }

      @Test
      void getRevenueExpense_moreThan10_groupsRemainingInOthers() {
          // 12 rows
          List<Object[]> mockRows = new java.util.ArrayList<>();
          for (int i = 0; i < 12; i++) {
              mockRows.add(new Object[]{"Cat " + i, "Grupo", AccountType.PAYABLE, new BigDecimal("100")});
          }
          when(accountRepository.findRevenueExpenseByCategory(any(), anyList(), any(), any()))
              .thenReturn(mockRows);

          List<RevenueExpenseItemDTO> result = service.getRevenueExpense(companyId, from, to);

          assertEquals(11, result.size()); // 10 + "Outros"
          assertEquals("Outros", result.get(10).categoryName());
          assertEquals(new BigDecimal("200"), result.get(10).total()); // 2 itens * 100
      }
  }
  ```

- [ ] Rodar os testes:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test -q
  ```
  Esperado: `BUILD SUCCESS`, todos verdes.

- [ ] Commit:
  ```bash
  git add gestao-empresarial-backend/src/test/java/com/findash/service/impl/DashboardServiceImplTest.java
  git commit -m "test(dashboard): add unit tests for DashboardServiceImpl"
  ```

---

## Task 8: Frontend — models e DashboardService

**Files:**
- Create: `gestao-empresaial-frontend/src/app/features/dashboard/models/dashboard.model.ts`
- Create: `gestao-empresaial-frontend/src/app/features/dashboard/services/dashboard.service.ts`

- [ ] Criar `models/dashboard.model.ts`:
  ```typescript
  export interface DashboardSummary {
    totalPayable: number;
    totalReceivable: number;
    totalRevenue: number;
    totalExpenses: number;
  }

  export interface CashFlowPoint {
    month: string;
    revenue: number;
    expense: number;
  }

  export interface RevenueExpenseItem {
    categoryName: string;
    groupName: string;
    total: number;
    type: 'REVENUE' | 'EXPENSE' | 'OTHER';
  }

  export interface MonthlyEvolutionPoint {
    month: string;
    revenue: number;
    expense: number;
    balance: number;
  }

  export interface DashboardPeriod {
    from: string; // yyyy-MM-dd
    to: string;   // yyyy-MM-dd
  }
  ```

- [ ] Criar `services/dashboard.service.ts`:
  ```typescript
  import { inject, Injectable } from '@angular/core';
  import { HttpClient, HttpParams } from '@angular/common/http';
  import { forkJoin, Observable } from 'rxjs';
  import {
    CashFlowPoint,
    DashboardPeriod,
    DashboardSummary,
    MonthlyEvolutionPoint,
    RevenueExpenseItem,
  } from '../models/dashboard.model';

  export interface DashboardData {
    summary: DashboardSummary;
    cashFlow: CashFlowPoint[];
    revenueExpense: RevenueExpenseItem[];
    monthlyEvolution: MonthlyEvolutionPoint[];
  }

  @Injectable({ providedIn: 'root' })
  export class DashboardService {
    private readonly http = inject(HttpClient);
    private readonly base = '/api/dashboard';

    loadAll(period: DashboardPeriod): Observable<DashboardData> {
      const params = new HttpParams()
        .set('from', period.from)
        .set('to', period.to);

      return forkJoin({
        summary: this.http.get<DashboardSummary>(`${this.base}/summary`, { params }),
        cashFlow: this.http.get<CashFlowPoint[]>(`${this.base}/cash-flow`, { params }),
        revenueExpense: this.http.get<RevenueExpenseItem[]>(`${this.base}/revenue-expense`, { params }),
        monthlyEvolution: this.http.get<MonthlyEvolutionPoint[]>(`${this.base}/monthly-evolution`, { params }),
      });
    }
  }
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/models/
  git add gestao-empresaial-frontend/src/app/features/dashboard/services/
  git commit -m "feat(frontend): add dashboard models and DashboardService with forkJoin"
  ```

---

## Task 9: Frontend — DashboardFilterComponent

**Files:**
- Create: `...dashboard/components/dashboard-filter/dashboard-filter.component.ts`
- Create: `...dashboard/components/dashboard-filter/dashboard-filter.component.html`

- [ ] Criar `dashboard-filter.component.ts`:
  ```typescript
  import {
    ChangeDetectionStrategy, Component, computed,
    output, signal,
  } from '@angular/core';
  import { FormsModule } from '@angular/forms';
  import { DashboardPeriod } from '../../models/dashboard.model';

  type Preset = 'today' | 'week' | 'month' | 'quarter' | 'year' | 'custom';

  @Component({
    selector: 'app-dashboard-filter',
    templateUrl: './dashboard-filter.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule],
  })
  export class DashboardFilterComponent {
    readonly periodChange = output<DashboardPeriod>();

    protected readonly activePreset = signal<Preset>('month');
    protected readonly customFrom = signal('');
    protected readonly customTo = signal('');

    protected readonly presets: { key: Preset; label: string }[] = [
      { key: 'today', label: 'Hoje' },
      { key: 'week', label: 'Esta semana' },
      { key: 'month', label: 'Este mês' },
      { key: 'quarter', label: 'Este trimestre' },
      { key: 'year', label: 'Este ano' },
      { key: 'custom', label: 'Personalizado' },
    ];

    protected readonly isCustomValid = computed(() => {
      if (this.activePreset() !== 'custom') return true;
      return !!this.customFrom() && !!this.customTo() &&
             this.customFrom() <= this.customTo();
    });

    selectPreset(preset: Preset): void {
      this.activePreset.set(preset);
      if (preset !== 'custom') {
        this.periodChange.emit(this.computePeriod(preset));
      }
    }

    applyCustom(): void {
      if (!this.isCustomValid()) return;
      this.periodChange.emit({ from: this.customFrom(), to: this.customTo() });
    }

    private computePeriod(preset: Preset): DashboardPeriod {
      const today = new Date();
      const fmt = (d: Date) => d.toISOString().split('T')[0];
      const from = new Date(today);

      switch (preset) {
        case 'today':
          return { from: fmt(today), to: fmt(today) };
        case 'week':
          from.setDate(today.getDate() - today.getDay());
          return { from: fmt(from), to: fmt(today) };
        case 'month':
          from.setDate(1);
          return { from: fmt(from), to: fmt(today) };
        case 'quarter':
          from.setMonth(Math.floor(today.getMonth() / 3) * 3, 1);
          return { from: fmt(from), to: fmt(today) };
        case 'year':
          from.setMonth(0, 1);
          return { from: fmt(from), to: fmt(today) };
        default:
          return { from: fmt(from), to: fmt(today) };
      }
    }
  }
  ```

- [ ] Criar `dashboard-filter.component.html`:
  ```html
  <div class="flex items-center gap-2 flex-wrap">
    @for (preset of presets; track preset.key) {
      <button
        (click)="selectPreset(preset.key)"
        class="px-3 py-1.5 text-sm rounded-md border transition-colors"
        [class]="activePreset() === preset.key
          ? 'bg-blue-600 text-white border-blue-600'
          : 'bg-white text-gray-600 border-gray-300 hover:border-gray-400'"
      >
        {{ preset.label }}
      </button>
    }

    @if (activePreset() === 'custom') {
      <div class="flex items-center gap-2 ml-2">
        <input
          type="date"
          class="px-2 py-1.5 text-sm border border-gray-300 rounded-md"
          [ngModel]="customFrom()"
          (ngModelChange)="customFrom.set($event)"
        />
        <span class="text-gray-400 text-sm">até</span>
        <input
          type="date"
          class="px-2 py-1.5 text-sm border border-gray-300 rounded-md"
          [ngModel]="customTo()"
          (ngModelChange)="customTo.set($event)"
          [min]="customFrom()"
        />
        <button
          (click)="applyCustom()"
          [disabled]="!isCustomValid()"
          class="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Aplicar
        </button>
      </div>
    }
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/components/dashboard-filter/
  git commit -m "feat(frontend): add DashboardFilterComponent with presets and custom date range"
  ```

---

## Task 10: Frontend — SummaryCardsComponent

**Files:**
- Create: `...dashboard/components/summary-cards/summary-cards.component.ts`
- Create: `...dashboard/components/summary-cards/summary-cards.component.html`

- [ ] Criar `summary-cards.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, input } from '@angular/core';
  import { CurrencyPipe } from '@angular/common';
  import { DashboardSummary } from '../../models/dashboard.model';

  @Component({
    selector: 'app-summary-cards',
    templateUrl: './summary-cards.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CurrencyPipe],
  })
  export class SummaryCardsComponent {
    readonly summary = input<DashboardSummary | null>(null);
    readonly loading = input(false);

    protected readonly cards = [
      { key: 'totalPayable' as const, label: 'A Pagar', icon: 'pi-arrow-up', color: 'text-red-600', bg: 'bg-red-50' },
      { key: 'totalReceivable' as const, label: 'A Receber', icon: 'pi-arrow-down', color: 'text-emerald-600', bg: 'bg-emerald-50' },
      { key: 'totalRevenue' as const, label: 'Receitas', icon: 'pi-wallet', color: 'text-emerald-600', bg: 'bg-emerald-50' },
      { key: 'totalExpenses' as const, label: 'Despesas', icon: 'pi-credit-card', color: 'text-red-600', bg: 'bg-red-50' },
    ];
  }
  ```

- [ ] Criar `summary-cards.component.html`:
  ```html
  <div class="grid grid-cols-4 gap-4">
    @for (card of cards; track card.key) {
      <div class="border border-gray-200 rounded-lg p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="w-10 h-10 rounded-lg flex items-center justify-center {{ card.bg }}">
            <i class="pi {{ card.icon }} {{ card.color }}"></i>
          </div>
        </div>
        <p class="text-xs text-gray-500 mb-1">{{ card.label }}</p>
        @if (loading()) {
          <div class="h-6 w-24 bg-gray-200 rounded animate-pulse"></div>
        } @else {
          <p class="text-xl font-bold font-mono text-gray-900">
            {{ (summary()?.[card.key] ?? 0) | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </p>
        }
      </div>
    }
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/components/summary-cards/
  git commit -m "feat(frontend): add SummaryCardsComponent with loading skeleton"
  ```

---

## Task 11: Frontend — CashFlowChartComponent

**Files:**
- Create: `...dashboard/components/cash-flow-chart/cash-flow-chart.component.ts`
- Create: `...dashboard/components/cash-flow-chart/cash-flow-chart.component.html`

- [ ] Criar `cash-flow-chart.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
  import { ChartModule } from 'primeng/chart';
  import { CashFlowPoint } from '../../models/dashboard.model';

  @Component({
    selector: 'app-cash-flow-chart',
    templateUrl: './cash-flow-chart.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ChartModule],
  })
  export class CashFlowChartComponent {
    readonly data = input<CashFlowPoint[]>([]);
    readonly loading = input(false);

    protected readonly chartData = computed(() => {
      const points = this.data();
      return {
        labels: points.map((p) => p.month),
        datasets: [
          {
            label: 'Receitas',
            data: points.map((p) => p.revenue),
            borderColor: '#059669',
            backgroundColor: 'rgba(5,150,105,0.1)',
            fill: true,
            tension: 0.4,
          },
          {
            label: 'Despesas',
            data: points.map((p) => p.expense),
            borderColor: '#DC2626',
            backgroundColor: 'rgba(220,38,38,0.1)',
            fill: true,
            tension: 0.4,
          },
        ],
      };
    });

    protected readonly chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: 'top' } },
      scales: {
        y: { ticks: { callback: (v: number) => `R$ ${v.toLocaleString('pt-BR')}` } },
      },
    };
  }
  ```

- [ ] Criar `cash-flow-chart.component.html`:
  ```html
  <div class="border border-gray-200 rounded-lg p-6">
    <h2 class="text-base font-semibold text-gray-900 mb-4">Fluxo de Caixa</h2>
    @if (loading()) {
      <div class="h-64 bg-gray-100 rounded animate-pulse"></div>
    } @else if (data().length === 0) {
      <div class="h-64 flex items-center justify-center text-gray-400 text-sm">
        Nenhum lançamento neste período
      </div>
    } @else {
      <div style="height: 256px">
        <p-chart type="line" [data]="chartData()" [options]="chartOptions" height="256" />
      </div>
    }
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/components/cash-flow-chart/
  git commit -m "feat(frontend): add CashFlowChartComponent using PrimeNG Charts"
  ```

---

## Task 12: Frontend — RevenueExpenseChartComponent

**Files:**
- Create: `...dashboard/components/revenue-expense-chart/revenue-expense-chart.component.ts`
- Create: `...dashboard/components/revenue-expense-chart/revenue-expense-chart.component.html`

- [ ] Criar `revenue-expense-chart.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
  import { ChartModule } from 'primeng/chart';
  import { RevenueExpenseItem } from '../../models/dashboard.model';

  @Component({
    selector: 'app-revenue-expense-chart',
    templateUrl: './revenue-expense-chart.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ChartModule],
  })
  export class RevenueExpenseChartComponent {
    readonly data = input<RevenueExpenseItem[]>([]);
    readonly loading = input(false);

    protected readonly chartData = computed(() => {
      const items = this.data();
      return {
        labels: items.map((i) => i.categoryName),
        datasets: [
          {
            label: 'Total',
            data: items.map((i) => i.total),
            backgroundColor: items.map((i) =>
              i.type === 'REVENUE' ? 'rgba(5,150,105,0.7)' :
              i.type === 'EXPENSE' ? 'rgba(220,38,38,0.7)' : 'rgba(107,114,128,0.7)'
            ),
          },
        ],
      };
    });

    protected readonly chartOptions = {
      indexAxis: 'y' as const,
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { callback: (v: number) => `R$ ${v.toLocaleString('pt-BR')}` } },
      },
    };
  }
  ```

- [ ] Criar `revenue-expense-chart.component.html`:
  ```html
  <div class="border border-gray-200 rounded-lg p-6">
    <h2 class="text-base font-semibold text-gray-900 mb-4">Receita x Despesa por Categoria</h2>
    @if (loading()) {
      <div class="h-64 bg-gray-100 rounded animate-pulse"></div>
    } @else if (data().length === 0) {
      <div class="h-64 flex items-center justify-center text-gray-400 text-sm">
        Nenhum lançamento neste período
      </div>
    } @else {
      <div style="height: 256px">
        <p-chart type="bar" [data]="chartData()" [options]="chartOptions" height="256" />
      </div>
    }
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/components/revenue-expense-chart/
  git commit -m "feat(frontend): add RevenueExpenseChartComponent with horizontal bar chart"
  ```

---

## Task 13: Frontend — MonthlyEvolutionChartComponent

**Files:**
- Create: `...dashboard/components/monthly-evolution-chart/monthly-evolution-chart.component.ts`
- Create: `...dashboard/components/monthly-evolution-chart/monthly-evolution-chart.component.html`

- [ ] Criar `monthly-evolution-chart.component.ts`:
  ```typescript
  import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
  import { ChartModule } from 'primeng/chart';
  import { MonthlyEvolutionPoint } from '../../models/dashboard.model';

  @Component({
    selector: 'app-monthly-evolution-chart',
    templateUrl: './monthly-evolution-chart.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ChartModule],
  })
  export class MonthlyEvolutionChartComponent {
    readonly data = input<MonthlyEvolutionPoint[]>([]);
    readonly loading = input(false);

    protected readonly chartData = computed(() => {
      const points = this.data();
      return {
        labels: points.map((p) => p.month),
        datasets: [
          {
            type: 'bar',
            label: 'Receitas',
            data: points.map((p) => p.revenue),
            backgroundColor: 'rgba(5,150,105,0.7)',
          },
          {
            type: 'bar',
            label: 'Despesas',
            data: points.map((p) => p.expense),
            backgroundColor: 'rgba(220,38,38,0.7)',
          },
          {
            type: 'line',
            label: 'Saldo Acumulado',
            data: points.map((p) => p.balance),
            borderColor: '#2563EB',
            backgroundColor: 'transparent',
            tension: 0.4,
            pointRadius: 4,
          },
        ],
      };
    });

    protected readonly chartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: 'top' } },
      scales: {
        y: { ticks: { callback: (v: number) => `R$ ${v.toLocaleString('pt-BR')}` } },
      },
    };
  }
  ```

- [ ] Criar `monthly-evolution-chart.component.html`:
  ```html
  <div class="border border-gray-200 rounded-lg p-6">
    <h2 class="text-base font-semibold text-gray-900 mb-4">Evolução Mensal</h2>
    @if (loading()) {
      <div class="h-64 bg-gray-100 rounded animate-pulse"></div>
    } @else if (data().length === 0) {
      <div class="h-64 flex items-center justify-center text-gray-400 text-sm">
        Nenhum lançamento neste período
      </div>
    } @else {
      <div style="height: 256px">
        <p-chart type="bar" [data]="chartData()" [options]="chartOptions" height="256" />
      </div>
    }
  </div>
  ```

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/components/monthly-evolution-chart/
  git commit -m "feat(frontend): add MonthlyEvolutionChartComponent with mixed bar+line chart"
  ```

---

## Task 14: Frontend — DashboardComponent (orquestrador)

**Files:**
- Modify: `gestao-empresaial-frontend/src/app/features/dashboard/pages/dashboard/dashboard.component.ts`
- Modify: `gestao-empresaial-frontend/src/app/features/dashboard/pages/dashboard/dashboard.component.html`

Substituir o componente stub com dados hardcoded pelo orquestrador real.

- [ ] Substituir `dashboard.component.ts`:
  ```typescript
  import {
    ChangeDetectionStrategy, Component, effect, inject, signal,
  } from '@angular/core';
  import { DashboardService, DashboardData } from '../../services/dashboard.service';
  import { DashboardPeriod } from '../../models/dashboard.model';
  import { DashboardFilterComponent } from '../../components/dashboard-filter/dashboard-filter.component';
  import { SummaryCardsComponent } from '../../components/summary-cards/summary-cards.component';
  import { CashFlowChartComponent } from '../../components/cash-flow-chart/cash-flow-chart.component';
  import { RevenueExpenseChartComponent } from '../../components/revenue-expense-chart/revenue-expense-chart.component';
  import { MonthlyEvolutionChartComponent } from '../../components/monthly-evolution-chart/monthly-evolution-chart.component';

  @Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
      DashboardFilterComponent,
      SummaryCardsComponent,
      CashFlowChartComponent,
      RevenueExpenseChartComponent,
      MonthlyEvolutionChartComponent,
    ],
  })
  export class DashboardComponent {
    private readonly dashboardService = inject(DashboardService);

    protected readonly loading = signal(true);
    protected readonly dashboardData = signal<DashboardData | null>(null);

    // Default: mês corrente
    protected readonly period = signal<DashboardPeriod>(this.currentMonthPeriod());

    constructor() {
      effect(() => {
        const p = this.period();
        this.loading.set(true);
        this.dashboardService.loadAll(p).subscribe({
          next: (data) => {
            this.dashboardData.set(data);
            this.loading.set(false);
          },
          error: () => this.loading.set(false),
        });
      });
    }

    protected onPeriodChange(period: DashboardPeriod): void {
      this.period.set(period);
    }

    private currentMonthPeriod(): DashboardPeriod {
      const today = new Date();
      const from = new Date(today.getFullYear(), today.getMonth(), 1);
      const fmt = (d: Date) => d.toISOString().split('T')[0];
      return { from: fmt(from), to: fmt(today) };
    }
  }
  ```

- [ ] Substituir `dashboard.component.html`:
  ```html
  <div class="mb-6 flex items-start justify-between flex-wrap gap-4">
    <div>
      <h1 class="text-2xl font-bold text-gray-900">Visão Geral</h1>
      <p class="mt-1 text-sm text-gray-500">Resumo financeiro da empresa.</p>
    </div>
    <app-dashboard-filter (periodChange)="onPeriodChange($event)" />
  </div>

  <!-- KPI Cards -->
  <div class="mb-6">
    <app-summary-cards
      [summary]="dashboardData()?.summary ?? null"
      [loading]="loading()"
    />
  </div>

  <!-- Fluxo de Caixa (linha inteira) -->
  <div class="mb-6">
    <app-cash-flow-chart
      [data]="dashboardData()?.cashFlow ?? []"
      [loading]="loading()"
    />
  </div>

  <!-- Receita x Despesa + Evolução Mensal (lado a lado) -->
  <div class="grid grid-cols-2 gap-6">
    <app-revenue-expense-chart
      [data]="dashboardData()?.revenueExpense ?? []"
      [loading]="loading()"
    />
    <app-monthly-evolution-chart
      [data]="dashboardData()?.monthlyEvolution ?? []"
      [loading]="loading()"
    />
  </div>
  ```

- [ ] Rodar o frontend e verificar que o dashboard carrega sem erros:
  ```bash
  cd gestao-empresaial-frontend && npm start
  ```
  Navegar para `http://localhost:4200` e verificar que o dashboard aparece (com estado vazio se não houver dados ou com gráficos reais).

- [ ] Commit:
  ```bash
  git add gestao-empresaial-frontend/src/app/features/dashboard/pages/dashboard/
  git commit -m "feat(frontend): implement DashboardComponent as real-data orchestrator"
  ```

---

## Task 15: Commit final, push e PR

- [ ] Rodar todos os testes backend:
  ```bash
  cd gestao-empresarial-backend && ./mvnw test
  ```
  Esperado: todos verdes.

- [ ] Verificar que o frontend compila sem erros:
  ```bash
  cd gestao-empresaial-frontend && npm run build -- --configuration development 2>&1 | tail -10
  ```

- [ ] Push da branch:
  ```bash
  git push origin feature/phase6-dashboard
  ```

- [ ] Criar Pull Request para `master` descrevendo:
  - 4 endpoints `/api/dashboard/**` com queries JPQL de agregação
  - Filtro global de período (presets + customizado) propagado para todos os widgets
  - 4 KPI cards com skeleton de loading
  - 3 gráficos: Fluxo de Caixa (linha), Receita x Despesa (barra horizontal), Evolução Mensal (barra + linha)
  - MVP completo ✅

---

## Notas de Implementação

### CompanyContextHolder
O `DashboardController` usa `CompanyContextHolder.get()` — padrão já estabelecido nos outros controllers.

### Formato do mês nos gráficos
A query retorna `'YYYY-MM'` (ex: `"2026-01"`). O frontend exibe isso diretamente no eixo X. Para formatar como `"Jan/26"`, adicionar um pipe de transformação ou mapear no service antes de passar aos charts.

### PrimeNG Chart + Chart.js
O `<p-chart>` é do pacote `primeng/chart`. Requer `chart.js` instalado (Task 1). Importar `ChartModule` de `'primeng/chart'` em cada componente que o usa.

### JPQL `FUNCTION('TO_CHAR', ...)`
A query de cash-flow usa `FUNCTION('TO_CHAR', a.dueDate, 'YYYY-MM')` — syntax JPQL para invocar funções nativas do banco. Funciona com PostgreSQL. Se houver problemas de parsing, considerar usar `@Query(nativeQuery = true)` com SQL puro.
