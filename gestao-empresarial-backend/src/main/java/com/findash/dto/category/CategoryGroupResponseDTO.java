package com.findash.dto.category;

import java.util.List;
import java.util.UUID;

public record CategoryGroupResponseDTO(
    UUID id,
    String name,
    String type,
    int displayOrder,
    List<CategoryResponseDTO> categories
) {}
