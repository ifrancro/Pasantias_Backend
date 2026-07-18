package com.example.herbalife_clubes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke liviano sin BD. El arranque completo con Flyway+validate se cubre en
 * {@code FlywayPostgresIT} (Testcontainers) y en staging/Render tras baseline.
 */
class HerbalifeClubesApplicationTests {

    @Test
    void flywayStarterIsOnTestClasspath() {
        assertTrue(
                classPresent("org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
                        || classPresent("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"),
                "spring-boot-starter-flyway debe estar en el classpath");
        assertTrue(
                classPresent("org.flywaydb.core.Flyway"),
                "Flyway core debe estar en el classpath");
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
