package com.reservas.notificationservice.service;

import com.reservas.common.events.ReservationEvent;
import com.reservas.common.events.ReservationEventType;
import com.reservas.notificationservice.model.Notification;
import com.reservas.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository);
    }

    @Test
    void recordEvent_guardaNotificacionParaReservaConfirmada() {
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600), ReservationEventType.CREATED, Instant.now());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.recordEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUserId()).isEqualTo(event.userId());
        assertThat(saved.getReservationId()).isEqualTo(event.reservationId());
        assertThat(saved.getType()).isEqualTo(ReservationEventType.CREATED);
        assertThat(saved.getMessage()).containsIgnoringCase("confirmada");
    }

    @Test
    void recordEvent_guardaNotificacionParaReservaCancelada() {
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600), ReservationEventType.CANCELLED, Instant.now());

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.recordEvent(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertThat(captor.getValue().getMessage()).containsIgnoringCase("cancelada");
    }
}
