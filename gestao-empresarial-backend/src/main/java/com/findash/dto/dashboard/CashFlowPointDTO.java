package com.findash.dto.dashboard;

import java.math.BigDecimal;

public record CashFlowPointDTO(
    String month,
    BigDecimal revenue,
    BigDecimal expense
) {}
