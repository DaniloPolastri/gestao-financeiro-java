package com.findash.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyMemberResponseDTO(
    UUID userId,
    String name,
    String email,
    String role,
    String status,
    Instant joinedAt
) {}
