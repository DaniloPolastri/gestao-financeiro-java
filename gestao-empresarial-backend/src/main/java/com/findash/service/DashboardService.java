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
