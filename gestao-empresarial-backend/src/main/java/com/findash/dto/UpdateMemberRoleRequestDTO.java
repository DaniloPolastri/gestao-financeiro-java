package com.findash.dto;

import com.findash.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequestDTO(
    @NotNull(message = "Role e obrigatoria") Role role
) {}
