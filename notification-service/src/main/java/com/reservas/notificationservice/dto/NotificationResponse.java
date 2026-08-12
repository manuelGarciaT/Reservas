package com.reservas.notificationservice.dto;

import com.reservas.common.events.ReservationEventType;
import com.reservas.notificationservice.model.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID reservationId,
        UUID resourceId,
        UUID slotId,
        ReservationEventType type,
        String message,
        Instant startTime,
        Instant endTime,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getReservationId(),
                notification.getResourceId(),
                notification.getSlotId(),
                notification.getType(),
                notification.getMessage(),
                notification.getStartTime(),
                notification.getEndTime(),
                notification.getCreatedAt()
        );
    }
}
