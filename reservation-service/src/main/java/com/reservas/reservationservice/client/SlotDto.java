package com.reservas.reservationservice.client;

import java.time.Instant;
import java.util.UUID;

public record SlotDto(
        UUID id,
        UUID resourceId,
        Instant startTime,
        Instant endTime,
        SlotStatus status
) {
}
