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
    void v14AddsNullableBooleanOnMembresiasWithoutDefaultFalse() throws Exception {
        Path v14 = MIGRATIONS.resolve("V14__es_cliente_preferente_o_distribuidor_membresias.sql");
        assertTrue(Files.exists(v14));
        String sql = Files.readString(v14, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table membresias"));
        assertTrue(sql.contains("es_cliente_preferente_o_distribuidor"));
        assertTrue(sql.contains("boolean"));
        assertTrue(sql.contains("if not exists"));
        assertFalse(sql.contains("default false"),
                "No usar DEFAULT false: los históricos deben quedar NULL");
        assertFalse(sql.contains("not null"),
                "La columna debe ser nullable para registros históricos");
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void v15CreatesVerificationCodesIdempotently() throws Exception {
        Path v15 = MIGRATIONS.resolve("V15__verification_codes.sql");
        assertTrue(Files.exists(v15), "Falta la migración de verification_codes");
        String sql = Files.readString(v15, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("create table if not exists verification_codes"),
                "Debe ser idempotente: la tabla ya existe en producción");
        assertTrue(sql.contains("usuario_id integer not null references usuarios(id)"));
        assertTrue(sql.contains("code varchar(6) not null"));
        assertTrue(sql.contains("expires_at timestamp not null"));
        assertTrue(sql.contains("used boolean not null default false"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("insert into"));
    }

    @Test
    void v16AddsPurposeFailedAttemptsAndPasswordResetTokens() throws Exception {
        Path v16 = MIGRATIONS.resolve("V16__verification_codes_purpose_and_password_reset.sql");
        assertTrue(Files.exists(v16), "Falta la migración V16 de password reset");
        String sql = Files.readString(v16, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("purpose varchar(32)"));
        assertTrue(sql.contains("email_verification"));
        assertTrue(sql.contains("failed_attempts integer"));
        assertTrue(sql.contains("idx_verification_codes_usuario_purpose_used"));
        assertTrue(sql.contains("idx_verification_codes_usuario_purpose_created"));
        assertTrue(sql.contains("create table if not exists password_reset_tokens"));
        assertTrue(sql.contains("token_hash"));
        assertFalse(sql.contains("insert into"));
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
    void v17AddsNullableRevisionColumnsOnProductos() throws Exception {
        Path v17 = MIGRATIONS.resolve("V17__revision_productos.sql");
        assertTrue(Files.exists(v17), "Falta la migración V17 de revisión de productos");
        String sql = Files.readString(v17, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table productos"));
        assertTrue(sql.contains("comentario_revision"));
        assertTrue(sql.contains("revisado_por_usuario_id"));
        assertTrue(sql.contains("revisado_at"));
        assertTrue(sql.contains("if not exists"));
        assertTrue(sql.contains("references usuarios(id)"));
        assertFalse(sql.contains("comentario_revision text not null"),
                "comentario_revision debe ser nullable");
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void versionsAreUniqueAndIncludeV13AndV14() throws Exception {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<String> names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("V\\d+__.+\\.sql"))
                    .sorted()
                    .toList();
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V13__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V14__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V16__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V17__")));
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
