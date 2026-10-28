package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope publicado no RabbitMQ (docs/api/eventos.md, booking-event.schema.json v1).
 * Campos novos opcionais mantêm {@code version}; mudanças incompatíveis exigem nova versão.
 */
public record BookingEventMessage(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        Data data) {

    public static final int CURRENT_VERSION = 1;

    public record Data(
            UUID bookingId,
            UUID tutorId,
            UUID petId,
            UUID caregiverId,
            String serviceType,
            String status,
            Instant scheduledStart,
            Instant scheduledEnd,
            long priceCents,
            String reason) {
    }

    public static BookingEventMessage from(BookingDomainEvent event) {
        return new BookingEventMessage(
                event.eventId(),
                event.type().eventName(),
                CURRENT_VERSION,
                event.occurredAt(),
                new Data(
                        event.bookingId(),
                        event.tutorId(),
                        event.petId(),
                        event.caregiverId(),
                        event.serviceType().name(),
                        event.status().name(),
                        event.scheduledStart(),
                        event.scheduledEnd(),
                        event.priceCents(),
                        event.reason()));
    }
}
