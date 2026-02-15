package com.findash.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "Refresh token e obrigatorio")
    String refreshToken
) {}
