package com.reservas.reservationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "resource-service")
public interface ResourceClient {

    @GetMapping("/resources/{resourceId}/slots/{slotId}")
    SlotDto getSlot(@PathVariable UUID resourceId, @PathVariable UUID slotId);

    @PatchMapping("/resources/{resourceId}/slots/{slotId}/status")
    SlotDto updateSlotStatus(
            @PathVariable UUID resourceId,
            @PathVariable UUID slotId,
            @RequestBody SlotStatusUpdateRequest request
    );
}
