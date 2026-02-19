package com.findash.dto.dashboard;

import java.math.BigDecimal;

public record MonthlyEvolutionPointDTO(
    String month,
    BigDecimal revenue,
    BigDecimal expense,
    BigDecimal balance
) {}
