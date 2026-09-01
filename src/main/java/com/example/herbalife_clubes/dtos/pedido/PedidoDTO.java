package com.example.herbalife_clubes.dtos.pedido;

import com.example.herbalife_clubes.dtos.pedido.PedidoItemOpcionResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Integer id;
    private Integer membresiaId;
    private String membresiaNumeroSocio;
    private Integer clubId;
    private String clubNombre;
    // Compatibilidad: antes existía productoId/cantidad (pedido de 1 item).
    private Integer productoId;
    private String productoNombre;
    private Integer cantidad;

    private String horarioDeseado; // @deprecated - usar tiempoEstimadoMinutos
    private String tipoConsumo; // PARA_RECOGER | EN_LUGAR
    private String tipoPago;
    private Integer tiempoEstimadoMinutos; // Tiempo estimado de preparación en minutos
    private String observaciones;
    private String estado; // RECIBIDO | PREPARANDO | LISTO | ENTREGADO | CANCELADO
    private Instant fechaPedido;
    
    // Lista de items del pedido (pedido_items). Incluye componentes de combo para compatibilidad;
    // clientes modernos deben renderizar combos[] y omitir items con pedidoComboId/comboId duplicado.
    private List<PedidoItemDTO> items;

    /** Líneas comerciales de combos (precio congelado). Preferir esto frente a agrupar items. */
    private List<PedidoComboResponseDTO> combos = new ArrayList<>();

    /** Total = SUM(items sueltos) + SUM(combos.subtotal). Calculado al mapear si no viene persistido. */
    private BigDecimal total;

    /**
     * Selecciones del endpoint legacy POST /pedidos (un solo producto).
     */
    private List<PedidoItemOpcionResponseDTO> opciones = new ArrayList<>();
}

