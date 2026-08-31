package com.example.herbalife_clubes.membresias;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MEMBER-CODE-001: generación de códigos de socio legibles.
 */
class MemberCodeGeneratorTest {

    @Test
    void cvId1GeneraCv00000001() {
        assertEquals("CV-00000001", MemberCodeGenerator.generate("CV", 1));
    }

    @Test
    void cvId123GeneraCv00000123() {
        assertEquals("CV-00000123", MemberCodeGenerator.generate("CV", 123));
    }

    @Test
    void idMasDe8DigitosNoSeTrunca() {
        assertEquals("CV-123456789", MemberCodeGenerator.generate("CV", 123456789));
    }

    @Test
    void prefijoLowercaseSeNormaliza() {
        assertEquals("CV-00000123", MemberCodeGenerator.generate("cv", 123));
    }

    @Test
    void prefijoNullRechaza() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MemberCodeGenerator.generate(null, 1));
        assertTrue(ex.getMessage().contains("prefijo de socio"));
    }

    @Test
    void prefijoInvalidoRechaza() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MemberCodeGenerator.generate("C1", 1));
        assertTrue(ex.getMessage().contains("2 letras"));
    }

    @Test
    void idNullRechaza() {
        assertThrows(IllegalArgumentException.class, () -> MemberCodeGenerator.generate("CV", null));
    }

    @Test
    void idCeroRechaza() {
        assertThrows(IllegalArgumentException.class, () -> MemberCodeGenerator.generate("CV", 0));
    }

    @Test
    void bordesLat90Lng180Validos() {
        assertEquals("AB-00000090", MemberCodeGenerator.generate("AB", 90));
        assertEquals("XY-00000180", MemberCodeGenerator.generate("XY", 180));
    }
}
