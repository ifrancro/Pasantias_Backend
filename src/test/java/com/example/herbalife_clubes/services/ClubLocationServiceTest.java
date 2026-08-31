package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.asistencias.AttendanceLocationRejections;
import com.example.herbalife_clubes.asistencias.AttendanceLocationValidator;
import com.example.herbalife_clubes.clubes.ClubLocationRejections;
import com.example.herbalife_clubes.clubes.ClubLocationValidator;
import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.AttendanceLocationRejectedException;
import com.example.herbalife_clubes.exceptions.ClubLocationRejectedException;
import com.example.herbalife_clubes.exceptions.GlobalExceptionHandler;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CLUB-LOCATION-001: ubicación obligatoria y válida en flujos de club.
 */
@ExtendWith(MockitoExtension.class)
class ClubLocationServiceTest {

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
    void createConCoordenadasValidasCrea() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> {
            Club c = inv.getArgument(0);
            c.setId(10);
            return c;
        });

        ClubDTO created = clubService.createClub(clubDto(VALID_LAT, VALID_LNG), 1, 2);

        assertNotNull(created.getId());
        verify(clubRepository).save(any(Club.class));
    }

    @Test
    void createLatNullRequired() {
        assertCreateRejects(clubDto(null, VALID_LNG), ClubLocationRejections.CLUB_LOCATION_REQUIRED);
    }

    @Test
    void createLngNullRequired() {
        assertCreateRejects(clubDto(VALID_LAT, null), ClubLocationRejections.CLUB_LOCATION_REQUIRED);
    }

    @Test
    void createAmbasNullRequired() {
        assertCreateRejects(clubDto(null, null), ClubLocationRejections.CLUB_LOCATION_REQUIRED);
    }

    @Test
    void createLatMayor90Invalid() {
        assertCreateRejects(clubDto(bd("91"), VALID_LNG), ClubLocationRejections.CLUB_LOCATION_INVALID);
    }

    @Test
    void createLatMenorMenos90Invalid() {
        assertCreateRejects(clubDto(bd("-90.0001"), VALID_LNG), ClubLocationRejections.CLUB_LOCATION_INVALID);
    }

    @Test
    void createLngMayor180Invalid() {
        assertCreateRejects(clubDto(VALID_LAT, bd("180.0001")), ClubLocationRejections.CLUB_LOCATION_INVALID);
    }

    @Test
    void createLngMenorMenos180Invalid() {
        assertCreateRejects(clubDto(VALID_LAT, bd("-180.0001")), ClubLocationRejections.CLUB_LOCATION_INVALID);
    }

    @Test
    void createBordeLat90Valido() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));
        assertDoesNotThrow(() -> clubService.createClub(clubDto(bd("90"), VALID_LNG), 1, 2));
    }

    @Test
    void createBordeLng180Valido() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));
        assertDoesNotThrow(() -> clubService.createClub(clubDto(VALID_LAT, bd("180")), 1, 2));
    }

    @Test
    void createCeroCeroValido() {
        stubCreateDependencies();
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));
        assertDoesNotThrow(() -> clubService.createClub(clubDto(BigDecimal.ZERO, BigDecimal.ZERO), 1, 2));
    }

    @Test
    void createRechazoNoPersisteClub() {
        stubCreateDependencies();
        assertThrows(ClubLocationRejectedException.class,
                () -> clubService.createClub(clubDto(null, VALID_LNG), 1, 2));
        verify(clubRepository, never()).save(any());
    }

    // --- UPDATE ---

    @Test
    void updateConCoordenadasValidasOk() {
        Club loaded = clubLoaded("PENDIENTE", VALID_LAT, VALID_LNG);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCaseAndIdNot(anyInt(), any(), anyInt()))
                .thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubDTO request = clubDto(bd("-17.4"), bd("-66.2"));
        request.setNombreClub("Actualizado");

        ClubDTO updated = clubService.updateClub(1, request);

        assertEquals("Actualizado", updated.getNombreClub());
        assertEquals(0, bd("-17.4").compareTo(loaded.getLat()));
    }

    @Test
    void updateLatNullRequired() {
        Club loaded = clubLoaded("PENDIENTE", VALID_LAT, VALID_LNG);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        ClubDTO request = clubDto(null, VALID_LNG);
        assertUpdateRejects(request, ClubLocationRejections.CLUB_LOCATION_REQUIRED);
        assertEquals(VALID_LAT, loaded.getLat());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateLngNullRequired() {
        Club loaded = clubLoaded("PENDIENTE", VALID_LAT, VALID_LNG);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertUpdateRejects(clubDto(VALID_LAT, null), ClubLocationRejections.CLUB_LOCATION_REQUIRED);
        assertEquals(VALID_LNG, loaded.getLng());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateFueraDeRangoInvalid() {
        Club loaded = clubLoaded("PENDIENTE", VALID_LAT, VALID_LNG);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertUpdateRejects(clubDto(bd("95"), VALID_LNG), ClubLocationRejections.CLUB_LOCATION_INVALID);
        assertEquals(VALID_LAT, loaded.getLat());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateHistoricoSinUbicacionPuedeCorregirse() {
        Club loaded = clubLoaded("PENDIENTE", null, null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCaseAndIdNot(anyInt(), any(), anyInt()))
                .thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubDTO request = clubDto(VALID_LAT, VALID_LNG);
        clubService.updateClub(1, request);

        assertEquals(VALID_LAT, loaded.getLat());
        assertEquals(VALID_LNG, loaded.getLng());
    }

    // --- APPROVE ---

    @Test
    void aprobarConUbicacionValidaAprueba() {
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
    void aprobarLatNullUnavailable() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setLat(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertApproveRejects(ClubLocationRejections.CLUB_LOCATION_UNAVAILABLE);
        assertEquals("PENDIENTE", loaded.getEstado());
        verify(notificacionService, never()).enviarNotificacion(any(), any(), any(), any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void aprobarLngNullUnavailable() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setLng(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertApproveRejects(ClubLocationRejections.CLUB_LOCATION_UNAVAILABLE);
        assertEquals("PENDIENTE", loaded.getEstado());
    }

    @Test
    void aprobarRechazoConservaRolAnfitrion() {
        Club loaded = clubConRelacionesValidas("PENDIENTE");
        loaded.setLat(null);
        Rol rolBasico = new Rol();
        rolBasico.setNombre("USUARIO_BASICO");
        loaded.getAnfitrion().setRol(rolBasico);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertThrows(ClubLocationRejectedException.class, () -> clubService.aprobarClub(1));

        assertEquals("USUARIO_BASICO", loaded.getAnfitrion().getRol().getNombre());
    }

    // --- ACTIVATE ---

    @Test
    void activarConUbicacionValidaActiva() {
        Club loaded = clubConRelacionesValidas("INACTIVO");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubDTO dto = clubService.activarClub(1);

        assertEquals("ACTIVO", dto.getEstado());
    }

    @Test
    void activarUbicacionFaltanteUnavailable() {
        Club loaded = clubConRelacionesValidas("INACTIVO");
        loaded.setLng(null);
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        ClubLocationRejectedException ex =
                assertThrows(ClubLocationRejectedException.class, () -> clubService.activarClub(1));
        assertEquals(ClubLocationRejections.CLUB_LOCATION_UNAVAILABLE, ex.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
        assertEquals("INACTIVO", loaded.getEstado());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void activarUbicacionInvalidaHistoricaUnavailable() {
        Club loaded = clubConRelacionesValidas("INACTIVO");
        loaded.setLat(bd("999"));
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        assertThrows(ClubLocationRejectedException.class, () -> clubService.activarClub(1));
        assertEquals("INACTIVO", loaded.getEstado());
    }

    // --- GET / REGRESSION MOB-ATT-002 ---

    @Test
    void clubValidoDisponibleEnGet() {
        Club loaded = clubConRelacionesValidas("ACTIVO");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));

        ClubDTO dto = clubService.getClub(1);

        assertNotNull(dto);
        assertEquals(VALID_LAT, dto.getLat());
        assertEquals(VALID_LNG, dto.getLng());
    }

    @Test
    void attendanceValidatorAceptaClubConCoordsValidas() {
        Club club = clubLoaded("ACTIVO", VALID_LAT, VALID_LNG);
        assertDoesNotThrow(() -> AttendanceLocationValidator.validateClubCoordinates(club));
    }

    @Test
    void attendanceValidatorRechazaHistoricoSinCoords() {
        Club club = clubLoaded("ACTIVO", null, null);
        AttendanceLocationRejectedException ex = assertThrows(
                AttendanceLocationRejectedException.class,
                () -> AttendanceLocationValidator.validateClubCoordinates(club));
        assertEquals(AttendanceLocationRejections.ATTENDANCE_CLUB_LOCATION_UNAVAILABLE, ex.getErrorCode());
    }

    @Test
    void handlerClubLocationRequired400() {
        ClubLocationRejectedException ex = ClubLocationRejections.locationRequired();
        ResponseEntity<Map<String, Object>> response = handler.handleClubLocationRejected(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals(ClubLocationRejections.CLUB_LOCATION_REQUIRED, body.get("error"));
    }

    @Test
    void validatorLat90Lng180BordesValidos() {
        assertTrue(ClubLocationValidator.isValid(bd("90"), bd("180")));
        assertTrue(ClubLocationValidator.isValid(bd("-90"), bd("-180")));
    }

    private void assertCreateRejects(ClubDTO dto, String code) {
        stubCreateDependencies();
        ClubLocationRejectedException ex = assertThrows(ClubLocationRejectedException.class,
                () -> clubService.createClub(dto, 1, 2));
        assertEquals(code, ex.getErrorCode());
        verify(clubRepository, never()).save(any());
    }

    private void assertUpdateRejects(ClubDTO request, String code) {
        ClubLocationRejectedException ex = assertThrows(ClubLocationRejectedException.class,
                () -> clubService.updateClub(1, request));
        assertEquals(code, ex.getErrorCode());
    }

    private void assertApproveRejects(String code) {
        ClubLocationRejectedException ex = assertThrows(ClubLocationRejectedException.class,
                () -> clubService.aprobarClub(1));
        assertEquals(code, ex.getErrorCode());
        verify(clubRepository, never()).save(any());
    }

    private void stubCreateDependencies() {
        Hub hub = new Hub();
        hub.setId(1);
        Usuario host = new Usuario();
        host.setId(2);
        when(hubRepository.findById(1)).thenReturn(Optional.of(hub));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(host));
        lenient().when(clubRepository.existsByHubIdAndPrefijoSocioIgnoreCase(anyInt(), any())).thenReturn(false);
    }

    private static Club clubLoaded(String estado, BigDecimal lat, BigDecimal lng) {
        Club club = new Club();
        club.setId(1);
        club.setNombreClub("Club");
        club.setEstado(estado);
        club.setLat(lat);
        club.setLng(lng);
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

    private static ClubDTO clubDto(BigDecimal lat, BigDecimal lng) {
        ClubDTO dto = new ClubDTO();
        dto.setNombreClub("Club Test");
        dto.setLat(lat);
        dto.setLng(lng);
        dto.setPrefijoSocio("CV");
        return dto;
    }

    private static ClubDTO clubDto(BigDecimal lat, BigDecimal lng, String prefijoSocio) {
        ClubDTO dto = clubDto(lat, lng);
        dto.setPrefijoSocio(prefijoSocio);
        return dto;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
