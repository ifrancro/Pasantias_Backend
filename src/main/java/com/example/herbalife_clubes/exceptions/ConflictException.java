package com.example.herbalife_clubes.exceptions;

/**
 * Excepción para manejar conflictos (código 409).
 * Se usa cuando un recurso ya existe o hay una violación de restricción de unicidad.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

