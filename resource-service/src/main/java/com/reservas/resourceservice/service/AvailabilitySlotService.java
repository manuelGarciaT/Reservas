package com.reservas.resourceservice.service;

import com.reservas.resourceservice.dto.SlotRequest;
import com.reservas.resourceservice.dto.SlotResponse;
import com.reservas.resourceservice.exception.InvalidSlotException;
import com.reservas.resourceservice.exception.ResourceNotFoundException;
import com.reservas.resourceservice.exception.SlotNotFoundException;
import com.reservas.resourceservice.model.AvailabilitySlot;
import com.reservas.resourceservice.model.SlotStatus;
import com.reservas.resourceservice.repository.AvailabilitySlotRepository;
import com.reservas.resourceservice.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class AvailabilitySlotService {

    private final AvailabilitySlotRepository slotRepository;
    private final ResourceRepository resourceRepository;

    public AvailabilitySlotService(AvailabilitySlotRepository slotRepository, ResourceRepository resourceRepository) {
        this.slotRepository = slotRepository;
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public SlotResponse create(UUID resourceId, SlotRequest request) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Recurso no encontrado: " + resourceId);
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new InvalidSlotException("endTime debe ser posterior a startTime");
        }

        boolean overlaps = !slotRepository
                .findByResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        resourceId, request.endTime(), request.startTime())
                .isEmpty();
        if (overlaps) {
            throw new InvalidSlotException("El turno se superpone con uno existente para este recurso");
        }

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .resourceId(resourceId)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(SlotStatus.AVAILABLE)
                .build();

        return SlotResponse.from(slotRepository.save(slot));
    }

    public SlotResponse getById(UUID resourceId, UUID slotId) {
        return SlotResponse.from(findOrThrow(resourceId, slotId));
    }

    public Page<SlotResponse> list(UUID resourceId, LocalDate date, Pageable pageable) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Recurso no encontrado: " + resourceId);
        }

        if (date != null) {
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return slotRepository.findByResourceIdAndStartTimeBetween(resourceId, from, to, pageable)
                    .map(SlotResponse::from);
        }

        return slotRepository.findByResourceId(resourceId, pageable).map(SlotResponse::from);
    }

    @Transactional
    public SlotResponse updateStatus(UUID resourceId, UUID slotId, SlotStatus newStatus) {
        AvailabilitySlot slot = findOrThrow(resourceId, slotId);

        if (newStatus == SlotStatus.BLOCKED && slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new InvalidSlotException("El turno ya no esta disponible");
        }

        slot.setStatus(newStatus);
        return SlotResponse.from(slotRepository.save(slot));
    }

    private AvailabilitySlot findOrThrow(UUID resourceId, UUID slotId) {
        return slotRepository.findByIdAndResourceId(slotId, resourceId)
                .orElseThrow(() -> new SlotNotFoundException("Turno no encontrado: " + slotId));
    }
}
