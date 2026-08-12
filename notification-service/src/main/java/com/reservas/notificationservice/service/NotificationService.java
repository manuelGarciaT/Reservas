package com.reservas.notificationservice.service;

import com.reservas.common.events.ReservationEvent;
import com.reservas.notificationservice.dto.NotificationResponse;
import com.reservas.notificationservice.model.Notification;
import com.reservas.notificationservice.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;

@Service
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(new Locale("es", "AR")).withZone(java.time.ZoneOffset.UTC);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void recordEvent(ReservationEvent event) {
        String message = switch (event.type()) {
            case CREATED -> "Tu reserva para el " + TIME_FORMAT.format(event.startTime()) + " fue confirmada.";
            case CANCELLED -> "Tu reserva para el " + TIME_FORMAT.format(event.startTime()) + " fue cancelada.";
        };

        Notification notification = Notification.builder()
                .recipientUserId(event.userId())
                .reservationId(event.reservationId())
                .resourceId(event.resourceId())
                .slotId(event.slotId())
                .type(event.type())
                .startTime(event.startTime())
                .endTime(event.endTime())
                .message(message)
                .build();

        notificationRepository.save(notification);
    }

    public Page<NotificationResponse> listForUser(UUID recipientUserId, Pageable pageable) {
        return notificationRepository.findByRecipientUserId(recipientUserId, pageable).map(NotificationResponse::from);
    }

    public Page<NotificationResponse> listAll(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(NotificationResponse::from);
    }
}
