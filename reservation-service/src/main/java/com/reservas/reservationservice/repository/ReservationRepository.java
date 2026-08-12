package com.reservas.reservationservice.repository;

import com.reservas.reservationservice.model.Reservation;
import com.reservas.reservationservice.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(UUID userId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);
}
