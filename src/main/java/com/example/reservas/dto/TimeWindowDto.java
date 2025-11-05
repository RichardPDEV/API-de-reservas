package com.example.reservas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record TimeWindowDto(
    @NotNull(message = "La fecha de inicio es requerida")
    OffsetDateTime start,
    
    @NotNull(message = "La fecha de fin es requerida")
    OffsetDateTime end
) {
    /**
     * Valida que la fecha de inicio sea anterior a la fecha de fin.
     * @throws IllegalArgumentException si start >= end
     */
    public TimeWindowDto {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }
}

