package br.pucminas.plantaopet.notification.messaging;

import br.pucminas.plantaopet.notification.service.NotificationMetrics;
import br.pucminas.plantaopet.notification.service.NotificationProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada AMQP: recebe a mensagem bruta, converte e delega ao serviço.
 * Exceções são relançadas para o container aplicar retry/DLQ (ver RabbitListenerConfig).
 */
@Component
public class BookingEventListener {

    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);

    private final BookingEventParser parser;
    private final NotificationProcessingService processingService;
    private final NotificationMetrics metrics;

    public BookingEventListener(BookingEventParser parser,
                                NotificationProcessingService processingService,
                                NotificationMetrics metrics) {
        this.parser = parser;
        this.processingService = processingService;
        this.metrics = metrics;
    }

    @RabbitListener(id = "bookingEventsListener", queues = "${notification.messaging.queue}")
    public void onMessage(Message message) {
        BookingEventEnvelope envelope;
        try {
            envelope = parser.parse(message.getBody());
        } catch (InvalidEventException e) {
            metrics.error(NotificationMetrics.UNKNOWN_EVENT_TYPE);
            log.warn("Event rejected to DLQ reason=\"{}\" messageId={}",
                    e.getMessage(), message.getMessageProperties().getMessageId());
            throw e;
        }

        try {
            processingService.process(envelope);
        } catch (InvalidEventException e) {
            metrics.error(envelope.eventType());
            log.warn("Event rejected to DLQ eventId={} eventType={} reason=\"{}\"",
                    envelope.eventId(), envelope.eventType(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            metrics.error(envelope.eventType());
            log.error("Event processing failed eventId={} eventType={} (will retry or dead-letter)",
                    envelope.eventId(), envelope.eventType(), e);
            throw e;
        }
    }
}
