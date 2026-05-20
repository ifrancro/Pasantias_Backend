package com.example.herbalife_clubes.exceptions;

/**
 * El socio debe haber consumido al menos un producto tipo COMBO (pedido ENTREGADO)
 * antes de registrar asistencia.
 */
public class ComboRequiredException extends RuntimeException {

    public static final String ERROR_CODE = "COMBO_REQUIRED";

    public ComboRequiredException(String message) {
        super(message);
    }
}
