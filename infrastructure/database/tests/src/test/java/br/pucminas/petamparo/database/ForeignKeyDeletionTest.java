package br.pucminas.petamparo.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ForeignKeyDeletionTest extends DatabaseTestSupport {

    @Test
    void deveImpedirExclusaoDeTutorQuePossuiPet() throws Exception {
        UUID tutorId = UUID.randomUUID();

        try (Connection connection = connection()) {
            try (PreparedStatement tutor = connection.prepareStatement(
                    "INSERT INTO tutors (id, name, email) VALUES (?, ?, ?)"
            )) {
                tutor.setObject(1, tutorId);
                tutor.setString(2, "Tutor");
                tutor.setString(3, "delete@petamparo.test");
                tutor.executeUpdate();
            }

            try (PreparedStatement pet = connection.prepareStatement(
                    "INSERT INTO pets (id, tutor_id, name, species) VALUES (?, ?, ?, ?)"
            )) {
                pet.setObject(1, UUID.randomUUID());
                pet.setObject(2, tutorId);
                pet.setString(3, "Nina");
                pet.setString(4, "DOG");
                pet.executeUpdate();
            }
        }

        assertThrows(SQLException.class, () -> {
            try (Connection connection = connection();
                 PreparedStatement delete = connection.prepareStatement(
                         "DELETE FROM tutors WHERE id = ?"
                 )) {
                delete.setObject(1, tutorId);
                delete.executeUpdate();
            }
        });
    }
}
