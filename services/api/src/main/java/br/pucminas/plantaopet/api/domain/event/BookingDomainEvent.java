package br.pucminas.plantaopet.api.domain.event;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.ServiceType;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de domínio emitido (in-process, via ApplicationEventPublisher) a cada criação ou
 * transição de reserva. É um snapshot imutável: a camada de mensageria só o envia ao broker
 * após o commit da transação.
 *
 * @param eventId chave de idempotência do consumidor; gerada uma única vez por evento
 */
public record BookingDomainEvent(
        UUID eventId,
        BookingEventType type,
        Instant occurredAt,
        UUID bookingId,
        UUID tutorId,
        UUID petId,
        UUID caregiverId,
        ServiceType serviceType,
        BookingStatus status,
        Instant scheduledStart,
        Instant scheduledEnd,
        long priceCents,
        String reason) {

    public static BookingDomainEvent of(BookingEventType type, Booking booking, String reason,
                                        UUID eventId, Instant occurredAt) {
        return new BookingDomainEvent(
                eventId,
                type,
                occurredAt,
                booking.getId(),
                booking.getTutorId(),
                booking.getPetId(),
                booking.getCaregiverId(),
                booking.getServiceType(),
                booking.getStatus(),
                booking.getScheduledStart(),
                booking.getScheduledEnd(),
                booking.getPriceCents(),
                reason);
    }
}
