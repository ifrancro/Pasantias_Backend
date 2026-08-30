package com.example.herbalife_clubes.dtos.pedido;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoItemDTO {
    private Integer productoId;
    private String productoNombre; // Nombre del producto para facilitar visualización
    private Integer cantidad;
    private String nota; // Nota específica del item (opcional)
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Integer comboId; // ID del combo origen (null si es producto suelto)
    private String comboNombre; // Nombre del combo para visualización
    /** Selecciones estructuradas (request: grupoId/opcionId/cantidad; response incluye snapshots). */
    private List<PedidoItemOpcionResponseDTO> opciones = new ArrayList<>();
}

