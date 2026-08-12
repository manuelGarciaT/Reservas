package com.reservas.userservice.service;

import com.reservas.userservice.dto.AuthResponse;
import com.reservas.userservice.dto.LoginRequest;
import com.reservas.userservice.dto.RegisterRequest;
import com.reservas.userservice.exception.DuplicateUserException;
import com.reservas.userservice.model.Role;
import com.reservas.userservice.model.User;
import com.reservas.userservice.repository.UserRepository;
import com.reservas.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_creaUsuarioConRolUserYDevuelveToken() {
        RegisterRequest request = new RegisterRequest("nuevo", "nuevo@test.com", "password123");

        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            return u;
        });
        when(jwtService.generateToken(any(UUID.class), anyString(), anyList())).thenReturn("fake-jwt-token");
        when(jwtService.extractExpiration("fake-jwt-token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        assertThat(response.user().username()).isEqualTo("nuevo");
        assertThat(response.user().roles()).containsExactly(Role.ROLE_USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void register_lanzaExcepcionSiElUsernameYaExiste() {
        RegisterRequest request = new RegisterRequest("existente", "otro@test.com", "password123");
        when(userRepository.existsByUsername("existente")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("existente");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_lanzaExcepcionSiElEmailYaExiste() {
        RegisterRequest request = new RegisterRequest("nuevo", "repetido@test.com", "password123");
        when(userRepository.existsByUsername("nuevo")).thenReturn(false);
        when(userRepository.existsByEmail("repetido@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("repetido@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_devuelveTokenCuandoLasCredencialesSonValidas() {
        LoginRequest request = new LoginRequest("usuario", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("usuario")
                .email("usuario@test.com")
                .passwordHash("hashed")
                .roles(Set.of(Role.ROLE_USER))
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findByUsername("usuario")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getId(), user.getUsername(), List.of("ROLE_USER"))).thenReturn("fake-jwt-token");
        when(jwtService.extractExpiration("fake-jwt-token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("fake-jwt-token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_propagaExcepcionDeAutenticacionSiLasCredencialesSonInvalidas() {
        LoginRequest request = new LoginRequest("usuario", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsername(anyString());
    }
}
