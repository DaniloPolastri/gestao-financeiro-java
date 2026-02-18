package com.findash.dto.category;

import java.util.UUID;

public record CategoryResponseDTO(
    UUID id,
    UUID groupId,
    String name,
    boolean active
) {}
