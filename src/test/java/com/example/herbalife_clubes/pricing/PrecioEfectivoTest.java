package com.example.herbalife_clubes.pricing;

import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PrecioEfectivoTest {

    @Test
    void overrideGanaAlPrecioBase() {
        assertEquals(bd("28.00"), PrecioEfectivo.resolverPrecioEfectivo(producto(bd("25.00")), club(bd("28.00"))));
    }

    @Test
    void overrideNullUsaPrecioBase() {
        assertEquals(bd("25.00"), PrecioEfectivo.resolverPrecioEfectivo(producto(bd("25.00")), club(null)));
    }

    @Test
    void sinClubProductoUsaPrecioBase() {
        assertEquals(bd("25.00"), PrecioEfectivo.resolverPrecioEfectivo(producto(bd("25.00")), null));
    }

    @Test
    void overrideCeroSeResuelveACeroYNoEstaConfigurado() {
        BigDecimal efectivo = PrecioEfectivo.resolverPrecioEfectivo(producto(bd("25.00")), club(BigDecimal.ZERO));
        assertEquals(0, efectivo.compareTo(BigDecimal.ZERO));
        assertFalse(PrecioEfectivo.estaConfigurado(efectivo));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PrecioEfectivo.assertConfigurado(efectivo));
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_NO_CONFIGURADO, ex.getMessage());
    }

    @Test
    void precioBaseCeroNoEstaConfigurado() {
        assertFalse(PrecioEfectivo.estaConfigurado(
                PrecioEfectivo.resolverPrecioEfectivo(producto(BigDecimal.ZERO), null)));
    }

    private static Producto producto(BigDecimal precio) {
        Producto producto = new Producto();
        producto.setPrecio(precio);
        return producto;
    }

    private static ClubProducto club(BigDecimal precioVenta) {
        ClubProducto cp = new ClubProducto();
        cp.setPrecioVenta(precioVenta);
        return cp;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
