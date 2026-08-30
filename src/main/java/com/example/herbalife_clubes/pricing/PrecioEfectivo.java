package com.example.herbalife_clubes.pricing;

import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Producto;

import java.math.BigDecimal;

/**
 * Precio de venta autoritativo por club.
 *
 * <p>GLOBAL: {@code Producto.precio} = base ADMIN; {@code ClubProducto.precioVenta} = override HOST opcional.
 * LOCAL: {@code Producto.precio} = único precio de venta; {@code precioVenta} en club_productos se ignora.
 */
public final class PrecioEfectivo {

    public static final String MENSAJE_PRECIO_NO_CONFIGURADO =
            "El producto no tiene un precio de venta configurado";

    public static final String MENSAJE_PRECIO_LOCAL_OBLIGATORIO =
            "El precio de venta es obligatorio para un producto local";

    private PrecioEfectivo() {
    }

    /**
     * LOCAL → {@code producto.precio}.
     * GLOBAL → {@code clubProducto.precioVenta} si existe; si no, {@code producto.precio}.
     * Un override accidental en LOCAL se ignora al resolver.
     */
    public static BigDecimal resolverPrecioEfectivo(Producto producto, ClubProducto clubProducto) {
        if (producto == null) {
            return null;
        }
        if ("LOCAL".equalsIgnoreCase(producto.getTipo())) {
            return producto.getPrecio();
        }
        if (clubProducto != null && clubProducto.getPrecioVenta() != null) {
            return clubProducto.getPrecioVenta();
        }
        return producto.getPrecio();
    }

    public static boolean esGlobal(Producto producto) {
        return producto != null && "GLOBAL".equalsIgnoreCase(producto.getTipo());
    }

    public static boolean esLocal(Producto producto) {
        return producto != null && "LOCAL".equalsIgnoreCase(producto.getTipo());
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
