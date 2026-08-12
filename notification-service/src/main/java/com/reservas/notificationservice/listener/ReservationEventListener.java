package com.reservas.notificationservice.listener;

import com.reservas.common.events.ReservationEvent;
import com.reservas.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventListener.class);

    private final NotificationService notificationService;

    public ReservationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.reservation-events-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onReservationEvent(ReservationEvent event) {
        log.info("Evento recibido: {} para la reserva {} (usuario {})", event.type(), event.reservationId(), event.userId());
        notificationService.recordEvent(event);
    }
}
