package br.pucminas.petamparo.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintIntegrityTest extends DatabaseTestSupport {

    @Test
    void deveImpedirEmailDuplicadoDeTutor() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        insertTutor(first, "Tutor 1", "tutor@petamparo.test");

        assertThrows(SQLException.class,
                () -> insertTutor(second, "Tutor 2", "tutor@petamparo.test"));
    }

    @Test
    void deveImpedirPetComTutorInexistente() {
        assertThrows(SQLException.class, () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO pets (id, tutor_id, name, species) VALUES (?, ?, ?, ?)"
                 )) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, UUID.randomUUID());
                statement.setString(3, "Nina");
                statement.setString(4, "DOG");
                statement.executeUpdate();
            }
        });
    }

    @Test
    void deveImpedirPorteDePetForaDosValoresPermitidos() throws Exception {
        UUID tutorId = UUID.randomUUID();
        insertTutor(tutorId, "Tutor", "size@petamparo.test");

        assertThrows(SQLException.class, () -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO pets (id, tutor_id, name, species, size) VALUES (?, ?, ?, ?, ?)"
                 )) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, tutorId);
                statement.setString(3, "Nina");
                statement.setString(4, "DOG");
                statement.setString(5, "GIANT");
                statement.executeUpdate();
            }
        });
    }

    @Test
    void deveImpedirTipoDeServicoInvalido() throws Exception {
        BookingFixture fixture = createBookingFixture();

        assertThrows(SQLException.class, () ->
                insertBooking(
                        fixture,
                        "UNKNOWN",
                        "REQUESTED",
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(1).plusHours(1)
                )
        );
    }

    @Test
    void deveImpedirStatusInvalido() throws Exception {
        BookingFixture fixture = createBookingFixture();

        assertThrows(SQLException.class, () ->
                insertBooking(
                        fixture,
                        "WALK",
                        "UNKNOWN",
                        OffsetDateTime.now().plusDays(1),
                        OffsetDateTime.now().plusDays(1).plusHours(1)
                )
        );
    }

    @Test
    void deveImpedirFimAnteriorOuIgualAoInicio() throws Exception {
        BookingFixture fixture = createBookingFixture();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        assertThrows(SQLException.class, () ->
                insertBooking(fixture, "WALK", "REQUESTED", start, start.minusMinutes(1))
        );

        assertThrows(SQLException.class, () ->
                insertBooking(fixture, "WALK", "REQUESTED", start, start)
        );
    }

    @Test
    void devePermitirReservaEstruturalmenteValida() throws Exception {
        BookingFixture fixture = createBookingFixture();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);

        assertDoesNotThrow(() ->
                insertBooking(
                        fixture,
                        "HOME_VISIT",
                        "REQUESTED",
                        start,
                        start.plusHours(2)
                )
        );
    }

    private BookingFixture createBookingFixture() throws Exception {
        UUID tutorId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID caregiverId = UUID.randomUUID();

        insertTutor(tutorId, "Tutor", tutorId + "@petamparo.test");

        try (Connection connection = connection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO pets (id, tutor_id, name, species, size) VALUES (?, ?, ?, ?, ?)"
            )) {
                statement.setObject(1, petId);
                statement.setObject(2, tutorId);
                statement.setString(3, "Nina");
                statement.setString(4, "DOG");
                statement.setString(5, "MEDIUM");
                statement.executeUpdate();
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO caregivers (id, name, email) VALUES (?, ?, ?)"
            )) {
                statement.setObject(1, caregiverId);
                statement.setString(2, "Cuidador");
                statement.setString(3, caregiverId + "@petamparo.test");
                statement.executeUpdate();
            }
        }

        return new BookingFixture(tutorId, petId, caregiverId);
    }

    private void insertTutor(UUID id, String name, String email) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tutors (id, name, email) VALUES (?, ?, ?)"
             )) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.setString(3, email);
            statement.executeUpdate();
        }
    }

    private void insertBooking(
            BookingFixture fixture,
            String serviceType,
            String status,
            OffsetDateTime start,
            OffsetDateTime end
    ) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO bookings " +
                     "(id, tutor_id, pet_id, caregiver_id, service_type, status, scheduled_start, scheduled_end) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
             )) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, fixture.tutorId());
            statement.setObject(3, fixture.petId());
            statement.setObject(4, fixture.caregiverId());
            statement.setString(5, serviceType);
            statement.setString(6, status);
            statement.setObject(7, start);
            statement.setObject(8, end);
            statement.executeUpdate();
        }
    }

    private record BookingFixture(UUID tutorId, UUID petId, UUID caregiverId) {}
}
