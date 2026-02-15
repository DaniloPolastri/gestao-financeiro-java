package com.findash.dto;

public record AuthResponseDTO(String accessToken, String refreshToken, UserResponseDTO user) {}
