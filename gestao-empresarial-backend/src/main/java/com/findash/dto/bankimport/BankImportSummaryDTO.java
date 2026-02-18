package com.findash.dto.bankimport;

import java.time.LocalDateTime;
import java.util.UUID;

public record BankImportSummaryDTO(
    UUID id,
    String fileName,
    String fileType,
    String status,
    int totalRecords,
    LocalDateTime createdAt
) {}
