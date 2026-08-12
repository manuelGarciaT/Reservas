package com.reservas.reservationservice.service;

import com.reservas.common.events.ReservationEvent;
import com.reservas.common.events.ReservationEventType;
import com.reservas.reservationservice.client.ResourceClient;
import com.reservas.reservationservice.client.SlotDto;
import com.reservas.reservationservice.client.SlotStatus;
import com.reservas.reservationservice.client.SlotStatusUpdateRequest;
import com.reservas.reservationservice.dto.ReservationRequest;
import com.reservas.reservationservice.dto.ReservationResponse;
import com.reservas.reservationservice.event.ReservationEventPublisher;
import com.reservas.reservationservice.exception.InvalidReservationStateException;
import com.reservas.reservationservice.exception.ReservationNotFoundException;
import com.reservas.reservationservice.exception.SlotNotAvailableException;
import com.reservas.reservationservice.model.Reservation;
import com.reservas.reservationservice.model.ReservationStatus;
import com.reservas.reservationservice.repository.ReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceClient resourceClient;
    private final ReservationEventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            ResourceClient resourceClient,
            ReservationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.resourceClient = resourceClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ReservationResponse create(UUID userId, ReservationRequest request) {
        SlotDto slot = resourceClient.getSlot(request.resourceId(), request.slotId());

        if (slot.status() != SlotStatus.AVAILABLE) {
            throw new SlotNotAvailableException("El turno solicitado ya no esta disponible");
        }

        resourceClient.updateSlotStatus(
                request.resourceId(), request.slotId(), new SlotStatusUpdateRequest(SlotStatus.BLOCKED));

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .resourceId(request.resourceId())
                .slotId(request.slotId())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .status(ReservationStatus.CONFIRMED)
                .build();

        reservation = reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationEvent(
                reservation.getId(), reservation.getUserId(), reservation.getResourceId(), reservation.getSlotId(),
                reservation.getStartTime(), reservation.getEndTime(), ReservationEventType.CREATED, Instant.now()));

        return ReservationResponse.from(reservation);
    }

    public Page<ReservationResponse> listForUser(UUID userId, ReservationStatus status, Pageable pageable) {
        Page<Reservation> page = status != null
                ? reservationRepository.findByUserIdAndStatus(userId, status, pageable)
                : reservationRepository.findByUserId(userId, pageable);
        return page.map(ReservationResponse::from);
    }

    public Page<ReservationResponse> listAll(ReservationStatus status, Pageable pageable) {
        Page<Reservation> page = status != null
                ? reservationRepository.findByStatus(status, pageable)
                : reservationRepository.findAll(pageable);
        return page.map(ReservationResponse::from);
    }

    @Transactional
    public void cancel(UUID reservationId, UUID requesterId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException("Reserva no encontrada: " + reservationId));

        if (!isAdmin && !reservation.getUserId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No podes cancelar una reserva que no te pertenece");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new InvalidReservationStateException("La reserva ya esta cancelada");
        }

        resourceClient.updateSlotStatus(
                reservation.getResourceId(), reservation.getSlotId(), new SlotStatusUpdateRequest(SlotStatus.AVAILABLE));

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        eventPublisher.publish(new ReservationEvent(
                reservation.getId(), reservation.getUserId(), reservation.getResourceId(), reservation.getSlotId(),
                reservation.getStartTime(), reservation.getEndTime(), ReservationEventType.CANCELLED, Instant.now()));
    }
}
