package com.example.herbalife_clubes.serviceimpls;

import com.example.herbalife_clubes.dtos.membresia.EstadoComboDTO;
import com.example.herbalife_clubes.exceptions.ComboRequiredException;
import com.example.herbalife_clubes.exceptions.ResourceNotFoundException;
import com.example.herbalife_clubes.entities.EstadoPedido;
import com.example.herbalife_clubes.repositories.MembresiaRepository;
import com.example.herbalife_clubes.repositories.PedidoItemRepository;
import com.example.herbalife_clubes.services.ComboConsumoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ComboConsumoServiceImpl implements ComboConsumoService {

    public static final String TIPO_PRODUCTO_COMBO = "COMBO";
    private static final String MENSAJE_COMBO_REQUERIDO =
            "El socio no ha consumido ningún Combo antes de registrar asistencia.";

    private final PedidoItemRepository pedidoItemRepository;
    private final MembresiaRepository membresiaRepository;

    @Override
    @Transactional(readOnly = true)
    public EstadoComboDTO obtenerEstadoCombo(Integer membresiaId) {
        if (!membresiaRepository.existsById(membresiaId)) {
            throw new ResourceNotFoundException("Membresía no encontrada con id: " + membresiaId);
        }
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);
        long total = pedidoItemRepository.countCombosConsumidosEntregadosHoy(
                membresiaId, EstadoPedido.ENTREGADO, inicioDia, finDia);
        return EstadoComboDTO.builder()
                .haConsumidoCombo(total > 0)
                .totalCombosConsumidos(total)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validarComboConsumidoAntesDeAsistencia(Integer membresiaId) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia = inicioDia.plusDays(1);
        if (!pedidoItemRepository.hasConsumedComboEntregadoHoy(
                membresiaId, EstadoPedido.ENTREGADO, inicioDia, finDia)) {
            throw new ComboRequiredException(MENSAJE_COMBO_REQUERIDO);
        }
    }
}
