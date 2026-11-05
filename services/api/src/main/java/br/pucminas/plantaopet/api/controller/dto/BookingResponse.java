package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.Booking;
import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.ServiceType;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID tutorId,
        UUID petId,
        UUID caregiverId,
        ServiceType serviceType,
        BookingStatus status,
        Instant scheduledStart,
        Instant scheduledEnd,
        long priceCents,
        Instant createdAt,
        Instant updatedAt) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTutorId(),
                booking.getPetId(),
                booking.getCaregiverId(),
                booking.getServiceType(),
                booking.getStatus(),
                booking.getScheduledStart(),
                booking.getScheduledEnd(),
                booking.getPriceCents(),
                booking.getCreatedAt(),
                booking.getUpdatedAt());
    }
}
