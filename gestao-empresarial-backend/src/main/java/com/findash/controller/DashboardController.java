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

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : today.withDayOfMonth(1);
        LocalDate resolvedTo = to != null ? to : today;
        return new LocalDate[]{resolvedFrom, resolvedTo};
    }
}
