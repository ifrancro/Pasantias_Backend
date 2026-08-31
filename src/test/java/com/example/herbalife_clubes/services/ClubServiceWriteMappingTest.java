package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.dtos.notificacion.NotificacionDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Rol;
import com.example.herbalife_clubes.mappers.ClubMapperTest;
import com.example.herbalife_clubes.repositories.ClubRepository;
import com.example.herbalife_clubes.repositories.RolRepository;
import com.example.herbalife_clubes.repositories.UsuarioRepository;
import com.example.herbalife_clubes.serviceimpls.ClubServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceWriteMappingTest {

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ClubServiceImpl clubService;

    @Test
    void aprobarClubMapeaFindByIdYNoElRetornoDeSave() {
        Club loaded = ClubMapperTest.clubConRelaciones();
        loaded.setEstado("PENDIENTE");
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.save(any(Club.class))).thenReturn(clubSinRelaciones());
        Rol rol = new Rol();
        rol.setNombre("ANFITRION");
        when(rolRepository.findByNombre("ANFITRION")).thenReturn(Optional.of(rol));
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(notificacionService.enviarNotificacion(any(), isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(new NotificacionDTO());

        ClubDTO dto = assertDoesNotThrow(() -> clubService.aprobarClub(1));

        assertEquals("ACTIVO", dto.getEstado());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
        verify(notificacionService).enviarNotificacion(any(), isNull(), anyInt(), anyInt(), isNull());
    }

    @Test
    void rechazarClubMapeaFindByIdYNoElRetornoDeSave() {
        Club loaded = ClubMapperTest.clubConRelaciones();
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.save(any(Club.class))).thenReturn(clubSinRelaciones());
        when(notificacionService.enviarNotificacion(any(), isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(new NotificacionDTO());

        ClubDTO dto = clubService.rechazarClub(1);

        assertEquals("RECHAZADO", dto.getEstado());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(20, dto.getAnfitrionId());
    }

    @Test
    void activarYDesactivarMapeanFindById() {
        when(clubRepository.findById(1)).thenReturn(Optional.of(ClubMapperTest.clubConRelaciones()));
        when(clubRepository.save(any(Club.class))).thenReturn(clubSinRelaciones());

        ClubDTO activo = clubService.activarClub(1);
        assertEquals("ACTIVO", activo.getEstado());
        assertEquals("HUB Santa Cruz", activo.getHubNombre());

        when(clubRepository.findById(1)).thenReturn(Optional.of(ClubMapperTest.clubConRelaciones()));
        ClubDTO inactivo = clubService.desactivarClub(1);
        assertEquals("INACTIVO", inactivo.getEstado());
        assertEquals("Andrea Anfitriona", inactivo.getAnfitrionNombre());
    }

    @Test
    void updateClubMapeaFindByIdYNoElRetornoDeSave() {
        Club loaded = ClubMapperTest.clubConRelaciones();
        when(clubRepository.findById(1)).thenReturn(Optional.of(loaded));
        when(clubRepository.save(any(Club.class))).thenReturn(clubSinRelaciones());

        ClubDTO request = new ClubDTO();
        request.setNombreClub("Nuevo nombre");
        request.setPrefijoSocio("SC");
        request.setLat(new BigDecimal("-17.3935"));
        request.setLng(new BigDecimal("-66.1570"));

        ClubDTO dto = clubService.updateClub(1, request);

        assertEquals("Nuevo nombre", dto.getNombreClub());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
    }

    private static Club clubSinRelaciones() {
        Club club = new Club();
        club.setId(99);
        club.setNombreClub("si se mapea save() este DTO sale mal");
        return club;
    }
}
