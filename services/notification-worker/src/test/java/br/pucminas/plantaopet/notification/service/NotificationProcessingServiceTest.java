package br.pucminas.plantaopet.notification.service;

import br.pucminas.plantaopet.notification.TestEvents;
import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.NotificationChannel;
import br.pucminas.plantaopet.notification.domain.ProcessedEvent;
import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.messaging.BookingEventEnvelope;
import br.pucminas.plantaopet.notification.messaging.InvalidEventException;
import br.pucminas.plantaopet.notification.repository.NotificationRepository;
import br.pucminas.plantaopet.notification.repository.ProcessedEventRepository;
import br.pucminas.plantaopet.notification.service.handler.BookingAcceptedHandler;
import br.pucminas.plantaopet.notification.service.handler.BookingCancelledHandler;
import br.pucminas.plantaopet.notification.service.handler.BookingCreatedHandler;
import br.pucminas.plantaopet.notification.service.handler.BookingEventHandlerRegistry;
import br.pucminas.plantaopet.notification.service.handler.BookingRejectedHandler;
import br.pucminas.plantaopet.notification.service.sender.NotificationSender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T21:00:00Z");

    @Mock
    private ProcessedEventRepository processedEventRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationSender notificationSender;

    private SimpleMeterRegistry meterRegistry;
    private NotificationProcessingService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        BookingEventHandlerRegistry registry = new BookingEventHandlerRegistry(List.of(
                new BookingCreatedHandler(), new BookingAcceptedHandler(),
                new BookingRejectedHandler(), new BookingCancelledHandler()));
        service = new NotificationProcessingService(registry, processedEventRepository, notificationRepository,
                notificationSender, new NotificationMetrics(meterRegistry),
                TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void newEventStoresIdempotencyKeyAndNotificationAndSendsIt() {
        BookingEventEnvelope event = TestEvents.envelope("BookingCreated");
        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        when(notificationSender.channel()).thenReturn(NotificationChannel.LOG);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessingOutcome outcome = service.process(event);

        assertThat(outcome).isEqualTo(ProcessingOutcome.PROCESSED);

        ArgumentCaptor<ProcessedEvent> processed = ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).save(processed.capture());
        assertThat(processed.getValue().getEventId()).isEqualTo(event.eventId());
        assertThat(processed.getValue().getEventType()).isEqualTo("BookingCreated");
        assertThat(processed.getValue().getProcessedAt()).isEqualTo(NOW);

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationSender).send(notification.capture());
        Notification sent = notification.getValue();
        assertThat(sent.getEventId()).isEqualTo(event.eventId());
        assertThat(sent.getBookingId()).isEqualTo(event.data().bookingId());
        assertThat(sent.getRecipientType()).isEqualTo(RecipientType.CAREGIVER);
        assertThat(sent.getRecipientId()).isEqualTo(TestEvents.CAREGIVER_CARLA);
        assertThat(sent.getChannel()).isEqualTo(NotificationChannel.LOG);
        assertThat(sent.getCreatedAt()).isEqualTo(NOW);

        assertThat(counter("BookingCreated", "processed")).isEqualTo(1.0);
        assertThat(counter("BookingCreated", "duplicate")).isZero();
    }

    @Test
    void duplicateEventIsIgnoredAndCountedAsDuplicate() {
        BookingEventEnvelope event = TestEvents.envelope("BookingAccepted");
        when(processedEventRepository.existsById(event.eventId())).thenReturn(true);

        ProcessingOutcome outcome = service.process(event);

        assertThat(outcome).isEqualTo(ProcessingOutcome.DUPLICATE);
        verify(processedEventRepository, never()).save(any());
        verifyNoInteractions(notificationRepository, notificationSender);
        assertThat(counter("BookingAccepted", "duplicate")).isEqualTo(1.0);
        assertThat(counter("BookingAccepted", "processed")).isZero();
    }

    @Test
    void unknownEventTypeIsRejectedWithoutTouchingTheDatabase() {
        BookingEventEnvelope event = TestEvents.envelope("BookingPaid");

        assertThatThrownBy(() -> service.process(event))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("BookingPaid");

        verifyNoInteractions(processedEventRepository, notificationRepository, notificationSender);
        assertThat(meterRegistry.find(NotificationMetrics.PROCESSING).counters()).isEmpty();
    }

    @Test
    void sendFailurePropagatesSoTheTransactionRollsBackAndTheMessageIsRetried() {
        BookingEventEnvelope event = TestEvents.envelope("BookingCancelled");
        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        when(notificationSender.channel()).thenReturn(NotificationChannel.LOG);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("provider down")).when(notificationSender).send(any());

        assertThatThrownBy(() -> service.process(event)).isInstanceOf(IllegalStateException.class);

        assertThat(meterRegistry.find(NotificationMetrics.PROCESSING).counters()).isEmpty();
    }

    private double counter(String eventType, String outcome) {
        var counter = meterRegistry.find(NotificationMetrics.PROCESSING)
                .tag(NotificationMetrics.TAG_EVENT_TYPE, eventType)
                .tag(NotificationMetrics.TAG_OUTCOME, outcome)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
