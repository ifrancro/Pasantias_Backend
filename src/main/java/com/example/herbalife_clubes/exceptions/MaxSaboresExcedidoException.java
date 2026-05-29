package com.example.herbalife_clubes.exceptions;

public class MaxSaboresExcedidoException extends RuntimeException {
    public static final String ERROR_CODE = "MAX_SABORES_EXCEDIDO";
    private final int maxPermitido;
    private final int actual;

    public MaxSaboresExcedidoException(int maxPermitido, int actual, String message) {
        super(message);
        this.maxPermitido = maxPermitido;
        this.actual = actual;
    }

    public int getMaxPermitido() {
        return maxPermitido;
    }

    public int getActual() {
        return actual;
    }
}
