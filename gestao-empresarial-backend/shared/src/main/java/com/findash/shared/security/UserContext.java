package com.findash.shared.security;

import java.util.List;
import java.util.UUID;

public record UserContext(
    UUID userId,
    String email,
    List<String> roles
) {}
