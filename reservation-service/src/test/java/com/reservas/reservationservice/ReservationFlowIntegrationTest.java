package com.reservas.reservationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.common.security.JwtService;
import com.reservas.reservationservice.client.ResourceClient;
import com.reservas.reservationservice.client.SlotDto;
import com.reservas.reservationservice.client.SlotStatus;
import com.reservas.reservationservice.client.SlotStatusUpdateRequest;
import com.reservas.reservationservice.dto.ReservationRequest;
import com.reservas.reservationservice.event.ReservationEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class ReservationFlowIntegrationTest {

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

    private String tokenFor(UUID userId, String username, String role) {
        return jwtService.generateToken(userId, username, List.of(role));
    }

    @Test
    void crearReservaSinTokenDevuelve401() throws Exception {
        ReservationRequest request = new ReservationRequest(UUID.randomUUID(), UUID.randomUUID());
        mockMvc.perform(post("/reservations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearReservaConTurnoDisponibleYCancelarla() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        when(resourceClient.getSlot(resourceId, slotId))
                .thenReturn(new SlotDto(slotId, resourceId, start, end, SlotStatus.AVAILABLE));
        when(resourceClient.updateSlotStatus(any(), any(), any()))
                .thenReturn(new SlotDto(slotId, resourceId, start, end, SlotStatus.BLOCKED));

        String token = tokenFor(userId, "cliente1", "ROLE_USER");
        ReservationRequest request = new ReservationRequest(resourceId, slotId);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();

        String reservationId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/reservations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(delete("/reservations/" + reservationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void crearReservaConTurnoNoDisponibleDevuelve409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();

        when(resourceClient.getSlot(resourceId, slotId))
                .thenReturn(new SlotDto(slotId, resourceId, Instant.now(), Instant.now().plusSeconds(3600), SlotStatus.BLOCKED));

        String token = tokenFor(userId, "cliente2", "ROLE_USER");
        ReservationRequest request = new ReservationRequest(resourceId, slotId);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void listarTodasLasReservasRequiereRolAdmin() throws Exception {
        String userToken = tokenFor(UUID.randomUUID(), "cliente3", "ROLE_USER");
        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String adminToken = tokenFor(UUID.randomUUID(), "admin1", "ROLE_ADMIN");
        mockMvc.perform(get("/reservations").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void filtrarReservasPropiasPorStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = tokenFor(userId, "cliente-filtro", "ROLE_USER");

        UUID resourceId1 = UUID.randomUUID();
        UUID slotId1 = UUID.randomUUID();
        Instant start1 = Instant.now().plusSeconds(3600);
        when(resourceClient.getSlot(resourceId1, slotId1))
                .thenReturn(new SlotDto(slotId1, resourceId1, start1, start1.plusSeconds(3600), SlotStatus.AVAILABLE));
        when(resourceClient.updateSlotStatus(eq(resourceId1), eq(slotId1), any()))
                .thenReturn(new SlotDto(slotId1, resourceId1, start1, start1.plusSeconds(3600), SlotStatus.BLOCKED));

        String toCancelResponse = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReservationRequest(resourceId1, slotId1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String toCancelId = objectMapper.readTree(toCancelResponse).get("id").asText();

        mockMvc.perform(delete("/reservations/" + toCancelId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        UUID resourceId2 = UUID.randomUUID();
        UUID slotId2 = UUID.randomUUID();
        Instant start2 = Instant.now().plusSeconds(7200);
        when(resourceClient.getSlot(resourceId2, slotId2))
                .thenReturn(new SlotDto(slotId2, resourceId2, start2, start2.plusSeconds(3600), SlotStatus.AVAILABLE));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ReservationRequest(resourceId2, slotId2))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/reservations/me?status=CANCELLED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(toCancelId));

        mockMvc.perform(get("/reservations/me?status=CONFIRMED").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)));

        mockMvc.perform(get("/reservations/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    void cancelarReservaDeOtroUsuarioDevuelve403() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Instant start = Instant.now().plusSeconds(3600);

        when(resourceClient.getSlot(resourceId, slotId))
                .thenReturn(new SlotDto(slotId, resourceId, start, start.plusSeconds(3600), SlotStatus.AVAILABLE));
        when(resourceClient.updateSlotStatus(any(), any(), any()))
                .thenReturn(new SlotDto(slotId, resourceId, start, start.plusSeconds(3600), SlotStatus.BLOCKED));

        String ownerToken = tokenFor(ownerId, "dueño", "ROLE_USER");
        ReservationRequest request = new ReservationRequest(resourceId, slotId);

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reservationId = objectMapper.readTree(response).get("id").asText();

        String otroToken = tokenFor(UUID.randomUUID(), "otro", "ROLE_USER");
        mockMvc.perform(delete("/reservations/" + reservationId).header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isForbidden());
    }
}
