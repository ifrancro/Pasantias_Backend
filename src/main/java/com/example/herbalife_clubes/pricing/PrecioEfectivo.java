package com.example.herbalife_clubes.pricing;

import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Producto;

import java.math.BigDecimal;

/**
 * Precio de venta autoritativo por club.
 *
 * <p>{@code Producto.precio} = precio BASE / sugerido.
 * {@code ClubProducto.precioVenta} = override comercial opcional.
 * GLOBAL sin fila {@code club_productos} (PROD-AVAIL-002): usa el precio base.
 */
public final class PrecioEfectivo {

    public static final String MENSAJE_PRECIO_NO_CONFIGURADO =
            "El producto no tiene un precio de venta configurado";

    private PrecioEfectivo() {
    }

    /**
     * {@code precioEfectivo = clubProducto.precioVenta != null ? clubProducto.precioVenta : producto.precio}
     */
    public static BigDecimal resolverPrecioEfectivo(Producto producto, ClubProducto clubProducto) {
        if (clubProducto != null && clubProducto.getPrecioVenta() != null) {
            return clubProducto.getPrecioVenta();
        }
        return producto != null ? producto.getPrecio() : null;
    }

    /**
     * 0 y null no se interpretan como gratis. Un producto realmente gratuito
     * exigirá una regla explícita posterior.
     */
    public static boolean estaConfigurado(BigDecimal precioEfectivo) {
        return precioEfectivo != null && precioEfectivo.compareTo(BigDecimal.ZERO) > 0;
    }

    public static void assertConfigurado(BigDecimal precioEfectivo) {
        if (!estaConfigurado(precioEfectivo)) {
            throw new IllegalArgumentException(MENSAJE_PRECIO_NO_CONFIGURADO);
        }
    }
}
