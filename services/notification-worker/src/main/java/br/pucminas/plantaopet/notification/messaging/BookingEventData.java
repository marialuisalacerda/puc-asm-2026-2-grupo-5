package br.pucminas.plantaopet.notification.messaging;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** Campo {@code data} do envelope (docs/api/events/booking-event.schema.json, v1). */
public record BookingEventData(
        @NotNull UUID bookingId,
        @NotNull UUID tutorId,
        @NotNull UUID petId,
        @NotNull UUID caregiverId,
        @NotBlank String serviceType,
        @NotBlank String status,
        @NotNull Instant scheduledStart,
        @NotNull Instant scheduledEnd,
        @NotNull @PositiveOrZero Long priceCents,
        @Size(max = 300) String reason) {
}
