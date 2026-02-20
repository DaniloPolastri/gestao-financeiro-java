package com.findash.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BatchPayRequestDTO(
    @NotEmpty(message = "Lista de contas e obrigatoria")
    List<UUID> accountIds,
    @NotNull(message = "Data de pagamento e obrigatoria")
    LocalDate paymentDate
) {}
