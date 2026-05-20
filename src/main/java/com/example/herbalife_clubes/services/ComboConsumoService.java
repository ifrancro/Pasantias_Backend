package com.example.herbalife_clubes.services;

import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;

public interface ComboConsumoService {

    EstadoComboDTO obtenerEstadoCombo(Integer membresiaId);

    void validarComboConsumidoAntesDeAsistencia(Integer membresiaId);
}
