package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.ServiceType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID tutorId,
        @NotNull UUID petId,
        @NotNull UUID caregiverId,
        @NotNull ServiceType serviceType,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd) {
}
