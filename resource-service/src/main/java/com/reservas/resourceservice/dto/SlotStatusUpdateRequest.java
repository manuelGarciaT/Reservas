package com.reservas.resourceservice.dto;

import com.reservas.resourceservice.model.SlotStatus;
import jakarta.validation.constraints.NotNull;

public record SlotStatusUpdateRequest(
        @NotNull(message = "status es obligatorio")
        SlotStatus status
) {
}
