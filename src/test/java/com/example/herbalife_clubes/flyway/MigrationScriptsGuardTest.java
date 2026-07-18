package com.example.herbalife_clubes.flyway;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MigrationScriptsGuardTest {

    private static final Path MIGRATIONS =
            Path.of("src/main/resources/db/migration");

    @Test
    void v13ExistsAndCreatesExpectedIndexes() throws Exception {
        Path v13 = MIGRATIONS.resolve("V13__indices_paginacion_pedidos_membresias.sql");
        assertTrue(Files.exists(v13));
        String sql = Files.readString(v13, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("idx_pedidos_club_fecha_id"));
        assertTrue(sql.contains("idx_pedidos_membresia_fecha_id"));
        assertTrue(sql.contains("idx_membresias_club_fecha_id"));
        assertTrue(sql.contains("if not exists"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void migrationsDoNotContainSecrets() throws Exception {
        assertTrue(Files.isDirectory(MIGRATIONS));
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<Path> sqlFiles = files.filter(p -> p.toString().endsWith(".sql")).toList();
            assertFalse(sqlFiles.isEmpty());
            for (Path file : sqlFiles) {
                String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                assertFalse(content.contains("password="), file + " no debe contener password=");
                assertFalse(content.contains("jwt"), file + " no debe contener jwt");
                assertFalse(content.contains("api_key"), file + " no debe contener api_key");
                assertFalse(content.contains("begin rsa private"), file + " no debe contener claves privadas");
            }
        }
    }

    @Test
    void versionsAreUniqueAndIncludeV13() throws Exception {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<String> names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("V\\d+__.+\\.sql"))
                    .sorted()
                    .toList();
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V13__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V2__")));
            assertFalse(names.stream().anyMatch(n -> n.startsWith("V1__")),
                    "No existe V1 histórico; no inventar una sin estrategia");
            long distinct = names.stream().map(n -> n.replaceFirst("V(\\d+)__.*", "$1")).distinct().count();
            assertEquals(names.size(), distinct, "No debe haber versiones Flyway duplicadas");
        }
    }

    @Test
    void flywayDependencyPresentInPom() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("spring-boot-starter-flyway"));
        assertTrue(pom.contains("flyway-database-postgresql"));
        assertTrue(pom.contains("testcontainers"));
    }
}
