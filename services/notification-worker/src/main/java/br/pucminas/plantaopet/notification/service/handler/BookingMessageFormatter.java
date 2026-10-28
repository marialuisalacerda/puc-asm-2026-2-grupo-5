package br.pucminas.plantaopet.notification.service.handler;

import br.pucminas.plantaopet.notification.messaging.BookingEventData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Formatação comum dos textos de notificação (pt-BR, horário de Brasília). */
final class BookingMessageFormatter {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZONE);
    private static final Map<String, String> SERVICE_NAMES = Map.of(
            "WALK", "passeio",
            "COMPANIONSHIP", "companhia",
            "HOME_VISIT", "visita",
            "HOSTING", "hospedagem",
            "TEMPORARY_CARE", "cuidado temporário");

    private BookingMessageFormatter() {
    }

    static String service(BookingEventData data) {
        return SERVICE_NAMES.getOrDefault(data.serviceType(), data.serviceType());
    }

    static String period(BookingEventData data) {
        return format(data.scheduledStart()) + " a " + format(data.scheduledEnd());
    }

    static String price(BookingEventData data) {
        long cents = data.priceCents();
        return String.format(Locale.ROOT, "R$ %d,%02d", cents / 100, cents % 100);
    }

    static String reasonSuffix(BookingEventData data) {
        return data.reason() == null || data.reason().isBlank() ? "" : " Motivo: " + data.reason().trim();
    }

    private static String format(Instant instant) {
        return DATE_TIME.format(instant);
    }
}
