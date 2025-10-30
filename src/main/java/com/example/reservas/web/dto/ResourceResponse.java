package com.example.reservas.web.dto;

public record ResourceResponse(
    Long id, Long businessId, String name, Integer capacity
) {}
