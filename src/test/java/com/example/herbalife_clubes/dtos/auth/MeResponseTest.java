package com.example.herbalife_clubes.dtos.auth;

import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MeResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @ParameterizedTest
    @CsvSource({
            "SOCIO, SOCIO",
            "ANFITRION, ANFITRION",
            "USUARIO_BASICO, USUARIO_BASICO"
    })
    void fromMapeaCamposPublicosYRolNombre(String rolBd, String rolEsperado) throws Exception {
        Usuario usuario = usuarioConRol(2, rolBd);
        usuario.setNombre("Andrea");
        usuario.setApellido("Anfitriona");
        usuario.setEmail("andrea@test.com");
        usuario.setTelefono("+59170000000");
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        usuario.setRedesSociales("@andrea");
        usuario.setEstado("ACTIVO");
        usuario.setPasswordHash("$2a$secret-hash");

        MeResponse dto = MeResponse.from(usuario);

        assertEquals(2, dto.getUserId());
        assertEquals("Andrea", dto.getNombre());
        assertEquals("Anfitriona", dto.getApellido());
        assertEquals("andrea@test.com", dto.getEmail());
        assertEquals("+59170000000", dto.getTelefono());
        assertEquals(LocalDate.of(1990, 5, 1), dto.getFechaNacimiento());
        assertEquals("@andrea", dto.getRedesSociales());
        assertEquals(rolEsperado, dto.getRolNombre());
        assertEquals("ACTIVO", dto.getEstado());

        String json = objectMapper.writeValueAsString(dto);
        assertJsonIsPublicContract(json);
        assertTrue(json.contains("\"rolNombre\":\"" + rolEsperado + "\""));
        assertTrue(json.contains("\"userId\":2"));
    }

    @Test
    void serializacionNoIncluyeCamposSensiblesNiUserDetails() throws Exception {
        Usuario usuario = usuarioConRol(7, "ANFITRION");
        usuario.setPasswordHash("hash-secreto");
        usuario.setNombre("A");
        usuario.setApellido("B");
        usuario.setEmail("a@b.com");
        usuario.setEstado("ACTIVO");

        String json = objectMapper.writeValueAsString(MeResponse.from(usuario));

        assertJsonIsPublicContract(json);
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("authorities"));
        assertFalse(json.contains("accountNonExpired"));
        assertFalse(json.contains("accountNonLocked"));
        assertFalse(json.contains("credentialsNonExpired"));
        assertFalse(json.contains("enabled"));
        assertFalse(json.contains("username"));
        assertFalse(json.contains("\"rol\":"));
        assertFalse(json.contains("createdAt"));
        assertFalse(json.contains("hash-secreto"));
    }

    @Test
    void fromConRolNullDejaRolNombreNull() {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail("x@y.com");
        usuario.setRol(null);

        MeResponse dto = MeResponse.from(usuario);

        assertNull(dto.getRolNombre());
        assertEquals(1, dto.getUserId());
    }

    private static void assertJsonIsPublicContract(String json) {
        assertTrue(json.contains("userId"));
        assertTrue(json.contains("nombre"));
        assertTrue(json.contains("apellido"));
        assertTrue(json.contains("email"));
        assertTrue(json.contains("rolNombre"));
        assertTrue(json.contains("estado"));
    }

    private static Usuario usuarioConRol(int id, String rolNombre) {
        Rol rol = new Rol();
        rol.setId(99);
        rol.setNombre(rolNombre);

        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }
}
