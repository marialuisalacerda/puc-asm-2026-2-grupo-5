package br.pucminas.petamparo.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

abstract class DatabaseTestSupport {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("petamparo_test")
                    .withUsername("petamparo")
                    .withPassword("petamparo");

    static {
        POSTGRES.start();

        String migrations = System.getProperty("petamparo.migrations");
        if (migrations == null || migrations.isBlank()) {
            throw new IllegalStateException(
                    "Propriedade petamparo.migrations não configurada. Execute os testes via Maven."
            );
        }

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(migrations.trim())
                .load()
                .migrate();
    }

    @BeforeEach
    void cleanBusinessTables() throws SQLException {
        try (Connection connection = connection()) {
            connection.createStatement().execute(
                    "TRUNCATE TABLE bookings, pets, caregivers, tutors CASCADE"
            );
        }
    }

    protected Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }
}
