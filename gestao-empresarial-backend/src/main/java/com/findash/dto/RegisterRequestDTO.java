package com.findash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
    @NotBlank(message = "Nome e obrigatorio") String name,
    @NotBlank(message = "Email e obrigatorio") @Email(message = "Email invalido") String email,
    @NotBlank(message = "Senha e obrigatoria") @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres") String password
) {}
