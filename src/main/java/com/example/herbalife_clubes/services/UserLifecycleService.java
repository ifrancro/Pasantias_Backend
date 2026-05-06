package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.asistencia.AsistenciaDTO;
import com.example.herbalife_clubes.dtos.membresia.*;
import com.example.herbalife_clubes.dtos.prospecto.*;

import java.util.List;

public interface UserLifecycleService {
    ProspectoDTO crearProspecto(Integer clubId, ProspectoCreateRequest request);
    List<ProspectoDTO> listarProspectos(Integer clubId);
    ProspectoDTO actualizarEstadoProspecto(Integer prospectoId, ProspectoEstadoUpdateRequest request);

    MisionProspectoDTO crearMision(Integer prospectoId, MisionProspectoCreateRequest request);
    MisionProspectoDTO incrementarProgresoMision(Integer misionId);
    void eliminarMision(Integer misionId);

    List<CompraManualDTO> listarCompras(Integer membresiaId);
    CompraManualDTO crearCompra(Integer membresiaId, CompraManualCreateRequest request);

    List<ReferidoSocioDTO> listarReferidos(Integer membresiaId);

    AsistenciaDTO registrarAsistenciaManual(Integer membresiaId, AsistenciaManualCreateRequest request);
}
