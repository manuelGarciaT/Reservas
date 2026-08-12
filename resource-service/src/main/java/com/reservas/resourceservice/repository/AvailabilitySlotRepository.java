package com.reservas.resourceservice.repository;

import com.reservas.resourceservice.model.AvailabilitySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, UUID> {

    Optional<AvailabilitySlot> findByIdAndResourceId(UUID id, UUID resourceId);

    Page<AvailabilitySlot> findByResourceId(UUID resourceId, Pageable pageable);

    Page<AvailabilitySlot> findByResourceIdAndStartTimeBetween(
            UUID resourceId, Instant from, Instant to, Pageable pageable);

    List<AvailabilitySlot> findByResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID resourceId, Instant endTime, Instant startTime);
}
