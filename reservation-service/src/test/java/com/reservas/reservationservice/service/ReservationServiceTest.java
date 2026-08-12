package com.reservas.reservationservice.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ResourceClient resourceClient;
    @Mock
    private ReservationEventPublisher eventPublisher;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, resourceClient, eventPublisher);
    }

    @Test
    void create_reservaElTurnoDisponibleYLoBloquea() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        ReservationRequest request = new ReservationRequest(resourceId, slotId);
        SlotDto slot = new SlotDto(slotId, resourceId, start, end, SlotStatus.AVAILABLE);

        when(resourceClient.getSlot(resourceId, slotId)).thenReturn(slot);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            return r;
        });

        ReservationResponse response = reservationService.create(userId, request);

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(response.userId()).isEqualTo(userId);
        verify(resourceClient).updateSlotStatus(resourceId, slotId, new SlotStatusUpdateRequest(SlotStatus.BLOCKED));
        verify(eventPublisher).publish(argThat(event -> event.type() == ReservationEventType.CREATED));
    }

    @Test
    void create_lanzaExcepcionSiElTurnoNoEstaDisponible() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        ReservationRequest request = new ReservationRequest(resourceId, slotId);
        SlotDto slot = new SlotDto(slotId, resourceId, Instant.now(), Instant.now().plusSeconds(3600), SlotStatus.BLOCKED);

        when(resourceClient.getSlot(resourceId, slotId)).thenReturn(slot);

        assertThatThrownBy(() -> reservationService.create(userId, request))
                .isInstanceOf(SlotNotAvailableException.class);

        verify(resourceClient, never()).updateSlotStatus(any(), any(), any());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancel_liberaElTurnoYMarcaLaReservaComoCancelada() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId).userId(userId)
                .resourceId(UUID.randomUUID()).slotId(UUID.randomUUID())
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .status(ReservationStatus.CONFIRMED).createdAt(Instant.now())
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.cancel(reservationId, userId, false);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(resourceClient).updateSlotStatus(
                reservation.getResourceId(), reservation.getSlotId(), new SlotStatusUpdateRequest(SlotStatus.AVAILABLE));
        verify(eventPublisher).publish(argThat(event -> event.type() == ReservationEventType.CANCELLED));
    }

    @Test
    void cancel_lanzaAccessDeniedSiNoEsElDuenioNiAdmin() {
        UUID ownerId = UUID.randomUUID();
        UUID otroUsuarioId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId).userId(ownerId)
                .resourceId(UUID.randomUUID()).slotId(UUID.randomUUID())
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .status(ReservationStatus.CONFIRMED).createdAt(Instant.now())
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(reservationId, otroUsuarioId, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(resourceClient, never()).updateSlotStatus(any(), any(), any());
    }

    @Test
    void cancel_lanzaExcepcionSiYaEstaCancelada() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.builder()
                .id(reservationId).userId(userId)
                .resourceId(UUID.randomUUID()).slotId(UUID.randomUUID())
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .status(ReservationStatus.CANCELLED).createdAt(Instant.now())
                .build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(reservationId, userId, false))
                .isInstanceOf(InvalidReservationStateException.class);
    }

    @Test
    void cancel_lanzaExcepcionSiNoExisteLaReserva() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(reservationId, UUID.randomUUID(), false))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
