package com.findash.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAccountRequestDTO(
    @NotBlank(message = "Tipo e obrigatorio (PAYABLE ou RECEIVABLE)")
    String type,

    @NotBlank(message = "Descricao e obrigatoria")
    @Size(min = 2, max = 255, message = "Descricao deve ter entre 2 e 255 caracteres")
    String description,

    @NotNull(message = "Valor e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    BigDecimal amount,

    @NotNull(message = "Data de vencimento e obrigatoria")
    LocalDate dueDate,

    @NotNull(message = "Categoria e obrigatoria")
    UUID categoryId,

    UUID supplierId,
    UUID clientId,
    String notes,
    RecurrenceRequestDTO recurrence
) {}
