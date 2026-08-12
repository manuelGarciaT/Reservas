package com.reservas.resourceservice.controller;

import com.reservas.resourceservice.dto.SlotRequest;
import com.reservas.resourceservice.dto.SlotResponse;
import com.reservas.resourceservice.dto.SlotStatusUpdateRequest;
import com.reservas.resourceservice.service.AvailabilitySlotService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/resources/{resourceId}/slots")
public class SlotController {

    private final AvailabilitySlotService slotService;

    public SlotController(AvailabilitySlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SlotResponse> create(
            @PathVariable UUID resourceId,
            @Valid @RequestBody SlotRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.create(resourceId, request));
    }

    @GetMapping("/{slotId}")
    public SlotResponse getById(@PathVariable UUID resourceId, @PathVariable UUID slotId) {
        return slotService.getById(resourceId, slotId);
    }

    @GetMapping
    public Page<SlotResponse> list(
            @PathVariable UUID resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return slotService.list(resourceId, date, pageable);
    }

    @PatchMapping("/{slotId}/status")
    public SlotResponse updateStatus(
            @PathVariable UUID resourceId,
            @PathVariable UUID slotId,
            @Valid @RequestBody SlotStatusUpdateRequest request
    ) {
        return slotService.updateStatus(resourceId, slotId, request.status());
    }
}
