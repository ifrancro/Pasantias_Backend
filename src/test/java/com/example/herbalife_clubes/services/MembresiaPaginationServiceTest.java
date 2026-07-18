package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;
import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;
import com.example.herbalife_clubes.entities.Club;
import com.example.herbalife_clubes.entities.Membresia;
import com.example.herbalife_clubes.entities.Usuario;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.serviceimpls.MembresiaServiceImpl;
import com.example.herbalife_clubes.services.ComboConsumoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembresiaPaginationServiceTest {

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private ComboConsumoService comboConsumoService;

    @InjectMocks
    private MembresiaServiceImpl membresiaService;

    @Test
    void clubPageBatchLoadsOnceAndPreservesOrder() {
        Page<Integer> idPage = new PageImpl<>(List.of(3, 1), PageRequest.of(0, 20), 2);
        when(membresiaRepository.findIdsByClubId(eq(7), eq(""), any(Pageable.class)))
                .thenReturn(idPage);
        when(membresiaRepository.findWithUsuarioClubByIds(List.of(3, 1)))
                .thenReturn(List.of(stubMembresia(1), stubMembresia(3)));
        when(comboConsumoService.obtenerEstadoCombo(anyInt()))
                .thenReturn(EstadoComboDTO.builder().haConsumidoCombo(false).totalCombosConsumidos(0).build());

        PagedResponse<MembresiaDTO> result =
                membresiaService.getMembresiasByClubPaginadas(7, 0, 20, "  ");

        assertEquals(2, result.content().size());
        assertEquals(3, result.content().get(0).getId());
        assertEquals(1, result.content().get(1).getId());
        verify(membresiaRepository, times(1)).findWithUsuarioClubByIds(anyList());
    }

    @Test
    void emptySearchUsesActiveIdsQuery() {
        when(membresiaRepository.findIdsAllActive(any(Pageable.class)))
                .thenReturn(Page.empty());

        PagedResponse<MembresiaDTO> result =
                membresiaService.buscarMiembrosGlobalPaginado(null, 0, 20);

        assertTrue(result.content().isEmpty());
        verify(membresiaRepository).findIdsAllActive(any(Pageable.class));
        verify(membresiaRepository, never()).buscarIdsGlobal(anyString(), any());
    }

    @Test
    void queryUsesSearchIds() {
        when(membresiaRepository.buscarIdsGlobal(eq("ana"), any(Pageable.class)))
                .thenReturn(Page.empty());

        membresiaService.buscarMiembrosGlobalPaginado(" ana ", 0, 20);

        verify(membresiaRepository).buscarIdsGlobal(eq("ana"), any(Pageable.class));
    }

    private static Membresia stubMembresia(int id) {
        Membresia m = new Membresia();
        m.setId(id);
        m.setNumeroSocio("SOC-" + id);
        m.setEstado("ACTIVA");
        Usuario u = new Usuario();
        u.setId(100 + id);
        u.setNombre("Nombre" + id);
        u.setApellido("Apellido");
        m.setUsuario(u);
        Club c = new Club();
        c.setId(7);
        c.setNombreClub("Club Test");
        m.setClub(c);
        return m;
    }
}
