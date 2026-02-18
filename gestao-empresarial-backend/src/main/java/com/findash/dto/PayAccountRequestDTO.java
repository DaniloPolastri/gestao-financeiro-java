package com.findash.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PayAccountRequestDTO(
    @NotNull(message = "Data de pagamento e obrigatoria")
    LocalDate paymentDate,
    BigDecimal amountPaid
) {}
