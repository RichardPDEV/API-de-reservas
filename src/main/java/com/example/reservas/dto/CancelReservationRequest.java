package com.example.reservas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
    @NotBlank
    @Size(max = 500, message = "La razón de cancelación no puede exceder 500 caracteres")
    String reason
) {}

