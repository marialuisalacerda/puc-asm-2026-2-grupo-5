package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.BookingStatus;
import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;
import br.pucminas.plantaopet.api.domain.event.BookingEventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class RabbitBookingEventPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static BookingDomainEvent event(BookingEventType type, BookingStatus status, String reason) {
        return new BookingDomainEvent(
                UUID.fromString("4f0c1d2e-6a1b-4c39-9d7e-1b2f3a4c5d6e"), type,
                Instant.parse("2026-10-28T20:00:00Z"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222221"),
                UUID.fromString("33333333-3333-3333-3333-333333333331"),
                ServiceType.WALK, status,
                Instant.parse("2026-11-02T13:00:00Z"), Instant.parse("2026-11-02T15:00:00Z"),
                6000, reason);
    }

    @Test
    void publishesContractEnvelopeAsPersistentJson() throws Exception {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitBookingEventPublisher publisher = new RabbitBookingEventPublisher(template, objectMapper);

        publisher.publish(event(BookingEventType.CREATED, BookingStatus.REQUESTED, null));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("booking.events"), eq("booking.created"), captor.capture());
        Message message = captor.getValue();
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("4f0c1d2e-6a1b-4c39-9d7e-1b2f3a4c5d6e");
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");

        JsonNode json = objectMapper.readTree(message.getBody());
        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("eventId", "eventType", "version", "occurredAt", "data");
        assertThat(json.get("eventType").asText()).isEqualTo("BookingCreated");
        assertThat(json.get("version").asInt()).isEqualTo(1);
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-10-28T20:00:00Z");
        JsonNode data = json.get("data");
        assertThat(data.get("bookingId").asText()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(data.get("serviceType").asText()).isEqualTo("WALK");
        assertThat(data.get("status").asText()).isEqualTo("REQUESTED");
        assertThat(data.get("scheduledStart").asText()).isEqualTo("2026-11-02T13:00:00Z");
        assertThat(data.get("priceCents").asLong()).isEqualTo(6000);
        assertThat(data.has("reason")).isTrue();
        assertThat(data.get("reason").isNull()).isTrue();
    }

    @Test
    void usesRoutingKeyOfEachEventType() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        RabbitBookingEventPublisher publisher = new RabbitBookingEventPublisher(template, objectMapper);

        publisher.publish(event(BookingEventType.ACCEPTED, BookingStatus.ACCEPTED, null));
        publisher.publish(event(BookingEventType.REJECTED, BookingStatus.REJECTED, "sem agenda"));
        publisher.publish(event(BookingEventType.CANCELLED, BookingStatus.CANCELLED, null));

        verify(template).send(eq("booking.events"), eq("booking.accepted"), any(Message.class));
        verify(template).send(eq("booking.events"), eq("booking.rejected"), any(Message.class));
        verify(template).send(eq("booking.events"), eq("booking.cancelled"), any(Message.class));
    }

    @Test
    void relaySwallowsBrokerFailuresAfterCommit() {
        RabbitTemplate template = mock(RabbitTemplate.class);
        doThrow(new org.springframework.amqp.AmqpConnectException(new RuntimeException("down")))
                .when(template).send(anyString(), anyString(), any(Message.class));
        BookingEventRelay relay = new BookingEventRelay(new RabbitBookingEventPublisher(template, objectMapper));

        // não deve propagar: a reserva já foi gravada e a resposta HTTP não pode virar 500
        relay.onBookingEvent(event(BookingEventType.CREATED, BookingStatus.REQUESTED, null));

        verify(template).send(eq("booking.events"), eq("booking.created"), any(Message.class));
    }
}
