package com.example.herbalife_clubes.dtos.producto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecioVentaClubRequestDTO {
    /** Override del club. {@code null} elimina el override y vuelve al precio base. */
    private BigDecimal precioVenta;
}
