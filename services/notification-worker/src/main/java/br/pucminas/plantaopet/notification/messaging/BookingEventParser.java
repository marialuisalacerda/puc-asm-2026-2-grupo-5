package br.pucminas.plantaopet.notification.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

/** Converte o corpo da mensagem no envelope e valida o contrato v1. */
@Component
public class BookingEventParser {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public BookingEventParser(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public BookingEventEnvelope parse(byte[] body) {
        if (body == null || body.length == 0) {
            throw new InvalidEventException("Empty message body");
        }
        BookingEventEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, BookingEventEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new InvalidEventException("Malformed event JSON: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new InvalidEventException("Unreadable event body", e);
        }
        if (envelope == null) {
            throw new InvalidEventException("Event body is null");
        }

        Set<ConstraintViolation<BookingEventEnvelope>> violations = validator.validate(envelope);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException("Invalid event envelope: " + details);
        }
        if (envelope.version() != BookingEventEnvelope.SUPPORTED_VERSION) {
            throw new InvalidEventException("Unsupported event version " + envelope.version()
                    + " for eventType=" + envelope.eventType());
        }
        return envelope;
    }
}
