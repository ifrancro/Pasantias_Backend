package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.usuario.UsuarioDTO;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Transiciones de estado de una cuenta: ACTIVO {@literal <->} INACTIVO y nada mas.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioEstadoServiceTest {

    private static final Integer ADMIN_ID = 1;
    private static final Integer USUARIO_ID = 42;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    // === DESACTIVAR ===

    @Test
    void desactivarUsuarioActivoLoDejaInactivo() {
        Usuario usuario = usuario("ACTIVO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID);

        assertEquals("INACTIVO", dto.getEstado());
        assertEquals("INACTIVO", usuario.getEstado());
    }

    @Test
    void desactivarUsuarioConEstadoNullLoDejaInactivo() {
        // estado null se comporta como ACTIVO en Usuario.isEnabled(); debe poder darse de baja
        Usuario usuario = usuario(null, "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID);

        assertEquals("INACTIVO", dto.getEstado());
    }

    @Test
    void desactivarPendienteDeVerificacionEsConflictoYNoPersiste() {
        // Si se pudiera, el registro saldria del barrido findPendientesVencidos
        // y su email quedaria ocupado para siempre en la columna UNIQUE.
        Usuario usuario = usuario("PENDIENTE_VERIFICACION", "USUARIO_BASICO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID));

        assertTrue(ex.getMessage().contains("verificado"));
        assertEquals("PENDIENTE_VERIFICACION", usuario.getEstado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarUsuarioYaInactivoEsConflicto() {
        Usuario usuario = usuario("INACTIVO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID));

        assertTrue(ex.getMessage().contains("ya"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noSePuedeDesactivarUnaCuentaAdmin() {
        Usuario usuario = usuario("ACTIVO", "ADMIN");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID));

        assertTrue(ex.getMessage().contains("ADMIN"));
        assertEquals("ACTIVO", usuario.getEstado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void unAdminNoPuedeDesactivarseASiMismo() {
        Usuario usuario = usuario("ACTIVO", "ADMIN");
        usuario.setId(ADMIN_ID);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.desactivarUsuario(ADMIN_ID, ADMIN_ID));

        assertTrue(ex.getMessage().contains("propia cuenta"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarUsuarioInexistenteEsNotFound() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.desactivarUsuario(999, ADMIN_ID));
    }

    // === REACTIVAR ===

    @Test
    void reactivarUsuarioInactivoLoDejaActivo() {
        Usuario usuario = usuario("INACTIVO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO dto = usuarioService.reactivarUsuario(USUARIO_ID);

        assertEquals("ACTIVO", dto.getEstado());
        assertEquals("ACTIVO", usuario.getEstado());
    }

    @Test
    void reactivarNoSaltaLaVerificacionDeCorreo() {
        Usuario usuario = usuario("PENDIENTE_VERIFICACION", "USUARIO_BASICO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.reactivarUsuario(USUARIO_ID));

        assertTrue(ex.getMessage().contains("verificar su correo"));
        assertEquals("PENDIENTE_VERIFICACION", usuario.getEstado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reactivarNoLevantaUnBloqueo() {
        Usuario usuario = usuario("BLOQUEADO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.reactivarUsuario(USUARIO_ID));

        assertTrue(ex.getMessage().contains("bloqueada"));
        assertEquals("BLOQUEADO", usuario.getEstado());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reactivarUsuarioYaActivoEsConflicto() {
        Usuario usuario = usuario("ACTIVO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> usuarioService.reactivarUsuario(USUARIO_ID));

        assertTrue(ex.getMessage().contains("ya"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void reactivarUsuarioInexistenteEsNotFound() {
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.reactivarUsuario(999));
    }

    // === CICLO COMPLETO ===

    @Test
    void desactivarYReactivarNoAlteraRolNiDatosDelUsuario() {
        Usuario usuario = usuario("ACTIVO", "SOCIO");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        usuarioService.desactivarUsuario(USUARIO_ID, ADMIN_ID);
        UsuarioDTO dto = usuarioService.reactivarUsuario(USUARIO_ID);

        assertEquals("ACTIVO", dto.getEstado());
        assertEquals("SOCIO", dto.getRolNombre());
        assertEquals("ana@demo.com", dto.getEmail());
    }

    private Usuario usuario(String estado, String rolNombre) {
        Rol rol = new Rol();
        rol.setId(3);
        rol.setNombre(rolNombre);

        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        usuario.setEmail("ana@demo.com");
        usuario.setEstado(estado);
        usuario.setRol(rol);
        return usuario;
    }
}
