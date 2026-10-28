package br.pucminas.plantaopet.api.messaging;

import br.pucminas.plantaopet.api.domain.event.BookingDomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Publica o envelope JSON na exchange {@code booking.events}. O corpo é serializado aqui
 * (sem o header {@code __TypeId__} do Spring) para que o consumidor dependa apenas do
 * contrato JSON e não de classes Java da API.
 */
@Component
public class RabbitBookingEventPublisher implements BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitBookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitBookingEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(BookingDomainEvent event) {
        BookingEventMessage payload = BookingEventMessage.from(event);
        String routingKey = BookingEventsTopology.routingKey(event.type());
        Message message = MessageBuilder.withBody(toJson(payload))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(payload.eventId().toString())
                .setType(payload.eventType())
                .setTimestamp(Date.from(payload.occurredAt()))
                .build();
        rabbitTemplate.send(BookingEventsTopology.EXCHANGE, routingKey, message);
        log.info("Booking event published eventId={} eventType={} bookingId={} routingKey={}",
                payload.eventId(), payload.eventType(), event.bookingId(), routingKey);
    }

    private byte[] toJson(BookingEventMessage payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize booking event " + payload.eventId(), e);
        }
    }
}
