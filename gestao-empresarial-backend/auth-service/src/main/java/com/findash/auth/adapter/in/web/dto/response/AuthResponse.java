package com.findash.auth.adapter.in.web.dto.response;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserResponse user
) {
    public record UserResponse(UUID id, String name, String email) {}
}
