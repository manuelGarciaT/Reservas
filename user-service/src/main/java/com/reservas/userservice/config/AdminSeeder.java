package com.reservas.userservice.config;

import com.reservas.userservice.model.Role;
import com.reservas.userservice.model.User;
import com.reservas.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Crea una cuenta admin por defecto en entornos de desarrollo/demo para poder
 * probar los endpoints protegidos con ROLE_ADMIN sin pasos manuales.
 */
@Component
@Profile({"local", "docker"})
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email("admin@reservas.local")
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .roles(Set.of(Role.ROLE_ADMIN))
                .enabled(true)
                .build();

        userRepository.save(admin);
    }
}
