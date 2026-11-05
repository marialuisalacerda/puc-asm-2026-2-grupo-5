package br.pucminas.plantaopet.api.domain.event;

/** Tipos de evento de domínio da reserva (docs/api/eventos.md). */
public enum BookingEventType {
    CREATED("BookingCreated"),
    ACCEPTED("BookingAccepted"),
    REJECTED("BookingRejected"),
    CANCELLED("BookingCancelled");

    private final String eventName;

    BookingEventType(String eventName) {
        this.eventName = eventName;
    }

    /** Valor do campo {@code eventType} do envelope. */
    public String eventName() {
        return eventName;
    }
}
