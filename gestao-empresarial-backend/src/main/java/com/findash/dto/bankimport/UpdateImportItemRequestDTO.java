package com.findash.dto.bankimport;

import java.util.UUID;

public record UpdateImportItemRequestDTO(
    UUID supplierId,
    UUID categoryId,
    String accountType
) {}
