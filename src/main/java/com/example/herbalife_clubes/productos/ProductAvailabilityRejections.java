package com.example.herbalife_clubes.productos;

import com.example.herbalife_clubes.exceptions.ProductAvailabilityRejectedException;
import org.springframework.http.HttpStatus;

/**
 * Códigos estables PROD-AVAIL-002 para habilitación de productos en club.
 */
public final class ProductAvailabilityRejections {

    public static final String PRODUCT_PRICE_REQUIRED = "PRODUCT_PRICE_REQUIRED";

    private static final String MSG_PRICE_REQUIRED =
            "Configura un precio de venta antes de habilitar este producto.";

    private ProductAvailabilityRejections() {
    }

    public static ProductAvailabilityRejectedException priceRequired() {
        return new ProductAvailabilityRejectedException(
                PRODUCT_PRICE_REQUIRED, MSG_PRICE_REQUIRED, HttpStatus.CONFLICT);
    }

    public static void throwPriceRequired() {
        throw priceRequired();
    }
}
