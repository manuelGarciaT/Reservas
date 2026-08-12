package com.reservas.resourceservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SlotRequest(
        @NotNull(message = "startTime es obligatorio")
        Instant startTime,

        @NotNull(message = "endTime es obligatorio")
        Instant endTime
) {
}
