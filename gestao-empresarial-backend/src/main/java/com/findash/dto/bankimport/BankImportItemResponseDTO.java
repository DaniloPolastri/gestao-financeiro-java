package com.findash.dto.bankimport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankImportItemResponseDTO(
    UUID id,
    LocalDate date,
    String description,
    BigDecimal amount,
    String type,
    String accountType,
    UUID supplierId,
    String supplierName,
    UUID categoryId,
    String categoryName,
    boolean possibleDuplicate
) {}
