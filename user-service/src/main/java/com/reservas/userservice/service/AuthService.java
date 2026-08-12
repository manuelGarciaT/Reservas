package com.reservas.userservice.service;

import com.reservas.userservice.dto.AuthResponse;
import com.reservas.userservice.dto.LoginRequest;
import com.reservas.userservice.dto.RegisterRequest;
import com.reservas.userservice.dto.UserResponse;
import com.reservas.userservice.exception.DuplicateUserException;
import com.reservas.userservice.model.Role;
import com.reservas.userservice.model.User;
import com.reservas.userservice.repository.UserRepository;
import com.reservas.common.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("El username '" + request.username() + "' ya esta en uso");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("El email '" + request.email() + "' ya esta en uso");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_USER))
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).toList();
        String token = jwtService.generateToken(user.getId(), user.getUsername(), roles);
        return new AuthResponse(token, jwtService.extractExpiration(token), UserResponse.from(user));
    }
}
