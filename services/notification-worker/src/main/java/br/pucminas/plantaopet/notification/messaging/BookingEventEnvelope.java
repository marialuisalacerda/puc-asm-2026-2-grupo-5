package br.pucminas.plantaopet.notification.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** Envelope comum a todos os eventos de reserva (docs/api/eventos.md). */
public record BookingEventEnvelope(
        @NotNull UUID eventId,
        @NotBlank String eventType,
        @NotNull Integer version,
        @NotNull Instant occurredAt,
        @NotNull @Valid BookingEventData data) {

    /** Única versão suportada do contrato. */
    public static final int SUPPORTED_VERSION = 1;
}
