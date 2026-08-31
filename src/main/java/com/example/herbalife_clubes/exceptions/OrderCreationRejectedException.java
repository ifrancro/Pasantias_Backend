package com.example.herbalife_clubes.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Rechazo funcional de POST /api/pedidos/con-items con código estable para Flutter sync.
 */
@Getter
public class OrderCreationRejectedException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public OrderCreationRejectedException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
