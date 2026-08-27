-- V16: propósito OTP + intentos fallidos + tokens de reset de contraseña.

ALTER TABLE verification_codes
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(32) NOT NULL DEFAULT 'EMAIL_VERIFICATION';

ALTER TABLE verification_codes
    ADD COLUMN IF NOT EXISTS failed_attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_verification_codes_usuario_purpose_used
    ON verification_codes (usuario_id, purpose, used);

CREATE INDEX IF NOT EXISTS idx_verification_codes_usuario_purpose_created
    ON verification_codes (usuario_id, purpose, created_at);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_hash_used
    ON password_reset_tokens (token_hash, used);

CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_usuario_used
    ON password_reset_tokens (usuario_id, used);
