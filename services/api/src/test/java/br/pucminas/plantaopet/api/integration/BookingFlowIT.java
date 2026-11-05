package br.pucminas.plantaopet.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo completo com PostgreSQL e RabbitMQ reais: Flyway (schema + seeds), POST /bookings,
 * linha gravada com price_cents e evento publicado na exchange booking.events após o commit.
 */
@Testcontainers
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingFlowIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.4-alpine")
            .withDatabaseName("plantao_pet")
            .withStartupTimeout(Duration.ofMinutes(3));

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13.7-management-alpine")
            .withStartupTimeout(Duration.ofMinutes(3));

    private static final String ANA = "11111111-1111-1111-1111-111111111111";
    private static final String BRUNO = "11111111-1111-1111-1111-111111111112";
    private static final String THOR = "22222222-2222-2222-2222-222222222221";
    private static final String BIDU = "22222222-2222-2222-2222-222222222223";
    private static final String DIEGO = "33333333-3333-3333-3333-333333333332";
    private static final String FABIO_INATIVO = "33333333-3333-3333-3333-333333333334";

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TopicExchange bookingEventsExchange;
    @Autowired
    private ObjectMapper objectMapper;

    private Queue testQueue;

    @BeforeEach
    void bindTemporaryQueue() {
        // fila temporária de teste (não exclusiva, para ser lida por qualquer canal do pool)
        testQueue = new Queue("test.booking-events." + UUID.randomUUID(), false, false, false);
        rabbitAdmin.declareQueue(testQueue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(testQueue).to(bookingEventsExchange).with("booking.*"));
    }

    @AfterEach
    void cleanUp() {
        rabbitAdmin.deleteQueue(testQueue.getName());
        jdbc.update("DELETE FROM bookings");
    }

    @Test
    void migrationsApplySchemaAndDemoSeeds() {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class)).containsExactly("1", "2", "3", "4", "5");
        assertThat(jdbc.queryForList("SELECT id::text FROM tutors ORDER BY id", String.class))
                .containsExactly(ANA, BRUNO);
        assertThat(jdbc.queryForList("SELECT id::text FROM pets ORDER BY id", String.class))
                .containsExactly(THOR, "22222222-2222-2222-2222-222222222222", BIDU);
        assertThat(jdbc.queryForObject("SELECT tutor_id::text FROM pets WHERE id = ?::uuid", String.class, BIDU))
                .isEqualTo(BRUNO);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM caregivers WHERE active", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT active FROM caregivers WHERE id = ?::uuid", Boolean.class,
                FABIO_INATIVO)).isFalse();

        ResponseEntity<List> caregivers = http.getForEntity("/caregivers", List.class);
        assertThat(caregivers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(caregivers.getBody()).hasSize(3);
    }

    @Test
    void createBookingPersistsPriceAndPublishesEventAfterCommit() throws Exception {
        Instant start = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(90, ChronoUnit.MINUTES);

        ResponseEntity<Map> response = http.postForEntity("/bookings", json(Map.of(
                "tutorId", ANA,
                "petId", THOR,
                "caregiverId", DIEGO,
                "serviceType", "WALK",
                "scheduledStart", start.toString(),
                "scheduledEnd", end.toString())), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String bookingId = (String) response.getBody().get("id");
        assertThat(response.getHeaders().getLocation()).hasPath("/bookings/" + bookingId);
        assertThat(response.getBody()).containsEntry("status", "REQUESTED").containsEntry("priceCents", 6000);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, service_type, price_cents, tutor_id::text AS tutor_id FROM bookings WHERE id = ?::uuid",
                bookingId);
        assertThat(row).containsEntry("status", "REQUESTED")
                .containsEntry("service_type", "WALK")
                .containsEntry("price_cents", 6000L)
                .containsEntry("tutor_id", ANA);

        Message message = rabbitTemplate.receive(testQueue.getName(), 10_000);
        assertThat(message).as("BookingCreated na exchange booking.events").isNotNull();
        assertThat(message.getMessageProperties().getReceivedRoutingKey()).isEqualTo("booking.created");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);

        JsonNode event = objectMapper.readTree(message.getBody());
        assertThat(UUID.fromString(event.get("eventId").asText())).isNotNull();
        assertThat(event.get("eventType").asText()).isEqualTo("BookingCreated");
        assertThat(event.get("version").asInt()).isEqualTo(1);
        assertThat(event.get("data").get("bookingId").asText()).isEqualTo(bookingId);
        assertThat(event.get("data").get("status").asText()).isEqualTo("REQUESTED");
        assertThat(event.get("data").get("priceCents").asLong()).isEqualTo(6000);
        assertThat(Instant.parse(event.get("data").get("scheduledStart").asText())).isEqualTo(start);

        // Sobreposição com o mesmo cuidador → 409, sem novo evento
        ResponseEntity<Map> overlap = http.postForEntity("/bookings", json(Map.of(
                "tutorId", BRUNO,
                "petId", BIDU,
                "caregiverId", DIEGO,
                "serviceType", "COMPANIONSHIP",
                "scheduledStart", start.plus(30, ChronoUnit.MINUTES).toString(),
                "scheduledEnd", end.plus(1, ChronoUnit.HOURS).toString())), Map.class);
        assertThat(overlap.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(overlap.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        // Aceite → BookingAccepted
        ResponseEntity<Map> accepted = http.exchange("/bookings/" + bookingId + "/accept", HttpMethod.PATCH,
                HttpEntity.EMPTY, Map.class);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accepted.getBody()).containsEntry("status", "ACCEPTED");

        Message acceptedMessage = rabbitTemplate.receive(testQueue.getName(), 10_000);
        assertThat(acceptedMessage).isNotNull();
        assertThat(acceptedMessage.getMessageProperties().getReceivedRoutingKey()).isEqualTo("booking.accepted");
        assertThat(objectMapper.readTree(acceptedMessage.getBody()).get("eventId").asText())
                .isNotEqualTo(event.get("eventId").asText());

        // Transição inválida → 409
        ResponseEntity<Map> invalid = http.exchange("/bookings/" + bookingId + "/reject", HttpMethod.PATCH,
                HttpEntity.EMPTY, Map.class);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(rabbitTemplate.receive(testQueue.getName(), 1_000)).isNull();
    }

    @Test
    void businessRuleViolationsReturn422WithoutEvents() {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        ResponseEntity<Map> otherTutorsPet = http.postForEntity("/bookings", json(Map.of(
                "tutorId", ANA, "petId", BIDU, "caregiverId", DIEGO, "serviceType", "WALK",
                "scheduledStart", start.toString(), "scheduledEnd", start.plus(1, ChronoUnit.HOURS).toString())),
                Map.class);
        ResponseEntity<Map> inactiveCaregiver = http.postForEntity("/bookings", json(Map.of(
                "tutorId", ANA, "petId", THOR, "caregiverId", FABIO_INATIVO, "serviceType", "WALK",
                "scheduledStart", start.toString(), "scheduledEnd", start.plus(1, ChronoUnit.HOURS).toString())),
                Map.class);

        assertThat(otherTutorsPet.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(inactiveCaregiver.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bookings", Integer.class)).isZero();
        assertThat(rabbitTemplate.receive(testQueue.getName(), 1_000)).isNull();
    }

    @Test
    void actuatorExposesHealthAndPrometheus() {
        ResponseEntity<String> health = http.getForEntity("/actuator/health", String.class);
        ResponseEntity<String> prometheus = http.getForEntity("/actuator/prometheus", String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
        assertThat(prometheus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prometheus.getBody()).contains("jvm_memory_used_bytes");
    }

    private static HttpEntity<Map<String, String>> json(Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
