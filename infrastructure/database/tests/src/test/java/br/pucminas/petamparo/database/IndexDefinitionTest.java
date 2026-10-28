package br.pucminas.petamparo.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexDefinitionTest extends DatabaseTestSupport {

    @Test
    void deveCriarIndicesDasChavesEstrangeiras() throws Exception {
        Set<String> indexes = new HashSet<>();

        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT indexname FROM pg_indexes WHERE schemaname = 'public'"
             )) {

            while (result.next()) {
                indexes.add(result.getString("indexname"));
            }
        }

        assertTrue(indexes.contains("idx_pets_tutor_id"));
        assertTrue(indexes.contains("idx_bookings_tutor_id"));
        assertTrue(indexes.contains("idx_bookings_pet_id"));
        assertTrue(indexes.contains("idx_bookings_caregiver_id"));
    }
}
