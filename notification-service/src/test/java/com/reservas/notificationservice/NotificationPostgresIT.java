package com.reservas.notificationservice;

import com.reservas.common.events.ReservationEvent;
import com.reservas.common.events.ReservationEventType;
import com.reservas.notificationservice.dto.NotificationResponse;
import com.reservas.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requiere `docker compose up -d postgres-test` en la raiz del repo. Si no esta
 * corriendo, el test se salta via Assumptions en vez de fallar.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationPostgresIT {

    private static final String HOST = "localhost";
    private static final int PORT = 5433;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + HOST + ":" + PORT + "/testdb");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Este test ejercita solo la capa JPA/Postgres, no Kafka (eso ya lo cubre
        // ReservationEventFlowIntegrationTest con @EmbeddedKafka) - evita que el
        // listener reintente conectarse en background contra un broker inexistente.
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @BeforeAll
    static void checkPostgresAvailable() {
        Assumptions.assumeTrue(isReachable(HOST, PORT),
                "postgres-test no esta corriendo (docker compose up -d postgres-test): se salta este test");
    }

    private static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Autowired
    private NotificationService notificationService;

    @Test
    void recordEventYListarContraPostgresReal() {
        UUID userId = UUID.randomUUID();
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600), ReservationEventType.CREATED, Instant.now());

        notificationService.recordEvent(event);

        Page<NotificationResponse> page = notificationService.listForUser(userId, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).type()).isEqualTo(ReservationEventType.CREATED);
    }
}
