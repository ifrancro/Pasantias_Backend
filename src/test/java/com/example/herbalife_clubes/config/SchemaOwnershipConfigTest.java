package com.example.herbalife_clubes.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Garantiza ownership de esquema: Flyway migra, Hibernate solo valida.
 */
class SchemaOwnershipConfigTest {

    @Test
    void productionDefaultsPreferValidateAndFlyway() throws Exception {
        Properties props = loadMainApplicationProperties();

        assertEquals("validate", props.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", props.getProperty("spring.flyway.enabled"));
        assertEquals("classpath:db/migration", props.getProperty("spring.flyway.locations"));
        assertEquals("true", props.getProperty("spring.flyway.validate-on-migrate"));
        assertEquals("true", props.getProperty("spring.flyway.clean-disabled"));

        String baseline = props.getProperty("spring.flyway.baseline-on-migrate");
        assertNotNull(baseline);
        assertTrue(
                baseline.contains("FLYWAY_BASELINE_ON_MIGRATE"),
                "baseline-on-migrate debe venir de env y default false");
        assertFalse("true".equalsIgnoreCase(baseline));
    }

    @Test
    void productionDoesNotUseCreateOrUpdateDdl() throws Exception {
        Properties props = loadMainApplicationProperties();
        String ddl = props.getProperty("spring.jpa.hibernate.ddl-auto");
        assertNotEquals("update", ddl);
        assertNotEquals("create", ddl);
        assertNotEquals("create-drop", ddl);
    }

    private static Properties loadMainApplicationProperties() throws Exception {
        var path = Path.of("src/main/resources/application.properties");
        assertTrue(Files.exists(path), "Falta src/main/resources/application.properties");
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }
}
