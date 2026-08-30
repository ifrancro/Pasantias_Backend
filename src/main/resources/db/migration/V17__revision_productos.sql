-- V17: ciclo de revisión de productos (comentario, revisor, fecha).
-- Nullable: productos nunca revisados y aprobaciones sin comentario quedan NULL.

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS comentario_revision TEXT;

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS revisado_por_usuario_id INTEGER REFERENCES usuarios(id);

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS revisado_at TIMESTAMP;
