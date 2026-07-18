package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.common.PagedResponse;
import com.example.herbalife_clubes.dtos.membresia.ArbolReferidosDTO;
import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;
import com.example.herbalife_clubes.dtos.membresia.MembresiaDTO;

import java.util.List;

public interface MembresiaService {
    MembresiaDTO createMembresia(MembresiaDTO membresiaDTO, Integer usuarioId, Integer clubId, Integer nivelId, Integer referidoPorMembresiaId);
    MembresiaDTO getMembresia(Integer membresiaId);
    MembresiaDTO getMembresiaByUsuario(Integer usuarioId);
    List<MembresiaDTO> getMembresiasByClub(Integer clubId);
    MembresiaDTO cambiarEstado(Integer membresiaId, String estado);
    MembresiaDTO cambiarNivel(Integer membresiaId, Integer nivelId);
    MembresiaDTO actualizarPuntos(Integer membresiaId, Integer puntos);
    MembresiaDTO recalcularPuntosPorAsistencias(Integer membresiaId);
    ArbolReferidosDTO getArbolReferidos(Integer membresiaId);

    EstadoComboDTO getEstadoCombo(Integer membresiaId);

    /** Legacy: page/size aplicados, respuesta sin metadata. */
    List<MembresiaDTO> buscarMiembrosGlobal(String query, int page, int size);

    PagedResponse<MembresiaDTO> getMembresiasByClubPaginadas(Integer clubId, int page, int size, String q);

    PagedResponse<MembresiaDTO> buscarMiembrosGlobalPaginado(String query, int page, int size);
}

