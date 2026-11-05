package br.pucminas.petamparo.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaMigrationTest extends DatabaseTestSupport {

    @Test
    void deveAplicarTodasAsMigrationsComSucesso() throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT version, success FROM flyway_schema_history " +
                     "WHERE type = 'SQL' ORDER BY installed_rank"
             )) {

            int count = 0;
            String lastVersion = null;

            while (result.next()) {
                count++;
                assertTrue(result.getBoolean("success"));
                lastVersion = result.getString("version");
            }

            // O esperado vem dos arquivos versionados: novas migrations não quebram este teste.
            List<String> versions = migrationVersions();
            assertEquals(versions.size(), count);
            assertEquals(versions.get(versions.size() - 1), lastVersion);
        }
    }

    private static List<String> migrationVersions() throws IOException {
        Path dir = Path.of(System.getProperty("petamparo.migrations").trim().replaceFirst("^filesystem:", ""));
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(f -> f.getFileName().toString())
                    .filter(name -> name.matches("V\\d+__.+\\.sql"))
                    .map(name -> name.substring(1, name.indexOf("__")))
                    .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b)))
                    .toList();
        }
    }

    @Test
    void deveCriarAsTabelasDoDominioInicial() throws Exception {
        Set<String> tables = new HashSet<>();

        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT table_name FROM information_schema.tables " +
                     "WHERE table_schema = 'public'"
             )) {

            while (result.next()) {
                tables.add(result.getString("table_name"));
            }
        }

        assertTrue(tables.contains("tutors"));
        assertTrue(tables.contains("pets"));
        assertTrue(tables.contains("caregivers"));
        assertTrue(tables.contains("bookings"));
        assertTrue(tables.contains("flyway_schema_history"));
    }
}
