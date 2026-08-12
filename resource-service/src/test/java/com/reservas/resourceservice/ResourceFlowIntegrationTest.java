package com.reservas.resourceservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.common.security.JwtService;
import com.reservas.resourceservice.dto.ResourceRequest;
import com.reservas.resourceservice.dto.SlotRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class ResourceFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    private String adminToken() {
        return jwtService.generateToken(UUID.randomUUID(), "admin", List.of("ROLE_ADMIN"));
    }

    private String userToken() {
        return jwtService.generateToken(UUID.randomUUID(), "user", List.of("ROLE_USER"));
    }

    @Test
    void listarRecursosEsPublico() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().isOk());
    }

    @Test
    void crearRecursoSinTokenDevuelve401() throws Exception {
        ResourceRequest request = new ResourceRequest("Cancha X", "CANCHA", 10, "Sede Centro");
        mockMvc.perform(post("/resources")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearRecursoConRolUserDevuelve403() throws Exception {
        ResourceRequest request = new ResourceRequest("Cancha Y", "CANCHA", 10, "Sede Centro");
        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void flujoCompletoDeRecursoYTurno() throws Exception {
        ResourceRequest resourceRequest = new ResourceRequest("Cancha Padel", "CANCHA", 4, "Sede Centro");

        String resourceResponse = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resourceRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String resourceId = objectMapper.readTree(resourceResponse).get("id").asText();

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        SlotRequest slotRequest = new SlotRequest(start, start.plusSeconds(3600));

        String slotResponse = mockMvc.perform(post("/resources/" + resourceId + "/slots")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(slotRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        String slotId = objectMapper.readTree(slotResponse).get("id").asText();

        mockMvc.perform(get("/resources/" + resourceId + "/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(slotId));

        String blockBody = """
                {"status":"BLOCKED"}
                """;

        mockMvc.perform(patch("/resources/" + resourceId + "/slots/" + slotId + "/status")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType("application/json")
                        .content(blockBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(patch("/resources/" + resourceId + "/slots/" + slotId + "/status")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType("application/json")
                        .content(blockBody))
                .andExpect(status().isConflict());
    }
}
