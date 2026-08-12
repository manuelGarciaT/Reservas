package com.reservas.reservationservice.event;

import com.reservas.common.events.ReservationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;
    private final String topic;

    public ReservationEventPublisher(
            KafkaTemplate<String, ReservationEvent> kafkaTemplate,
            @Value("${app.kafka.reservation-events-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(ReservationEvent event) {
        // kafkaTemplate.send() puede lanzar de forma sincronica (ej. TimeoutException si no
        // consigue metadata del topic dentro de max.block.ms) ademas de fallar de forma
        // asincrona via el future que devuelve - hay que cubrir ambos casos para que un
        // Kafka caido nunca rompa el flujo de creacion/cancelacion de reservas.
        try {
            kafkaTemplate.send(topic, event.reservationId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("No se pudo publicar el evento {} de la reserva {}: {}",
                                    event.type(), event.reservationId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("No se pudo publicar el evento {} de la reserva {}: {}",
                    event.type(), event.reservationId(), ex.getMessage());
        }
    }
}
