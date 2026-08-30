package com.example.herbalife_clubes.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integración real PostgreSQL vía Testcontainers.
 * Simula adopción: esquema existente (sin historial) → baseline 12 → V13+V14.
 * Requiere Docker. Sin Docker, la clase se omite.
 */
@Tag("postgres")
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
class FlywayPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("flyway_it")
                    .withUsername("test")
                    .withPassword("test");

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @BeforeAll
    static void bootstrapExistingSchemaWithoutFlywayHistory() throws Exception {
        assertTrue(POSTGRES.isRunning());
        try (Connection c = open()) {
            ScriptUtils.executeSqlScript(
                    c, new FileSystemResource("src/test/resources/db/bootstrap/recrear_bd_completa.sql"));
            ScriptUtils.executeSqlScript(
                    c, new FileSystemResource("src/test/resources/db/bootstrap/pre_flyway_patch.sql"));
        }
    }

    @Test
    void baselineThenV13AndV14KeepDataAndAreIdempotent() throws Exception {
        // Datos previos a Flyway (simula BD en producción antes de adoptar)
        try (Connection c = open(); Statement st = c.createStatement()) {
            st.executeUpdate("INSERT INTO roles(nombre) VALUES ('ADMIN')");
            st.executeUpdate("""
                    INSERT INTO usuarios(rol_id, nombre, apellido, email, password_hash, estado)
                    SELECT id, 'Test', 'User', 'flyway-it@example.com', 'x', 'ACTIVO'
                    FROM roles WHERE nombre='ADMIN' LIMIT 1
                    """);
            st.executeUpdate("""
                    INSERT INTO hubs(admin_id, nombre, estado)
                    SELECT id, 'Hub IT', 'ACTIVO' FROM usuarios WHERE email='flyway-it@example.com'
                    """);
            st.executeUpdate("""
                    INSERT INTO clubes(hub_id, anfitrion_id, nombre_club, estado)
                    SELECT h.id, u.id, 'Club IT', 'ACTIVO'
                    FROM hubs h
                    JOIN usuarios u ON u.email='flyway-it@example.com'
                    LIMIT 1
                    """);
            st.executeUpdate("""
                    INSERT INTO membresias(usuario_id, club_id, numero_socio, estado, puntos_acumulados, fecha_registro)
                    SELECT u.id, c.id, 'IT-001', 'ACTIVA', 0, NOW()
                    FROM usuarios u
                    JOIN clubes c ON c.nombre_club='Club IT'
                    WHERE u.email='flyway-it@example.com'
                    LIMIT 1
                    """);
        }

        Flyway flyway = configuredFlyway();
        MigrateResult first = flyway.migrate();
        assertTrue(first.success);
        assertTrue(first.migrationsExecuted >= 1, "Debe aplicar al menos V13 tras baseline");

        assertTrue(indexExists("idx_pedidos_club_fecha_id"));
        assertTrue(indexExists("idx_pedidos_membresia_fecha_id"));
        assertTrue(indexExists("idx_membresias_club_fecha_id"));
        assertTrue(flywayHistoryHasVersion("13"));
        assertTrue(flywayHistoryHasVersion("18"));
        assertTrue(flywayHistoryHasVersion("19"));
        assertTrue(columnExists("club_productos", "precio_venta"));

        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM membresias WHERE numero_socio='IT-001'")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "V13/V14 no deben borrar datos previos");
        }

        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT es_cliente_preferente_o_distribuidor FROM membresias WHERE numero_socio='IT-001'")) {
            assertTrue(rs.next());
            assertNull(rs.getObject(1), "Registros históricos deben quedar NULL, no false");
        }

        MigrateResult second = flyway.migrate();
        assertTrue(second.success);
        assertEquals(0, second.migrationsExecuted, "migrate repetido no reaplica V13/V14");

        flyway.validate();
    }

    @Test
    void cleanDisabledInApplicationProperties() throws Exception {
        String props = Files.readString(
                Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);
        assertTrue(props.contains("spring.flyway.clean-disabled=true"));
        assertFalse(props.matches("(?s).*spring\\.jpa\\.hibernate\\.ddl-auto\\s*=\\s*update.*"));
    }

    private static Flyway configuredFlyway() {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("12")
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
    }

    private static Connection open() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static boolean indexExists(String indexName) throws Exception {
        try (Connection c = open();
             var ps = c.prepareStatement(
                     "SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname=?")) {
            ps.setString(1, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(String tableName, String columnName) throws Exception {
        try (Connection c = open();
             var ps = c.prepareStatement(
                     "SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?")) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean flywayHistoryHasVersion(String version) throws Exception {
        try (Connection c = open();
             var ps = c.prepareStatement(
                     "SELECT 1 FROM flyway_schema_history WHERE version=? AND success=true")) {
            ps.setString(1, version);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
