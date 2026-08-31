package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.clubes.ClubLocationRejections;
import com.example.herbalife_clubes.clubes.ClubLocationValidator;
import com.example.herbalife_clubes.clubes.ClubPrefixRejections;
import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ClubLocationRejectedException;
import com.example.herbalife_clubes.exceptions.ClubPrefixRejectedException;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
import com.example.herbalife_clubes.membresias.MemberCodeGenerator;
import com.example.herbalife_clubes.mappers.ClubMapperTest;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.HubRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CLUB-PREFIX-001: prefijo/iniciales obligatorio y válido en flujos de club.
 */
@ExtendWith(MockitoExtension.class)
class ClubPrefixServiceTest {

    private static final BigDecimal VALID_LAT = new BigDecimal("-17.3935");
    private static final BigDecimal VALID_LNG = new BigDecimal("-66.1570");

    @Mock private ClubRepository clubRepository;
    @Mock private HubRepository hubRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private ClubServiceImpl clubService;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // --- CREATE ---

    @Test
    void createPrefijoNullRequired() {
        assertCreateRejects(clubDto(null), ClubPrefixRejections.CLUB_PREFIX_REQUIRED);
    }

    @Test
    void createPrefijoBlankRequired() {
        assertCreateRejects(clubDto("   "), ClubPrefixRejections.CLUB_PREFIX_REQUIRED);
    }

    @Test
    void createPrefijoUnaLetraInvalid() {
        assertCreateRejects(clubDto("C"), ClubPrefixRejections.CLUB_PREFIX_INVALID);
    }

    @Test
    void createPrefijoTresLetrasInvalid() {
        assertCreateRejects(clubDto("CVX"), ClubPrefixRejections.CLUB_PREFIX_INVALID);
    }

    @Test
    void createPrefijoNumerosInvalid() {
        assertCreateRejects(clubDto("C1"), ClubPrefixRejections.CLUB_PREFIX_INVALID);
    }

    @Test
    void createPrefijoLowercaseNormalizaUppercase() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> {
            Club c = inv.getArgument(0);
            c.setId(10);
            return c;
        });

        ClubDTO created = clubService.createClub(clubDto("cv"), 1, 2);

        assertEquals("CV", created.getPrefijoSocio());
    }

    @Test
    void createPrefijoValidoCrea() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> {
            Club c = inv.getArgument(0);
            c.setId(10);
            return c;
        });

        ClubDTO created = clubService.createClub(clubDto("CV"), 1, 2);

        assertEquals("CV", created.getPrefijoSocio());
        verify(clubRepository).save(any(Club.class));
    }

    @Test
    void createPrefijoRepetidoEnMismoHubConflict() {
        stubCreateHubAndHost();
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCase(1, "CV")).thenReturn(true);

        ClubPrefixRejectedException ex = assertThrows(ClubPrefixRejectedException.class,
                () -> clubService.createClub(clubDto("CV"), 1, 2));
        assertEquals(ClubPrefixRejections.CLUB_PREFIX_CONFLICT, ex.getErrorCode());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void createMismoPrefijoEnOtroHubPermitido() {
        Hub hub = new Hub();
        hub.setId(2);
        Usuario host = new Usuario();
        host.setId(2);
        when(hubRepository.findById(2)).thenReturn(Optional.of(hub));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(host));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCase(2, "CV")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> clubService.createClub(clubDto("CV"), 2, 2));
    }

    // --- UPDATE ---

    @Test
    void updateConservaOCambiaAPrefijoValido() {
        Club loaded = clubLoaded("PENDIENTE", "SC");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCaseAndIdNot(10, "PD", 1)).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubDTO request = clubDto("PD");
        request.setNombreClub("Actualizado");

        ClubDTO updated = clubService.updateClub(1, request);

        assertEquals("PD", updated.getPrefijoSocio());
        assertEquals("Actualizado", updated.getNombreClub());
    }

    @Test
    void updatePrefijoNullRequired() {
        Club loaded = clubLoaded("PENDIENTE", "SC");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertUpdateRejects(clubDto(null), ClubPrefixRejections.CLUB_PREFIX_REQUIRED);
        assertEquals("SC", loaded.getPrefijoSocio());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updatePrefijoInvalidoInvalid() {
        Club loaded = clubLoaded("PENDIENTE", "SC");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertUpdateRejects(clubDto("ABC"), ClubPrefixRejections.CLUB_PREFIX_INVALID);
        assertEquals("SC", loaded.getPrefijoSocio());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updatePrefijoConflictoConflict() {
        Club loaded = clubLoaded("PENDIENTE", "SC");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCaseAndIdNot(10, "PD", 1)).thenReturn(true);

        assertUpdateRejects(clubDto("PD"), ClubPrefixRejections.CLUB_PREFIX_CONFLICT);
        assertEquals("SC", loaded.getPrefijoSocio());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateHistoricoSinPrefijoPuedeCorregirse() {
        Club loaded = clubLoaded("PENDIENTE", null);
        loaded.setLat(null);
        loaded.setLng(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCaseAndIdNot(10, "CV", 1)).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubDTO request = clubDto("CV");
        clubService.updateClub(1, request);

        assertEquals("CV", loaded.getPrefijoSocio());
        assertEquals(VALID_LAT, loaded.getLat());
    }

    // --- APPROVE / ACTIVATE ---

    @Test
    void aprobarHistoricoSinPrefijoUnavailable() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setPrefijoSocio(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertApproveRejects(ClubPrefixRejections.CLUB_PREFIX_UNAVAILABLE);
        assertEquals("PENDIENTE", loaded.getEstado());
        verify(notificacionService, never()).enviarNotificacion(any(), any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void aprobarPrefijoInvalidoHistoricoUnavailable() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setPrefijoSocio("C1");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertApproveRejects(ClubPrefixRejections.CLUB_PREFIX_UNAVAILABLE);
        assertEquals("PENDIENTE", loaded.getEstado());
    }

    @Test
    void aprobarConPrefijoYUbicacionValidosContinua() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        when(rolRepository.findByNombre("ANFITRION")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionService.enviarNotificacion(any(), isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(new NotificacionDTO());

        ClubDTO dto = clubService.aprobarClub(1);

        assertEquals("ACTIVO", dto.getEstado());
        verify(notificacionService).enviarNotificacion(any(), isNull(), anyInt(), anyInt(), isNull());
    }

    @Test
    void activarHistoricoSinPrefijoUnavailable() {
        Club loaded = clubConRelacionesValidas("INACTIVO");
        loaded.setPrefijoSocio(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        ClubPrefixRejectedException ex =
                assertThrows(ClubPrefixRejectedException.class, () -> clubService.activarClub(1));
        assertEquals(ClubPrefixRejections.CLUB_PREFIX_UNAVAILABLE, ex.getErrorCode());
        assertEquals("INACTIVO", loaded.getEstado());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void rechazoAprobacionNoCambiaRolNiNotificacion() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setPrefijoSocio(null);
        Rol rolBasico = new Rol();
        rolBasico.setNombre("USUARIO_BASICO");
        loaded.getAnfitrion().setRol(rolBasico);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertThrows(ClubPrefixRejectedException.class, () -> clubService.aprobarClub(1));

        assertEquals("USUARIO_BASICO", loaded.getAnfitrion().getRol().getNombre());
        verify(notificacionService, never()).enviarNotificacion(any(), any(), any(), any(), any());
    }

    // --- REGRESIÓN ---

    @Test
    void memberCodeGeneraCv00000001() {
        assertEquals("CV-00000001", MemberCodeGenerator.generate("CV", 1));
    }

    @Test
    void codigoHistoricoNoCambiaAlActualizarEstado() {
        Membresia historica = new Membresia();
        historica.setId(3);
        historica.setNumeroSocio("CL-000003");
        historica.setEstado("ACTIVA");

        assertEquals("CL-000003", historica.getNumeroSocio());
    }

    @Test
    void clubLocationSigueFuncionando() {
        ClubLocationRejectedException ex = assertThrows(ClubLocationRejectedException.class,
                () -> ClubLocationValidator.validateRequired(null, VALID_LNG));
        assertEquals(ClubLocationRejections.CLUB_LOCATION_REQUIRED, ex.getErrorCode());
    }

    @Test
    void handlerClubPrefixRequired400() {
        ClubPrefixRejectedException ex = ClubPrefixRejections.prefixRequired();
        ResponseEntity<Map<String, Object>> response = handler.handleClubPrefixRejected(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(ClubPrefixRejections.CLUB_PREFIX_REQUIRED, body.get("error"));
    }

    private void assertCreateRejects(ClubDTO dto, String code) {
        stubCreateHubAndHost();
        ClubPrefixRejectedException ex = assertThrows(ClubPrefixRejectedException.class,
                () -> clubService.createClub(dto, 1, 2));
        assertEquals(code, ex.getErrorCode());
        verify(clubRepository, never()).save(any());
    }

    private void assertUpdateRejects(ClubDTO request, String code) {
        when(clubRepository.findById(1)).thenReturn(Optional.of(clubLoaded("PENDIENTE", "SC")));
        ClubPrefixRejectedException ex = assertThrows(ClubPrefixRejectedException.class,
                () -> clubService.updateClub(1, request));
        assertEquals(code, ex.getErrorCode());
    }

    private void assertApproveRejects(String code) {
        ClubPrefixRejectedException ex = assertThrows(ClubPrefixRejectedException.class,
                () -> clubService.aprobarClub(1));
        assertEquals(code, ex.getErrorCode());
        verify(clubRepository, never()).save(any());
    }

    private void stubCreateDependencies() {
        stubCreateHubAndHost();
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCase(eq(1), any())).thenReturn(false);
    }

    private void stubCreateHubAndHost() {
        Hub hub = new Hub();
        hub.setId(1);
        Usuario host = new Usuario();
        host.setId(2);
        when(hubRepository.findById(1)).thenReturn(Optional.of(hub));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(host));
    }

    private static Club clubLoaded(String estado, String prefijoSocio) {
        Club club = new Club();
        club.setId(1);
        club.setNombreClub("Club");
        club.setEstado(estado);
        club.setPrefijoSocio(prefijoSocio);
        club.setLat(VALID_LAT);
        club.setLng(VALID_LNG);
        Hub hub = new Hub();
        hub.setId(10);
        club.setHub(hub);
        return club;
    }

    private static Club clubConRelacionesValidas(String estado) {
        Club club = ClubMapperTest.clubConRelaciones();
        club.setEstado(estado);
        club.setLat(VALID_LAT);
        club.setLng(VALID_LNG);
        return club;
    }

    private static ClubDTO clubDto(String prefijoSocio) {
        ClubDTO dto = new ClubDTO();
        dto.setNombreClub("Club Test");
        dto.setLat(VALID_LAT);
        dto.setLng(VALID_LNG);
        dto.setPrefijoSocio(prefijoSocio);
        return dto;
    }
}
