package com.reservas.resourceservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ResourceRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,

        @NotBlank(message = "El tipo es obligatorio")
        String type,

        @Positive(message = "La capacidad debe ser mayor a 0")
        int capacity,

        @NotBlank(message = "La ubicacion es obligatoria")
        String location
) {
}
