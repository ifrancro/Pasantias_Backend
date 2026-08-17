package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.auth.ActivarSocioResponse;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.exceptions.ConflictException;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.SocioActivationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocioActivationServiceTest {

    private static final Integer CLUB_ID = 7;
    private static final Integer ANFITRION_ID = 1;
    private static final Integer USUARIO_ID = 42;
    private static final String PAYLOAD = "ACTIVATE:42";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private MembresiaRepository membresiaRepository;
    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private SocioActivationServiceImpl socioActivationService;

    @Test
    void falsePermiteActivacionYPersisteDeclaracion() {
        stubActivacionValidaHastaAntesDeGuardar();
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> {
            Membresia m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId(100);
            }
            return m;
        });
        when(clubRepository.findById(CLUB_ID)).thenReturn(Optional.of(clubStub()));
        when(membresiaRepository.findByNumeroSocio(any())).thenReturn(Optional.empty());
        Rol rolSocio = new Rol();
        rolSocio.setId(3);
        rolSocio.setNombre("SOCIO");
        when(rolRepository.findByNombre("SOCIO")).thenReturn(Optional.of(rolSocio));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivarSocioResponse response = socioActivationService.activarSocio(
                CLUB_ID, ANFITRION_ID, PAYLOAD, null, "Redes", false);

        assertEquals(100, response.getMembresiaId());
        assertEquals(USUARIO_ID, response.getUsuarioId());
        assertNotNull(response.getNumeroSocio());

        ArgumentCaptor<Membresia> captor = ArgumentCaptor.forClass(Membresia.class);
        verify(membresiaRepository, times(2)).save(captor.capture());
        assertEquals(Boolean.FALSE, captor.getAllValues().get(0).getEsClientePreferenteODistribuidor());
        assertEquals(Boolean.FALSE, captor.getAllValues().get(1).getEsClientePreferenteODistribuidor());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void trueRechazaActivacionYNoCreaMembresia() {
        stubActivacionValidaHastaAntesDeGuardar();

        ConflictException ex = assertThrows(ConflictException.class, () ->
                socioActivationService.activarSocio(
                        CLUB_ID, ANFITRION_ID, PAYLOAD, null, "Redes", true));

        assertEquals(
                "Un cliente preferente o distribuidor independiente de Herbalife no puede registrarse como socio",
                ex.getMessage());
        verify(membresiaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
        verify(rolRepository, never()).findByNombre(any());
    }

    @Test
    void nullRechazaActivacionYNoCreaMembresia() {
        stubActivacionValidaHastaAntesDeGuardar();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                socioActivationService.activarSocio(
                        CLUB_ID, ANFITRION_ID, PAYLOAD, null, "Redes", null));

        assertEquals(
                "Debe responder la declaración sobre si usted, su cónyuge o pareja de vida es cliente preferente o distribuidor independiente de Herbalife",
                ex.getMessage());
        verify(membresiaRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
        verify(rolRepository, never()).findByNombre(any());
    }

    private void stubActivacionValidaHastaAntesDeGuardar() {
        when(clubRepository.findByIdAndAnfitrionId(CLUB_ID, ANFITRION_ID))
                .thenReturn(Optional.of(clubStub()));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioBasico()));
        when(membresiaRepository.existsByUsuarioId(USUARIO_ID)).thenReturn(false);
    }

    private static Club clubStub() {
        Club club = new Club();
        club.setId(CLUB_ID);
        club.setNombreClub("Club Test");
        club.setPrefijoSocio("CT");
        Usuario anfitrion = new Usuario();
        anfitrion.setId(ANFITRION_ID);
        club.setAnfitrion(anfitrion);
        return club;
    }

    private static Usuario usuarioBasico() {
        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("USUARIO_BASICO");
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Ana");
        usuario.setApellido("Pérez");
        usuario.setRol(rol);
        return usuario;
    }
}
