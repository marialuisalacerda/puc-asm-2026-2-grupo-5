package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.event.BookingEventType;

/** Nomes da topologia RabbitMQ definidos em docs/api/eventos.md. */
public final class BookingEventsTopology {

    public static final String EXCHANGE = "booking.events";

    private BookingEventsTopology() {
    }

    public static String routingKey(BookingEventType type) {
        return switch (type) {
            case CREATED -> "booking.created";
            case ACCEPTED -> "booking.accepted";
            case REJECTED -> "booking.rejected";
            case CANCELLED -> "booking.cancelled";
        };
    }
}
