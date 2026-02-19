package com.findash.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
    BigDecimal totalPayable,
    BigDecimal totalReceivable,
    BigDecimal totalRevenue,
    BigDecimal totalExpenses
) {}
