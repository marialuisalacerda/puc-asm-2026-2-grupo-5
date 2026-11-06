package br.pucminas.plantaopet.notification.messaging;

import br.pucminas.plantaopet.notification.TestEvents;
import br.pucminas.plantaopet.notification.service.NotificationMetrics;
import br.pucminas.plantaopet.notification.service.NotificationProcessingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventListenerTest {

    @Mock
    private BookingEventParser parser;
    @Mock
    private NotificationProcessingService processingService;

    private SimpleMeterRegistry meterRegistry;
    private BookingEventListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new BookingEventListener(parser, processingService, new NotificationMetrics(meterRegistry));
    }

    @Test
    void validMessageIsDelegatedToTheService() {
        BookingEventEnvelope event = TestEvents.envelope("BookingCreated");
        when(parser.parse(any())).thenReturn(event);

        listener.onMessage(message("{}"));

        verify(processingService).process(event);
        assertThat(meterRegistry.find(NotificationMetrics.PROCESSING_ERRORS).counters()).isEmpty();
    }

    @Test
    void invalidMessageIsRejectedAndCountedAsUnknownError() {
        when(parser.parse(any())).thenThrow(new InvalidEventException("Malformed event JSON"));

        assertThatThrownBy(() -> listener.onMessage(message("lixo")))
                .isInstanceOf(InvalidEventException.class);

        verifyNoInteractions(processingService);
        assertThat(errors("unknown")).isEqualTo(1.0);
    }

    @Test
    void processingFailureIsRethrownForRetryAndCountedByEventType() {
        BookingEventEnvelope event = TestEvents.envelope("BookingRejected");
        when(parser.parse(any())).thenReturn(event);
        when(processingService.process(event)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> listener.onMessage(message("{}")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(errors("BookingRejected")).isEqualTo(1.0);
    }

    private double errors(String eventType) {
        var counter = meterRegistry.find(NotificationMetrics.PROCESSING_ERRORS)
                .tag(NotificationMetrics.TAG_EVENT_TYPE, eventType).counter();
        return counter == null ? 0.0 : counter.count();
    }

    private static Message message(String body) {
        return new Message(body.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
