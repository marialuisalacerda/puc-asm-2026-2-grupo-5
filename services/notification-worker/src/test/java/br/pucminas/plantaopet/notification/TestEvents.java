package br.pucminas.plantaopet.notification;

import br.pucminas.plantaopet.notification.messaging.BookingEventData;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;

import java.time.Instant;
import java.util.UUID;

/** Fábrica de eventos de teste com os IDs fixos do seed de demonstração (contratos-integracao.md). */
public final class TestEvents {

    public static final UUID TUTOR_ANA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PET_THOR = UUID.fromString("22222222-2222-2222-2222-222222222221");
    public static final UUID CAREGIVER_CARLA = UUID.fromString("33333333-3333-3333-3333-333333333331");

    private TestEvents() {
    }

    public static BookingEventEnvelope envelope(String eventType) {
        return envelope(UUID.randomUUID(), eventType, UUID.randomUUID(), null);
    }

    public static BookingEventEnvelope envelope(UUID eventId, String eventType, UUID bookingId, String reason) {
        BookingEventData data = new BookingEventData(
                bookingId, TUTOR_ANA, PET_THOR, CAREGIVER_CARLA, "WALK", statusFor(eventType),
                Instant.parse("2026-11-02T13:00:00Z"), Instant.parse("2026-11-02T15:00:00Z"), 6000L, reason);
        return new BookingEventEnvelope(eventId, eventType, 1, Instant.parse("2026-10-28T20:00:00Z"), data);
    }

    /** JSON no formato exato do contrato (docs/api/eventos.md). */
    public static String json(UUID eventId, String eventType, UUID bookingId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "version": 1,
                  "occurredAt": "2026-10-28T20:00:00Z",
                  "data": {
                    "bookingId": "%s",
                    "tutorId": "%s",
                    "petId": "%s",
                    "caregiverId": "%s",
                    "serviceType": "WALK",
                    "status": "%s",
                    "scheduledStart": "2026-11-02T13:00:00Z",
                    "scheduledEnd": "2026-11-02T15:00:00Z",
                    "priceCents": 6000,
                    "reason": null
                  }
                }
                """.formatted(eventId, eventType, bookingId, TUTOR_ANA, PET_THOR, CAREGIVER_CARLA, statusFor(eventType));
    }

    private static String statusFor(String eventType) {
        return switch (eventType) {
            case "BookingAccepted" -> "ACCEPTED";
            case "BookingRejected" -> "REJECTED";
            case "BookingCancelled" -> "CANCELLED";
            default -> "REQUESTED";
        };
    }
}
