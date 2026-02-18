package com.findash.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountResponseDTO(
    UUID id,
    String type,
    String description,
    BigDecimal amount,
    LocalDate dueDate,
    LocalDate paymentDate,
    String status,
    AccountCategoryDTO category,
    AccountSupplierDTO supplier,
    AccountClientDTO client,
    UUID recurrenceId,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {
    public record AccountCategoryDTO(UUID id, String name) {}
    public record AccountSupplierDTO(UUID id, String name) {}
    public record AccountClientDTO(UUID id, String name) {}
}
