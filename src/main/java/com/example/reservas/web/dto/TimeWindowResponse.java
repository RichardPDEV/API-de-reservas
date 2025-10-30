package com.example.reservas.web.dto;

import java.time.OffsetDateTime;

public record TimeWindowResponse(
    OffsetDateTime start, OffsetDateTime end
) {}
