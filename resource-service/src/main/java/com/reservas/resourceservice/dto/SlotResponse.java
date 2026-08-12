package com.reservas.resourceservice.dto;

import com.reservas.resourceservice.model.AvailabilitySlot;
import com.reservas.resourceservice.model.SlotStatus;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID resourceId,
        Instant startTime,
        Instant endTime,
        SlotStatus status
) {
    public static SlotResponse from(AvailabilitySlot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getResourceId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
