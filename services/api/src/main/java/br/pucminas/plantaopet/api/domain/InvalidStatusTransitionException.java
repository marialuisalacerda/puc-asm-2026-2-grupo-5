package br.pucminas.plantaopet.api.domain;

import java.util.UUID;

/** Transição de status não permitida pela máquina de estados da reserva (HTTP 409). */
public class InvalidStatusTransitionException extends RuntimeException {

    private final UUID bookingId;
    private final BookingStatus from;
    private final BookingStatus to;

    public InvalidStatusTransitionException(UUID bookingId, BookingStatus from, BookingStatus to) {
        super("Booking %s cannot change from %s to %s".formatted(bookingId, from, to));
        this.bookingId = bookingId;
        this.from = from;
        this.to = to;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public BookingStatus getFrom() {
        return from;
    }

    public BookingStatus getTo() {
        return to;
    }
}
