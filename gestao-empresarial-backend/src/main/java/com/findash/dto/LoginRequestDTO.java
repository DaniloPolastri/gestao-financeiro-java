package com.findash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido") String email,
    @NotBlank(message = "Senha e obrigatoria") String password
) {}
