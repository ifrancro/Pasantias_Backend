package com.example.herbalife_clubes.pricing;

import com.example.herbalife_clubes.entities.ClubProducto;
import com.example.herbalife_clubes.entities.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PrecioEfectivoTest {

    @Test
    void globalOverrideGanaAlPrecioBase() {
        assertEquals(bd("28.00"),
                PrecioEfectivo.resolverPrecioEfectivo(global(bd("25.00")), club(bd("28.00"))));
    }

    @Test
    void globalOverrideNullUsaPrecioBase() {
        assertEquals(bd("25.00"),
                PrecioEfectivo.resolverPrecioEfectivo(global(bd("25.00")), club(null)));
    }

    @Test
    void globalSinClubProductoUsaPrecioBase() {
        assertEquals(bd("25.00"),
                PrecioEfectivo.resolverPrecioEfectivo(global(bd("25.00")), null));
    }

    @Test
    void localIgnoraOverrideAccidentalEnClubProducto() {
        assertEquals(bd("20.00"),
                PrecioEfectivo.resolverPrecioEfectivo(local(bd("20.00")), club(bd("32.00"))));
    }

    @Test
    void localSinClubProductoUsaProductoPrecio() {
        assertEquals(bd("32.00"),
                PrecioEfectivo.resolverPrecioEfectivo(local(bd("32.00")), null));
    }

    @Test
    void globalOverrideCeroSeResuelveACeroYNoEstaConfigurado() {
        BigDecimal efectivo = PrecioEfectivo.resolverPrecioEfectivo(global(bd("25.00")), club(BigDecimal.ZERO));
        assertEquals(0, efectivo.compareTo(BigDecimal.ZERO));
        assertFalse(PrecioEfectivo.estaConfigurado(efectivo));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PrecioEfectivo.assertConfigurado(efectivo));
        assertEquals(PrecioEfectivo.MENSAJE_PRECIO_NO_CONFIGURADO, ex.getMessage());
    }

    @Test
    void precioBaseCeroNoEstaConfigurado() {
        assertFalse(PrecioEfectivo.estaConfigurado(
                PrecioEfectivo.resolverPrecioEfectivo(global(BigDecimal.ZERO), null)));
    }

    private static Producto global(BigDecimal precio) {
        Producto producto = new Producto();
        producto.setTipo("GLOBAL");
        producto.setPrecio(precio);
        return producto;
    }

    private static Producto local(BigDecimal precio) {
        Producto producto = new Producto();
        producto.setTipo("LOCAL");
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
