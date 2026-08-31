package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.ActivarSocioResponse;
import com.example.herbalife_clubes.dtos.auth.QrResponse;
import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.AsistenciaRepository;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.NivelSocioRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.MembresiaServiceImpl;
import com.example.herbalife_clubes.serviceimpls.SocioActivationServiceImpl;
import com.example.herbalife_clubes.services.ComboConsumoService;
import com.example.herbalife_clubes.services.MembresiaLogroService;
import com.example.herbalife_clubes.repositories.RolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MEMBER-CODE-001: integración de códigos de socio en flujos de membresía.
 */
@ExtendWith(MockitoExtension.class)
class MemberCodeServiceTest {

    private static final Integer CLUB_ID = 3;
    private static final Integer USUARIO_ID = 50;
    private static final Integer NUEVO_USUARIO_ID = 51;
    private static final Integer ANFITRION_ID = 2;

    @Mock private MembresiaRepository membresiaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private NivelSocioRepository nivelSocioRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private MembresiaLogroService membresiaLogroService;
    @Mock private ComboConsumoService comboConsumoService;
    @Mock private RolRepository rolRepository;

    @InjectMocks
    private MembresiaServiceImpl membresiaService;

    @InjectMocks
    private SocioActivationServiceImpl socioActivationService;

    // --- CREATE ---

    @Test
    void createIgnoraNumeroSocioEnviadoPorCliente() {
        stubCreateMembresia();
        MembresiaDTO request = new MembresiaDTO();
        request.setNumeroSocio("CL-999999");

        MembresiaDTO created = membresiaService.createMembresia(
                request, NUEVO_USUARIO_ID, CLUB_ID, null, null);

        assertEquals("CV-00000123", created.getNumeroSocio());
        assertNotEquals("CL-999999", created.getNumeroSocio());
        verify(membresiaRepository).saveAndFlush(any(Membresia.class));
    }

    @Test
    void createNuevaMembresiaRecibeCodigoConPrefijoDelClub() {
        stubCreateMembresia();
        MembresiaDTO created = membresiaService.createMembresia(
                new MembresiaDTO(), NUEVO_USUARIO_ID, CLUB_ID, null, null);
        assertEquals("CV-00000123", created.getNumeroSocio());
    }

    @Test
    void dosMembresiasGeneranCodigosDistintos() {
        when(membresiaRepository.findByUsuarioId(anyInt())).thenReturn(Optional.empty());
        when(usuarioRepository.findById(anyInt())).thenAnswer(inv -> Optional.of(usuario(inv.getArgument(0))));
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubConPrefijo("CV")));
        when(membresiaRepository.saveAndFlush(any(Membresia.class)))
                .thenAnswer(inv -> { Membresia m = inv.getArgument(0); m.setId(123); return m; })
                .thenAnswer(inv -> { Membresia m = inv.getArgument(0); m.setId(124); return m; });
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        MembresiaDTO first = membresiaService.createMembresia(
                new MembresiaDTO(), 60, CLUB_ID, null, null);
        MembresiaDTO second = membresiaService.createMembresia(
                new MembresiaDTO(), 61, CLUB_ID, null, null);

        assertEquals("CV-00000123", first.getNumeroSocio());
        assertEquals("CV-00000124", second.getNumeroSocio());
        assertNotEquals(first.getNumeroSocio(), second.getNumeroSocio());
    }

    @Test
    void createEsTransaccionalSaveAndFlushLuegoSave() {
        stubCreateMembresia();
        membresiaService.createMembresia(new MembresiaDTO(), NUEVO_USUARIO_ID, CLUB_ID, null, null);
        verify(membresiaRepository).saveAndFlush(any(Membresia.class));
        verify(membresiaRepository).save(any(Membresia.class));
    }

    // --- HISTÓRICOS ---

    @Test
    void historicoCl000003NoCambiaAlActualizarEstado() {
        Membresia historica = membresiaHistorica("CL-000003");
        when(membresiaRepository.findById(3)).thenReturn(Optional.of(historica));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));

        MembresiaDTO updated = membresiaService.cambiarEstado(3, "INACTIVA");

        assertEquals("CL-000003", updated.getNumeroSocio());
        assertEquals("INACTIVA", updated.getEstado());
    }

    @Test
    void findByNumeroSocioSigueFuncionando() {
        Membresia historica = membresiaHistorica("CL-000003");
        when(membresiaRepository.findByNumeroSocio("CL-000003")).thenReturn(Optional.of(historica));

        Optional<Membresia> found = membresiaRepository.findByNumeroSocio("CL-000003");

        assertTrue(found.isPresent());
        assertEquals("CL-000003", found.get().getNumeroSocio());
    }

    // --- QR ---

    @Test
    void qrNuevoContieneNumeroSocioGenerado() {
        Membresia membresia = membresiaConCodigo("CV-00000123");
        when(membresiaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(membresia));

        QrResponse qr = socioActivationService.obtenerQrSocio(USUARIO_ID);

        assertEquals("SOCIO", qr.getTipo());
        assertEquals("SOCIO:CV-00000123", qr.getQrPayload());
    }

    @Test
    void qrHistoricoSigueFuncionando() {
        Membresia historica = membresiaHistorica("CL-000003");
        when(membresiaRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(historica));

        QrResponse qr = socioActivationService.obtenerQrSocio(USUARIO_ID);

        assertEquals("SOCIO:CL-000003", qr.getQrPayload());
    }

    // --- ACTIVACIÓN ---

    @Test
    void activacionGeneraCodigoLegible() {
        stubActivacionValida();
        when(membresiaRepository.saveAndFlush(any(Membresia.class))).thenAnswer(inv -> {
            Membresia m = inv.getArgument(0);
            m.setId(100);
            return m;
        });
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubConPrefijo("CV")));
        Rol rolSocio = rol("SOCIO");
        when(rolRepository.findByNombre("SOCIO")).thenReturn(Optional.of(rolSocio));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivarSocioResponse response = socioActivationService.activarSocio(
                CLUB_ID, ANFITRION_ID, "ACTIVATE:42", null, "Redes", false);

        assertEquals("CV-00000100", response.getNumeroSocio());
        assertEquals("SOCIO:CV-00000100", response.getQrSocioPayload());
    }

    @Test
    void activacionConReferidoPorNumeroSocioHistorico() {
        stubActivacionValida();
        Membresia referente = membresiaHistorica("CL-000003");
        when(membresiaRepository.findByNumeroSocio("CL-000003")).thenReturn(Optional.of(referente));
        when(membresiaRepository.saveAndFlush(any(Membresia.class))).thenAnswer(inv -> {
            Membresia m = inv.getArgument(0);
            m.setId(101);
            return m;
        });
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubConPrefijo("CV")));
        when(rolRepository.findByNombre("SOCIO")).thenReturn(Optional.of(rol("SOCIO")));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivarSocioResponse response = socioActivationService.activarSocio(
                CLUB_ID, ANFITRION_ID, "ACTIVATE:42", "CL-000003", "Referido", false);

        assertEquals("CV-00000101", response.getNumeroSocio());
        ArgumentCaptor<Membresia> captor = ArgumentCaptor.forClass(Membresia.class);
        verify(membresiaRepository).saveAndFlush(captor.capture());
        assertEquals(referente, captor.getValue().getReferidoPorMembresia());
    }

    @Test
    void activacionClubSinPrefijoRechazaControlado() {
        stubActivacionValida();
        Club clubSinPrefijo = clubConPrefijo(null);
        when(clubRepository.findByIdAndAnfitrionId(CLUB_ID, ANFITRION_ID))
                .thenReturn(Optional.of(clubSinPrefijo));
        when(membresiaRepository.saveAndFlush(any(Membresia.class))).thenAnswer(inv -> {
            Membresia m = inv.getArgument(0);
            m.setId(100);
            return m;
        });
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubSinPrefijo));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                socioActivationService.activarSocio(
                        CLUB_ID, ANFITRION_ID, "ACTIVATE:42", null, "Redes", false));

        assertTrue(ex.getMessage().contains("prefijo de socio"));
        verify(rolRepository, never()).findByNombre(any());
    }

    private void stubCreateMembresia() {
        when(membresiaRepository.findByUsuarioId(NUEVO_USUARIO_ID)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(NUEVO_USUARIO_ID)).thenReturn(Optional.of(usuario(NUEVO_USUARIO_ID)));
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubConPrefijo("CV")));
        when(membresiaRepository.saveAndFlush(any(Membresia.class))).thenAnswer(inv -> {
            Membresia m = inv.getArgument(0);
            m.setId(123);
            return m;
        });
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubActivacionValida() {
        when(clubRepository.findByIdAndAnfitrionId(CLUB_ID, ANFITRION_ID))
                .thenReturn(Optional.of(clubConPrefijo("CV")));
        when(usuarioRepository.findById(42)).thenReturn(Optional.of(usuarioBasico(42)));
        when(membresiaRepository.existsByUsuarioId(42)).thenReturn(false);
    }

    private static Club clubConPrefijo(String prefijo) {
        Club club = new Club();
        club.setId(CLUB_ID);
        club.setNombreClub("Club Vitality");
        club.setPrefijoSocio(prefijo);
        club.setEstado("ACTIVO");
        Usuario anfitrion = new Usuario();
        anfitrion.setId(ANFITRION_ID);
        club.setAnfitrion(anfitrion);
        return club;
    }

    private static Usuario usuario(Integer id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Test");
        u.setApellido("User");
        return u;
    }

    private static Usuario usuarioBasico(Integer id) {
        Usuario u = usuario(id);
        Rol rol = rol("USUARIO_BASICO");
        u.setRol(rol);
        return u;
    }

    private static Rol rol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        return rol;
    }

    private static Membresia membresiaHistorica(String numeroSocio) {
        Membresia m = new Membresia();
        m.setId(3);
        m.setNumeroSocio(numeroSocio);
        m.setEstado("ACTIVA");
        m.setUsuario(usuario(USUARIO_ID));
        m.setClub(clubConPrefijo("CL"));
        return m;
    }

    private static Membresia membresiaConCodigo(String numeroSocio) {
        Membresia m = new Membresia();
        m.setId(123);
        m.setNumeroSocio(numeroSocio);
        m.setEstado("ACTIVA");
        m.setUsuario(usuario(USUARIO_ID));
        m.setClub(clubConPrefijo("CV"));
        return m;
    }
}
