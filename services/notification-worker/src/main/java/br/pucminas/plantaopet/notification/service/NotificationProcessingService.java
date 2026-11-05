package br.pucminas.plantaopet.notification.service;

import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.ProcessedEvent;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import br.pucminas.plantaopet.notification.messaging.InvalidEventException;
import br.pucminas.plantaopet.notification.repository.NotificationRepository;
import br.pucminas.plantaopet.notification.repository.ProcessedEventRepository;
import br.pucminas.plantaopet.notification.service.handler.BookingEventHandler;
import br.pucminas.plantaopet.notification.service.handler.BookingEventHandlerRegistry;
import br.pucminas.plantaopet.notification.service.handler.NotificationDraft;
import br.pucminas.plantaopet.notification.service.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Processamento idempotente de eventos de reserva.
 *
 * <p>Em uma única transação do banco do worker: verifica {@code processed_events}; se o
 * {@code eventId} é novo, grava o registro de idempotência, a notificação e faz o envio (simulado).
 * Qualquer falha desfaz tudo e a mensagem é retentada. Se duas entregas do mesmo evento correrem em
 * paralelo, a PK de {@code processed_events} faz uma delas falhar; na retentativa ela vira duplicata.
 *
 * <p>Garantia resultante: <b>at-least-once</b> na entrega do broker e <b>exatamente um</b> registro
 * de notificação por {@code eventId}.
 */
@Service
public class NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final BookingEventHandlerRegistry handlerRegistry;
    private final ProcessedEventRepository processedEventRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final NotificationMetrics metrics;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    public NotificationProcessingService(BookingEventHandlerRegistry handlerRegistry,
                                         ProcessedEventRepository processedEventRepository,
                                         NotificationRepository notificationRepository,
                                         NotificationSender notificationSender,
                                         NotificationMetrics metrics,
                                         TransactionOperations transactionOperations,
                                         Clock clock) {
        this.handlerRegistry = handlerRegistry;
        this.processedEventRepository = processedEventRepository;
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
        this.metrics = metrics;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public ProcessingOutcome process(BookingEventEnvelope event) {
        BookingEventHandler handler = handlerRegistry.find(event.eventType())
                .orElseThrow(() -> new InvalidEventException("Unknown eventType=" + event.eventType()));

        ProcessingOutcome outcome = transactionOperations.execute(status -> {
            if (processedEventRepository.existsById(event.eventId())) {
                return ProcessingOutcome.DUPLICATE;
            }
            Instant now = Instant.now(clock);
            NotificationDraft draft = handler.handle(event);

            processedEventRepository.save(new ProcessedEvent(event.eventId(), event.eventType(), now));
            Notification notification = notificationRepository.save(new Notification(
                    UUID.randomUUID(),
                    event.eventId(),
                    event.data().bookingId(),
                    draft.recipientType(),
                    draft.recipientId(),
                    notificationSender.channel(),
                    draft.message(),
                    now));
            notificationSender.send(notification);
            return ProcessingOutcome.PROCESSED;
        });

        metrics.outcome(event.eventType(), outcome);
        if (outcome == ProcessingOutcome.DUPLICATE) {
            log.info("Duplicate event ignored eventId={} eventType={} bookingId={}",
                    event.eventId(), event.eventType(), event.data().bookingId());
        } else {
            log.info("Event processed eventId={} eventType={} bookingId={}",
                    event.eventId(), event.eventType(), event.data().bookingId());
        }
        return outcome;
    }
}
