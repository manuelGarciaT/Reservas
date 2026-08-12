package com.reservas.reservationservice.controller;

import com.reservas.common.security.AuthenticatedUser;
import com.reservas.reservationservice.dto.ReservationRequest;
import com.reservas.reservationservice.dto.ReservationResponse;
import com.reservas.reservationservice.model.ReservationStatus;
import com.reservas.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            Authentication authentication,
            @Valid @RequestBody ReservationRequest request
    ) {
        UUID userId = principal(authentication).id();
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(userId, request));
    }

    @GetMapping("/me")
    public Page<ReservationResponse> myReservations(
            Authentication authentication,
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = principal(authentication).id();
        return reservationService.listForUser(userId, status, pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ReservationResponse> listAll(
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reservationService.listAll(status, pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(Authentication authentication, @PathVariable UUID id) {
        UUID userId = principal(authentication).id();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        reservationService.cancel(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    private AuthenticatedUser principal(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
