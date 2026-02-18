package com.findash.dto.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponseDTO(
    UUID id,
    String name,
    String document,
    String email,
    String phone,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
