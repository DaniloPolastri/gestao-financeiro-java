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
import java.util.ArrayList;
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

    @Test
    void getSummary_returnsAggregatedValues() {
        when(accountRepository.sumByTypeAndStatuses(
            eq(companyId), eq(AccountType.PAYABLE),
            eq(List.of(AccountStatus.PENDING, AccountStatus.OVERDUE)), eq(from), eq(to)))
            .thenReturn(new BigDecimal("5000.00"));
        when(accountRepository.sumByTypeAndStatuses(
            eq(companyId), eq(AccountType.RECEIVABLE),
            eq(List.of(AccountStatus.PENDING)), eq(from), eq(to)))
            .thenReturn(new BigDecimal("3000.00"));
        when(accountRepository.sumByTypeAndStatuses(
            eq(companyId), eq(AccountType.RECEIVABLE),
            eq(List.of(AccountStatus.RECEIVED)), eq(from), eq(to)))
            .thenReturn(new BigDecimal("8000.00"));
        when(accountRepository.sumByTypeAndStatuses(
            eq(companyId), eq(AccountType.PAYABLE),
            eq(List.of(AccountStatus.PAID)), eq(from), eq(to)))
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
        assertEquals(BigDecimal.ZERO, result.totalReceivable());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
        assertEquals(BigDecimal.ZERO, result.totalExpenses());
    }

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
        CashFlowPointDTO jan = result.stream()
            .filter(p -> "2026-01".equals(p.month())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("3000"), jan.revenue());
        assertEquals(new BigDecimal("1500"), jan.expense());

        CashFlowPointDTO feb = result.stream()
            .filter(p -> "2026-02".equals(p.month())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("4000"), feb.revenue());
        assertEquals(BigDecimal.ZERO, feb.expense());
    }

    @Test
    void getCashFlow_noData_returnsEmptyList() {
        when(accountRepository.findMonthlyTotalsByType(any(), anyList(), any(), any()))
            .thenReturn(List.of());

        List<CashFlowPointDTO> result = service.getCashFlow(companyId, from, to);

        assertTrue(result.isEmpty());
    }

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
        assertEquals(new BigDecimal("2000"), result.get(0).balance());
        assertEquals(new BigDecimal("3500"), result.get(1).balance());
    }

    @Test
    void getRevenueExpense_returnsAllIfLessOrEqual10() {
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
        List<Object[]> mockRows = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            mockRows.add(new Object[]{"Cat " + i, "Grupo", AccountType.PAYABLE, new BigDecimal("100")});
        }
        when(accountRepository.findRevenueExpenseByCategory(any(), anyList(), any(), any()))
            .thenReturn(mockRows);

        List<RevenueExpenseItemDTO> result = service.getRevenueExpense(companyId, from, to);

        assertEquals(11, result.size());
        assertEquals("Outros", result.get(10).categoryName());
        assertEquals(new BigDecimal("200"), result.get(10).total());
    }

    @Test
    void getMonthlyEvolution_noData_returnsEmptyList() {
        when(accountRepository.findMonthlyTotalsByType(any(), anyList(), any(), any()))
            .thenReturn(List.of());

        List<MonthlyEvolutionPointDTO> result = service.getMonthlyEvolution(companyId, from, to);

        assertTrue(result.isEmpty());
    }
}
