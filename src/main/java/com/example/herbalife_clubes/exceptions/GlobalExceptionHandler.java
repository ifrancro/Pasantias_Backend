package com.example.herbalife_clubes.exceptions;

import com.example.herbalife_clubes.common.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Hidden
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Error de validación en uno o más campos")
                        .data(fieldErrors)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(emailAlreadyExistsBody(ex.getMessage()));
    }

    /**
     * Fallback seguro ante UNIQUE en carrera (p. ej. usuarios.email).
     * Nunca expone SQL, constraints ni mensajes de Hibernate/PostgreSQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolation ({})",
                ex.getMostSpecificCause() != null
                        ? ex.getMostSpecificCause().getClass().getSimpleName()
                        : ex.getClass().getSimpleName());

        if (looksLikeEmailUniqueViolation(ex)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(emailAlreadyExistsBody(EmailAlreadyExistsException.DEFAULT_MESSAGE));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", "CONFLICT");
        body.put("message", "No se pudo completar la operación por un conflicto de datos.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private static Map<String, Object> emailAlreadyExistsBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", EmailAlreadyExistsException.ERROR_CODE);
        body.put("message", message != null && !message.isBlank()
                ? message
                : EmailAlreadyExistsException.DEFAULT_MESSAGE);
        return body;
    }

    private static boolean looksLikeEmailUniqueViolation(DataIntegrityViolationException ex) {
        String joined = (safeLower(ex.getMessage()) + " "
                + safeLower(ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "")).toLowerCase();
        return joined.contains("usuarios_email")
                || joined.contains("email")
                || (joined.contains("unique") && joined.contains("usuario"));
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    /**
     * Formato esperado por Flutter al bloquear asistencia sin combo previo.
     */
    @ExceptionHandler(ComboRequiredException.class)
    public ResponseEntity<Map<String, String>> handleComboRequired(ComboRequiredException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ComboRequiredException.ERROR_CODE);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxSaboresExcedidoException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSabores(MaxSaboresExcedidoException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", MaxSaboresExcedidoException.ERROR_CODE);
        body.put("message", ex.getMessage());
        body.put("maxPermitido", ex.getMaxPermitido());
        body.put("actual", ex.getActual());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<String>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Credenciales incorrectas. Verifique su email y contraseña.")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", EmailNotVerifiedException.ERROR_CODE);
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(GoogleTokenInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleGoogleTokenInvalid(GoogleTokenInvalidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", GoogleTokenInvalidException.ERROR_CODE);
        body.put("message", GoogleTokenInvalidException.DEFAULT_MESSAGE);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(GoogleEmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleGoogleEmailNotVerified(
            GoogleEmailNotVerifiedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("error", GoogleEmailNotVerifiedException.ERROR_CODE);
        body.put("message", GoogleEmailNotVerifiedException.DEFAULT_MESSAGE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<String>> handleDisabledUser(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Usuario deshabilitado. Contacte al administrador.")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<String>> handleAuthenticationException(AuthenticationException ex) {
        // El detalle interno solo va al log: exponerlo al cliente filtraba
        // información del servidor y qué correos existen en la base de datos.
        log.warn("Fallo de autenticación ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Credenciales incorrectas. Verifique su email y contraseña.")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Error interno no controlado ({})", ex.getClass().getSimpleName());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message("Error interno del servidor")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}

