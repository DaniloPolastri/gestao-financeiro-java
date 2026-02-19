package com.findash.dto.bankimport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BankImportResponseDTO(
    UUID id,
    String fileName,
    String fileType,
    String status,
    int totalRecords,
    LocalDateTime createdAt,
    List<BankImportItemResponseDTO> items
) {}
