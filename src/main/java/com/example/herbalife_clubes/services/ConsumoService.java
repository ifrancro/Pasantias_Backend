package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.consumo.ConsumoDTO;

import java.util.List;

public interface ConsumoService {
    ConsumoDTO registrarConsumo(Integer membresiaId, Integer clubId, Integer productoId, 
                                 String descripcion, Integer cantidad, java.math.BigDecimal precioReferencial,
                                 Integer asistenciaId);
    List<ConsumoDTO> getHistorialBySocio(Integer membresiaId);
    List<ConsumoDTO> getHistorialByClub(Integer clubId);
    List<ConsumoDTO> getHistorialByAsistencia(Integer asistenciaId);
}

