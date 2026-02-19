package com.findash.dto.bankimport;

import java.util.List;
import java.util.UUID;

public record BatchUpdateImportItemsRequestDTO(
    List<UUID> itemIds,
    UUID supplierId,
    UUID categoryId,
    String accountType
) {}
