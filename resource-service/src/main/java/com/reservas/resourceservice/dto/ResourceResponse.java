package com.reservas.resourceservice.dto;

import com.reservas.resourceservice.model.Resource;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String name,
        String type,
        int capacity,
        String location,
        Instant createdAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getCapacity(),
                resource.getLocation(),
                resource.getCreatedAt()
        );
    }
}
