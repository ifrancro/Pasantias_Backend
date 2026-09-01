package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.AuthenticationResponse;
import com.example.herbalife_clubes.dtos.auth.GoogleAuthRequest;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.GoogleEmailNotVerifiedException;
import com.example.herbalife_clubes.exceptions.GoogleTokenInvalidException;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.security.JwtService;
import com.example.herbalife_clubes.serviceimpls.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleAuthTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private VerificationService verificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void googleLegacyCasingUsuarioExistenteReutilizaSinDuplicar() {
        Usuario legacy = Usuario.builder()
                .id(26)
                .email("Evis96568@gmail.com")
                .nombre("Eva")
                .apellido("Tradicional")
                .passwordHash("hash-original")
                .telefono("70000001")
                .estado("ACTIVO")
                .rol(rolBasico())
                .build();
        when(usuarioRepository.findByEmailIgnoreCase("evis96568@gmail.com"))
                .thenReturn(Optional.of(legacy));
        when(jwtService.generateToken(legacy)).thenReturn("jwt-legacy");

        AuthenticationResponse response = authService.completeGoogleAuthentication(
                "EVIS96568@GMAIL.COM", "Otro", "Nombre", true);

        assertEquals("jwt-legacy", response.getToken());
        assertEquals(26, response.getUserId());
        assertEquals("Evis96568@gmail.com", response.getEmail());
        verify(usuarioRepository).findByEmailIgnoreCase("evis96568@gmail.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void googleEmailMixedCaseUsuarioExistenteAutenticaSinDuplicar() {
        Usuario existente = usuarioExistente(26, "ACTIVO", "hash-original", "70000001");
        when(usuarioRepository.findByEmailIgnoreCase("evis96568@gmail.com"))
                .thenReturn(Optional.of(existente));
        when(jwtService.generateToken(existente)).thenReturn("jwt-mixed");

        AuthenticationResponse response = authService.completeGoogleAuthentication(
                "Evis96568@Gmail.com", "Otro", "Nombre", true);

        assertEquals("jwt-mixed", response.getToken());
        assertEquals(26, response.getUserId());
        verify(usuarioRepository).findByEmailIgnoreCase("evis96568@gmail.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void googleEmailMixedCaseUsuarioNuevoPersisteNormalizado() {
        when(usuarioRepository.findByEmailIgnoreCase("nuevo.google@gmail.com")).thenReturn(Optional.empty());
        Rol rol = rolBasico();
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode(any())).thenReturn("random-hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(101);
            return u;
        });
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-nuevo-mixed");

        authService.completeGoogleAuthentication(
                "Nuevo.Google@Gmail.com", "Nuevo", "Google", true);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("nuevo.google@gmail.com", captor.getValue().getEmail());
        verify(usuarioRepository).findByEmailIgnoreCase("nuevo.google@gmail.com");
    }

    @Test
    void googleUsuarioInexistenteCreaActivoYEmiteJwt() {
        when(usuarioRepository.findByEmailIgnoreCase("nuevo@gmail.com")).thenReturn(Optional.empty());
        Rol rol = rolBasico();
        when(rolRepository.findByNombre("USUARIO_BASICO")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode(any())).thenReturn("random-hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(100);
            return u;
        });
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-nuevo");

        AuthenticationResponse response = authService.completeGoogleAuthentication(
                "nuevo@gmail.com", "Eva", "Test", true);

        assertEquals("jwt-nuevo", response.getToken());
        assertEquals(100, response.getUserId());
        assertFalse(response.isRequiresVerification());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("ACTIVO", captor.getValue().getEstado());
        assertEquals("USUARIO_BASICO", captor.getValue().getRol().getNombre());
        verify(verificationService, never()).invalidateCodes(any());
    }

    @Test
    void googleUsuarioActivoLoginConservaDatos() {
        Usuario existente = usuarioExistente(26, "ACTIVO", "hash-original", "70000001");
        when(usuarioRepository.findByEmailIgnoreCase("evis96568@gmail.com"))
                .thenReturn(Optional.of(existente));
        when(jwtService.generateToken(existente)).thenReturn("jwt-activo");

        AuthenticationResponse response = authService.completeGoogleAuthentication(
                "evis96568@gmail.com", "Otro", "Nombre", true);

        assertEquals("jwt-activo", response.getToken());
        assertEquals(26, response.getUserId());
        assertEquals("Eva", response.getNombre()); // no sobrescribe
        assertFalse(response.isRequiresVerification());
        verify(usuarioRepository, never()).save(any());
        verify(verificationService, never()).invalidateCodes(any());
        assertEquals("hash-original", existente.getPasswordHash());
        assertEquals("70000001", existente.getTelefono());
        assertEquals("ACTIVO", existente.getEstado());
    }

    @Test
    void googleUsuarioPendienteActivaMismoIdConservaDatosEInvalidaOtp() {
        Usuario pendiente = usuarioExistente(26, "PENDIENTE_VERIFICACION", "hash-reg", "+59170000000");
        when(usuarioRepository.findByEmailIgnoreCase("evis96568@gmail.com"))
                .thenReturn(Optional.of(pendiente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(Usuario.class))).thenReturn("jwt-activado");

        AuthenticationResponse response = authService.completeGoogleAuthentication(
                "evis96568@gmail.com", "GoogleNombre", "GoogleApellido", true);

        assertEquals("jwt-activado", response.getToken());
        assertEquals(26, response.getUserId());
        assertEquals("Eva", response.getNombre());
        assertEquals("Tradicional", response.getApellido());
        assertEquals("USUARIO_BASICO", response.getRolNombre());
        assertFalse(response.isRequiresVerification());

        verify(verificationService).invalidateCodes(pendiente);
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals(26, guardado.getId());
        assertEquals("ACTIVO", guardado.getEstado());
        assertEquals("hash-reg", guardado.getPasswordHash());
        assertEquals("+59170000000", guardado.getTelefono());
        assertEquals("USUARIO_BASICO", guardado.getRol().getNombre());
    }

    @Test
    void googleUsuarioInactivoNoReactiva() {
        Usuario inactivo = usuarioExistente(30, "INACTIVO", "hash", null);
        when(usuarioRepository.findByEmailIgnoreCase("off@gmail.com")).thenReturn(Optional.of(inactivo));

        assertThrows(DisabledException.class, () ->
                authService.completeGoogleAuthentication("off@gmail.com", "A", "B", true));

        verify(usuarioRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
        verify(verificationService, never()).invalidateCodes(any());
        assertEquals("INACTIVO", inactivo.getEstado());
    }

    @Test
    void googleUsuarioBloqueadoNoReactiva() {
        Usuario bloqueado = usuarioExistente(31, "BLOQUEADO", "hash", null);
        when(usuarioRepository.findByEmailIgnoreCase("blocked@gmail.com")).thenReturn(Optional.of(bloqueado));

        assertThrows(DisabledException.class, () ->
                authService.completeGoogleAuthentication("blocked@gmail.com", "A", "B", true));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void googleEmailVerifiedFalseRechazado() {
        GoogleEmailNotVerifiedException ex = assertThrows(GoogleEmailNotVerifiedException.class, () ->
                authService.completeGoogleAuthentication("x@gmail.com", "A", "B", false));

        assertEquals(GoogleEmailNotVerifiedException.DEFAULT_MESSAGE, ex.getMessage());
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void googleEmailVerifiedNullRechazado() {
        assertThrows(GoogleEmailNotVerifiedException.class, () ->
                authService.completeGoogleAuthentication("x@gmail.com", "A", "B", null));
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    @Test
    void authenticateWithGoogleTokenInvalidoNoEs500() throws Exception {
        AuthServiceImpl spy = spy(authService);
        doReturn(null).when(spy).verifyGoogleIdToken(anyString());

        GoogleTokenInvalidException ex = assertThrows(GoogleTokenInvalidException.class, () ->
                spy.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken("token-invalido")
                        .build()));

        assertEquals(GoogleTokenInvalidException.DEFAULT_MESSAGE, ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("eyj"));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticateWithGoogleTokenExpiradoOAudIncorrectoMapeaATokenInvalid() throws Exception {
        // verifier.verify() → null cubre expirado / audience incorrecto / firma inválida.
        AuthServiceImpl spy = spy(authService);
        doReturn(null).when(spy).verifyGoogleIdToken("expired-or-bad-aud");

        assertThrows(GoogleTokenInvalidException.class, () ->
                spy.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken("expired-or-bad-aud")
                        .build()));
    }

    @Test
    void authenticateWithGoogleGeneralSecurityExceptionMapeaATokenInvalid() throws Exception {
        AuthServiceImpl spy = spy(authService);
        doThrow(new GeneralSecurityException("signature invalid")).when(spy)
                .verifyGoogleIdToken(anyString());

        GoogleTokenInvalidException ex = assertThrows(GoogleTokenInvalidException.class, () ->
                spy.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken("signed-bad")
                        .build()));

        assertEquals(GoogleTokenInvalidException.DEFAULT_MESSAGE, ex.getMessage());
        assertFalse(ex.getMessage().contains("signature"));
    }

    @Test
    void authenticateWithGoogleIdTokenVacioLanzaTokenInvalid() {
        assertThrows(GoogleTokenInvalidException.class, () ->
                authService.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken("   ")
                        .build()));
    }

    @Test
    void authenticateWithGoogleErrorInternoNoEnvuelveMensajeDeLibreria() throws Exception {
        AuthServiceImpl spy = spy(authService);
        doThrow(new IllegalStateException("detalle-interno-secreto")).when(spy)
                .verifyGoogleIdToken(anyString());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                spy.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken("tok")
                        .build()));

        // Se propaga sin RuntimeException envolvente con mensaje de Google/JWT.
        assertEquals("detalle-interno-secreto", ex.getMessage());
    }

    @Test
    void authenticateWithGoogleNuncaPropagaElIdTokenEnExcepcionControlada() throws Exception {
        String secretToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.secret-payload";
        AuthServiceImpl spy = spy(authService);
        doThrow(new IOException("network")).when(spy).verifyGoogleIdToken(secretToken);

        GoogleTokenInvalidException ex = assertThrows(GoogleTokenInvalidException.class, () ->
                spy.authenticateWithGoogle(GoogleAuthRequest.builder()
                        .idToken(secretToken)
                        .build()));

        assertFalse(ex.getMessage().contains(secretToken));
        assertFalse(ex.getMessage().contains("eyJ"));
    }

    @Test
    void googleEmailAusenteEnPayloadEsTokenInvalid() {
        assertThrows(GoogleTokenInvalidException.class, () ->
                authService.completeGoogleAuthentication("  ", "A", "B", true));
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    private static Rol rolBasico() {
        Rol rol = new Rol();
        rol.setId(4);
        rol.setNombre("USUARIO_BASICO");
        return rol;
    }

    private static Usuario usuarioExistente(Integer id, String estado, String passwordHash, String telefono) {
        return Usuario.builder()
                .id(id)
                .email(id == 26 ? "evis96568@gmail.com" : (id == 30 ? "off@gmail.com" : "blocked@gmail.com"))
                .nombre("Eva")
                .apellido("Tradicional")
                .passwordHash(passwordHash)
                .telefono(telefono)
                .estado(estado)
                .rol(rolBasico())
                .build();
    }
}
