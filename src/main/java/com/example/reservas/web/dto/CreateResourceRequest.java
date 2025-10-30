package com.example.reservas.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateResourceRequest(
    @NotNull Long businessId,
    @NotBlank String name,
    @Min(1) int capacity
) {}
