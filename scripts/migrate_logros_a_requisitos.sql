-- Migración: de logros con una sola métrica (columnas) a tabla requisitos_logro.
-- Ejecutar UNA VEZ sobre una BD que aún tenga tipo_metrica / meta_cantidad / tipo_requisito en logros.
-- Ajusta según tu motor (este script es para PostgreSQL).

BEGIN;

CREATE TABLE IF NOT EXISTS requisitos_logro (
  id SERIAL PRIMARY KEY,
  logro_id INTEGER NOT NULL,
  tipo_metrica VARCHAR(50) NOT NULL,
  cantidad_esperada INTEGER NOT NULL
);

-- Desde tipo_metrica + meta_cantidad (si existían)
INSERT INTO requisitos_logro (logro_id, tipo_metrica, cantidad_esperada)
SELECT id, UPPER(TRIM(tipo_metrica)), meta_cantidad
FROM logros
WHERE tipo_metrica IS NOT NULL AND TRIM(tipo_metrica) <> ''
  AND meta_cantidad IS NOT NULL AND meta_cantidad > 0
  AND NOT EXISTS (SELECT 1 FROM requisitos_logro r WHERE r.logro_id = logros.id);

-- Desde tipo_requisito (umbral de asistencias) si no hubo fila anterior
INSERT INTO requisitos_logro (logro_id, tipo_metrica, cantidad_esperada)
SELECT id, 'ASISTENCIA', tipo_requisito
FROM logros
WHERE tipo_requisito IS NOT NULL AND tipo_requisito > 0
  AND NOT EXISTS (SELECT 1 FROM requisitos_logro r WHERE r.logro_id = logros.id);

ALTER TABLE requisitos_logro
  ADD CONSTRAINT fk_requisitos_logro_logro
  FOREIGN KEY (logro_id) REFERENCES logros(id) ON DELETE CASCADE;

ALTER TABLE logros DROP COLUMN IF EXISTS tipo_requisito;
ALTER TABLE logros DROP COLUMN IF EXISTS tipo_metrica;
ALTER TABLE logros DROP COLUMN IF EXISTS meta_cantidad;

CREATE INDEX IF NOT EXISTS idx_requisitos_logro_logro ON requisitos_logro(logro_id);

COMMIT;
