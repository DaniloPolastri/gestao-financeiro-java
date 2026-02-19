package com.findash.dto.dashboard;

import java.math.BigDecimal;

public record RevenueExpenseItemDTO(
    String categoryName,
    String groupName,
    BigDecimal total,
    String type
) {}
