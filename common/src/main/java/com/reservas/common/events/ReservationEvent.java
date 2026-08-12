package com.reservas.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato compartido entre reservation-service (productor) y notification-service
 * (consumidor) para el topic Kafka "reservation-events".
 */
public record ReservationEvent(
        UUID reservationId,
        UUID userId,
        UUID resourceId,
        UUID slotId,
        Instant startTime,
        Instant endTime,
        ReservationEventType type,
        Instant occurredAt
) {
}
