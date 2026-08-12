package com.reservas.reservationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.common.security.JwtService;
import com.reservas.reservationservice.client.ResourceClient;
import com.reservas.reservationservice.client.SlotDto;
import com.reservas.reservationservice.client.SlotStatus;
import com.reservas.reservationservice.dto.ReservationRequest;
import com.reservas.reservationservice.event.ReservationEventPublisher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Requiere `docker compose up -d postgres-test` en la raiz del repo. Si no esta
 * corriendo, el test se salta via Assumptions en vez de fallar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationPostgresIT {

    private static final String HOST = "localhost";
    private static final int PORT = 5433;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + HOST + ":" + PORT + "/testdb");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
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
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private ResourceClient resourceClient;
    @MockBean
    private ReservationEventPublisher eventPublisher;

    @Test
    void crearReservaContraPostgresReal() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);

        when(resourceClient.getSlot(resourceId, slotId))
                .thenReturn(new SlotDto(slotId, resourceId, start, start.plusSeconds(3600), SlotStatus.AVAILABLE));

        String token = jwtService.generateToken(userId, "cliente-pg", List.of("ROLE_USER"));
        ReservationRequest request = new ReservationRequest(resourceId, slotId);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
