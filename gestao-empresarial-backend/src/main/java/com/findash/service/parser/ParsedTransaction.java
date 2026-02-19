package com.findash.service.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record ParsedTransaction(
    LocalDate date,
    String description,
    BigDecimal amount,
    String type,       // "CREDIT" ou "DEBIT"
    Map<String, Object> rawData
) {}
