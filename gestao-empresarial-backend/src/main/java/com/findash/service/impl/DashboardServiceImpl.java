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
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final List<AccountStatus> PENDING_STATUSES =
        List.of(AccountStatus.PENDING, AccountStatus.OVERDUE);
    private static final List<AccountStatus> PAID_STATUSES =
        List.of(AccountStatus.PAID, AccountStatus.RECEIVED, AccountStatus.PARTIAL);

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

        Map<String, BigDecimal[]> byMonth = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String month = (String) row[0];
            AccountType type = (AccountType) row[1];
            BigDecimal total = (BigDecimal) row[2];
            byMonth.putIfAbsent(month, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (type == AccountType.RECEIVABLE) {
                byMonth.get(month)[0] = total;
            } else {
                byMonth.get(month)[1] = total;
            }
        }

        return byMonth.entrySet().stream()
            .map(e -> new CashFlowPointDTO(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();
    }

    @Override
    public List<RevenueExpenseItemDTO> getRevenueExpense(UUID companyId, LocalDate from, LocalDate to) {
        List<Object[]> rows = accountRepository.findRevenueExpenseByCategory(
            companyId, PAID_STATUSES, from, to);

        List<RevenueExpenseItemDTO> result = rows.stream()
            .map(row -> new RevenueExpenseItemDTO(
                (String) row[0],
                (String) row[1],
                (BigDecimal) row[3],
                (AccountType) row[2] == AccountType.RECEIVABLE ? "REVENUE" : "EXPENSE"
            ))
            .toList();

        if (result.size() <= 10) return result;

        List<RevenueExpenseItemDTO> top10 = result.subList(0, 10);
        BigDecimal othersTotal = result.subList(10, result.size()).stream()
            .map(RevenueExpenseItemDTO::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RevenueExpenseItemDTO> finalResult = new ArrayList<>(top10);
        finalResult.add(new RevenueExpenseItemDTO("Outros", "\u2014", othersTotal, "OTHER"));
        return finalResult;
    }

    @Override
    public List<MonthlyEvolutionPointDTO> getMonthlyEvolution(UUID companyId, LocalDate from, LocalDate to) {
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
}
