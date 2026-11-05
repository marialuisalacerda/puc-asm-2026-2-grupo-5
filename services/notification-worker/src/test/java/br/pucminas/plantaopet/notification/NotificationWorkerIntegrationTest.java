package br.pucminas.plantaopet.notification;

import br.pucminas.plantaopet.notification.controller.NotificationResponse;
import br.pucminas.plantaopet.notification.domain.Notification;
import br.pucminas.plantaopet.notification.domain.RecipientType;
import br.pucminas.plantaopet.notification.repository.NotificationRepository;
import br.pucminas.plantaopet.notification.repository.ProcessedEventRepository;
import br.pucminas.plantaopet.notification.service.NotificationMetrics;
import br.pucminas.plantaopet.notification.service.sender.NotificationSender;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Fluxo real com PostgreSQL e RabbitMQ em containers: topologia, consumo, idempotência, retry e DLQ.
 * Intervalos de retry reduzidos para o teste (o padrão é 1s, 2s).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
                "spring.rabbitmq.listener.simple.retry.max-interval=400ms"
        })
class NotificationWorkerIntegrationTest {

    private static final String EXCHANGE = "booking.events";
    private static final String QUEUE = "notification.booking-events";
    private static final String DLQ = "notification.booking-events.dlq";
    private static final Duration AWAIT = Duration.ofSeconds(30);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.4-alpine")
            .withDatabaseName("notifications")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13.7-management-alpine")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private AmqpAdmin amqpAdmin;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private ProcessedEventRepository processedEventRepository;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private TestRestTemplate restTemplate;
    @SpyBean
    private NotificationSender notificationSender;

    @BeforeEach
    void purgeDeadLetterQueue() {
        amqpAdmin.purgeQueue(DLQ, false);
    }

    @Test
    void topologyIsDeclaredWithDeadLetterExchange() {
        assertThat(amqpAdmin.getQueueInfo(QUEUE)).isNotNull();
        assertThat(amqpAdmin.getQueueInfo(DLQ)).isNotNull();
    }

    @Test
    void bookingCreatedPublishedOnExchangeIsStoredAsCaregiverNotification() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        publish("booking.created", TestEvents.json(eventId, "BookingCreated", bookingId));

        await().atMost(AWAIT).untilAsserted(() ->
                assertThat(notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).hasSize(1));

        Notification notification = notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).get(0);
        assertThat(notification.getEventId()).isEqualTo(eventId);
        assertThat(notification.getRecipientType()).isEqualTo(RecipientType.CAREGIVER);
        assertThat(notification.getRecipientId()).isEqualTo(TestEvents.CAREGIVER_CARLA);
        assertThat(processedEventRepository.existsById(eventId)).isTrue();

        NotificationResponse[] body = restTemplate.getForObject("/notifications?bookingId=" + bookingId,
                NotificationResponse[].class);
        assertThat(body).hasSize(1);
        assertThat(body[0].eventId()).isEqualTo(eventId);
    }

    @Test
    void sameEventIdDeliveredTwiceProducesASingleNotification() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        double duplicatesBefore = processingCount("BookingAccepted", "duplicate");
        String json = TestEvents.json(eventId, "BookingAccepted", bookingId);

        publish("booking.accepted", json);
        publish("booking.accepted", json);

        await().atMost(AWAIT).untilAsserted(() ->
                assertThat(processingCount("BookingAccepted", "duplicate")).isEqualTo(duplicatesBefore + 1));

        assertThat(notificationRepository.countByEventId(eventId)).isEqualTo(1);
        List<Notification> notifications = notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getRecipientType()).isEqualTo(RecipientType.TUTOR);
    }

    @Test
    void malformedMessageGoesStraightToTheDeadLetterQueue() {
        String garbage = "isto-nao-e-json {" + UUID.randomUUID();

        publish("booking.created", garbage);

        Message dead = receiveFromDlq();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo(garbage);
        assertThat(firstDeath(dead)).containsEntry("queue", QUEUE).containsEntry("reason", "rejected");
        verify(notificationSender, never()).send(argThat(n -> true));
    }

    @Test
    void unknownEventTypeGoesToTheDeadLetterQueueWithoutRetry() {
        UUID bookingId = UUID.randomUUID();
        publish("booking.paid", TestEvents.json(UUID.randomUUID(), "BookingPaid", bookingId));

        Message dead = receiveFromDlq();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains("BookingPaid");
        assertThat(notificationRepository.findByBookingIdOrderByCreatedAtAsc(bookingId)).isEmpty();
    }

    @Test
    void transientFailureIsRetriedThreeTimesThenDeadLetteredAndRolledBack() {
        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ArgumentMatcher<Notification> forBooking = n -> n != null && bookingId.equals(n.getBookingId());
        doThrow(new IllegalStateException("provedor indisponível"))
                .when(notificationSender).send(argThat(forBooking));

        publish("booking.cancelled", TestEvents.json(eventId, "BookingCancelled", bookingId));

        Message dead = receiveFromDlq();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).contains(eventId.toString());
        verify(notificationSender, times(3)).send(argThat(forBooking));
        // a transação foi desfeita em todas as tentativas
        assertThat(processedEventRepository.existsById(eventId)).isFalse();
        assertThat(notificationRepository.countByEventId(eventId)).isZero();
        assertThat(errorCount("BookingCancelled")).isGreaterThanOrEqualTo(3);
    }

    private void publish(String routingKey, String body) {
        Message message = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(UUID.randomUUID().toString())
                .build();
        rabbitTemplate.send(EXCHANGE, routingKey, message);
    }

    private Message receiveFromDlq() {
        Message message = rabbitTemplate.receive(DLQ, AWAIT.toMillis());
        assertThat(message).as("mensagem na DLQ").isNotNull();
        return message;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstDeath(Message message) {
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) message.getMessageProperties()
                .getHeaders().get("x-death");
        assertThat(xDeath).isNotEmpty();
        return xDeath.get(0);
    }

    private double processingCount(String eventType, String outcome) {
        Counter counter = meterRegistry.find(NotificationMetrics.PROCESSING)
                .tag(NotificationMetrics.TAG_EVENT_TYPE, eventType)
                .tag(NotificationMetrics.TAG_OUTCOME, outcome)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }

    private double errorCount(String eventType) {
        Counter counter = meterRegistry.find(NotificationMetrics.PROCESSING_ERRORS)
                .tag(NotificationMetrics.TAG_EVENT_TYPE, eventType)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
