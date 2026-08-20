-- V15: tabla de códigos OTP de verificación de correo.

CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP
);

-- Sirve a invalidateAllByUsuario y a countRecentCodes.
CREATE INDEX IF NOT EXISTS idx_verification_codes_usuario_used
    ON verification_codes (usuario_id, used);

-- Sirve a la consulta de límite de reenvíos.
CREATE INDEX IF NOT EXISTS idx_verification_codes_usuario_created
    ON verification_codes (usuario_id, created_at);
