package com.reservas.reservationservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "resourceId es obligatorio")
        UUID resourceId,

        @NotNull(message = "slotId es obligatorio")
        UUID slotId
) {
}
