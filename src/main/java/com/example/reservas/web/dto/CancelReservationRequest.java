package com.example.reservas.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelReservationRequest(
    @NotBlank String reason
) {}
