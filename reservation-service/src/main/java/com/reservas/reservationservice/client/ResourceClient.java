package com.reservas.reservationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Con url vacia (default), Feign resuelve "resource-service" via Eureka + load balancer.
 * Si resource-service.url esta seteada (ej. deploy sin discovery-server), se usa esa
 * URL fija directamente, sin pasar por Eureka.
 */
@FeignClient(name = "resource-service", url = "${resource-service.url:}")
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
