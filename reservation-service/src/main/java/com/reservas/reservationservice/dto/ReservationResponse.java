package com.reservas.reservationservice.dto;

import com.reservas.reservationservice.model.Reservation;
import com.reservas.reservationservice.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID userId,
        UUID resourceId,
        UUID slotId,
        Instant startTime,
        Instant endTime,
        ReservationStatus status,
        Instant createdAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getResourceId(),
                reservation.getSlotId(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
    }
}
