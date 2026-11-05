package br.pucminas.plantaopet.notification.messaging;

import br.pucminas.plantaopet.notification.TestEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingEventParserTest {

    private BookingEventParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        parser = new BookingEventParser(objectMapper, validator);
    }

    @Test
    void parsesContractEnvelope() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        BookingEventEnvelope envelope = parser.parse(bytes(TestEvents.json(eventId, "BookingCreated", bookingId)));

        assertThat(envelope.eventId()).isEqualTo(eventId);
        assertThat(envelope.eventType()).isEqualTo("BookingCreated");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2026-10-28T20:00:00Z"));
        assertThat(envelope.data().bookingId()).isEqualTo(bookingId);
        assertThat(envelope.data().priceCents()).isEqualTo(6000L);
        assertThat(envelope.data().reason()).isNull();
    }

    @Test
    void ignoresUnknownOptionalFieldsForForwardCompatibility() {
        String json = TestEvents.json(UUID.randomUUID(), "BookingCreated", UUID.randomUUID())
                .replace("\"reason\": null", "\"reason\": null, \"newOptionalField\": \"x\"");

        assertThat(parser.parse(bytes(json)).eventType()).isEqualTo("BookingCreated");
    }

    @Test
    void malformedJsonIsRejectedWithoutRequeue() {
        assertThatThrownBy(() -> parser.parse(bytes("{not-json")))
                .isInstanceOf(InvalidEventException.class)
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasMessageContaining("Malformed");
    }

    @Test
    void emptyBodyIsRejected() {
        assertThatThrownBy(() -> parser.parse(new byte[0])).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void missingRequiredFieldIsRejected() {
        String json = TestEvents.json(UUID.randomUUID(), "BookingCreated", UUID.randomUUID())
                .replaceFirst("\"bookingId\": \"[^\"]+\",", "");

        assertThatThrownBy(() -> parser.parse(bytes(json)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("data.bookingId");
    }

    @Test
    void unsupportedVersionIsRejected() {
        String json = TestEvents.json(UUID.randomUUID(), "BookingCreated", UUID.randomUUID())
                .replace("\"version\": 1", "\"version\": 2");

        assertThatThrownBy(() -> parser.parse(bytes(json)))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("version 2");
    }

    @Test
    void invalidUuidIsRejected() {
        String json = TestEvents.json(UUID.randomUUID(), "BookingCreated", UUID.randomUUID())
                .replaceFirst("\"eventId\": \"[^\"]+\"", "\"eventId\": \"abc\"");

        assertThatThrownBy(() -> parser.parse(bytes(json))).isInstanceOf(InvalidEventException.class);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
