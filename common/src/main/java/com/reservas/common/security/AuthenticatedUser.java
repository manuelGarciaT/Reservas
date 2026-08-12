package com.reservas.common.security;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String username) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }
}
