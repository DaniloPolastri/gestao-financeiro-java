package com.findash.dto.supplier;

import java.time.Instant;
import java.util.UUID;

public record SupplierResponseDTO(
    UUID id,
    String name,
    String document,
    String email,
    String phone,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
