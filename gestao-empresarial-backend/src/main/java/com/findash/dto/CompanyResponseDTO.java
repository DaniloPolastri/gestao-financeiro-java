package com.findash.dto;

import java.util.UUID;

public record CompanyResponseDTO(
    UUID id,
    String name,
    String cnpj,
    String segment,
    UUID ownerId,
    String ownerName,
    String role,
    boolean active
) {}
