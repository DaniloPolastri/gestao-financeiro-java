package com.findash.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RecurrenceRequestDTO(
    @NotBlank(message = "Frequencia e obrigatoria")
    String frequency,
    LocalDate endDate,
    Integer maxOccurrences
) {}
