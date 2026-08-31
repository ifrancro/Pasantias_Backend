package com.example.herbalife_clubes.mappers;

import com.example.herbalife_clubes.dtos.club.ClubDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Hub;
import com.example.herbalife_clubes.entities.Usuario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ClubMapperTest {

    @Test
    void mapClubToClubDTOExponeHubYAnfitrion() {
        ClubDTO dto = ClubMapper.mapClubToClubDTO(clubConRelaciones());

        assertEquals(1, dto.getId());
        assertEquals(10, dto.getHubId());
        assertEquals("HUB Santa Cruz", dto.getHubNombre());
        assertEquals(20, dto.getAnfitrionId());
        assertEquals("Andrea Anfitriona", dto.getAnfitrionNombre());
        assertEquals("Club Demo", dto.getNombreClub());
        assertEquals("ACTIVO", dto.getEstado());
        assertEquals("SC", dto.getPrefijoSocio());
    }

    @Test
    void mapClubToClubDTOToleraHubYAnfitrionNull() {
        Club club = new Club();
        club.setId(1);
        club.setNombreClub("Sin relaciones");
        club.setHub(null);
        club.setAnfitrion(null);

        ClubDTO dto = ClubMapper.mapClubToClubDTO(club);

        assertEquals(1, dto.getId());
        assertNull(dto.getHubId());
        assertNull(dto.getHubNombre());
        assertNull(dto.getAnfitrionId());
        assertNull(dto.getAnfitrionNombre());
        assertEquals("Sin relaciones", dto.getNombreClub());
    }

    public static Club clubConRelaciones() {
        Hub hub = new Hub();
        hub.setId(10);
        hub.setNombre("HUB Santa Cruz");

        Usuario anfitrion = new Usuario();
        anfitrion.setId(20);
        anfitrion.setNombre("Andrea");
        anfitrion.setApellido("Anfitriona");

        Club club = new Club();
        club.setId(1);
        club.setNombreClub("Club Demo");
        club.setDireccion("Av. Ejemplo");
        club.setHorario("Lun-Sab");
        club.setEstado("ACTIVO");
        club.setPrefijoSocio("SC");
        club.setLat(new BigDecimal("-17.3935"));
        club.setLng(new BigDecimal("-66.1570"));
        club.setHub(hub);
        club.setAnfitrion(anfitrion);
        return club;
    }
}
