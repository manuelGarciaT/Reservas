package com.reservas.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservas.userservice.dto.RegisterRequest;
import com.reservas.userservice.model.Role;
import com.reservas.userservice.model.User;
import com.reservas.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerLoginYAccederAUsersMe() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("integuser", "integuser@test.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("integuser"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"));

        String loginBody = """
                {"username":"integuser","password":"password123"}
                """;

        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String token = json.get("token").asText();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integuser"));
    }

    @Test
    void registerConUsernameDuplicadoDevuelve409() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("duplicado", "duplicado@test.com", "password123");

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Usuario duplicado"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("wrongpass", "wrongpass@test.com", "password123");
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        String loginBody = """
                {"username":"wrongpass","password":"incorrecta"}
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accederAUsersSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarUsuariosRequiereRolAdmin() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("plainuser", "plainuser@test.com", "password123");
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        String loginBody = """
                {"username":"plainuser","password":"password123"}
                """;
        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginBody))
                .andReturn().getResponse().getContentAsString();
        String userToken = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        User admin = User.builder()
                .username("adminuser")
                .email("adminuser@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.ROLE_ADMIN))
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        userRepository.save(admin);

        String adminLoginBody = """
                {"username":"adminuser","password":"password123"}
                """;
        String adminResponse = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(adminLoginBody))
                .andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(adminResponse).get("token").asText();

        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
