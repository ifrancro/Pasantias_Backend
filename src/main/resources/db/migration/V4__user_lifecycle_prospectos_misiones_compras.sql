-- Módulo ciclo de vida: prospectos, misiones, compras manuales y nota de asistencia.

CREATE TABLE IF NOT EXISTS prospectos (
    id SERIAL PRIMARY KEY,
    club_id INTEGER NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    telefono VARCHAR(50) NOT NULL,
    referido_por_membresia_id INTEGER NULL,
    fecha_creacion DATE NOT NULL DEFAULT CURRENT_DATE,
    estado VARCHAR(50) NOT NULL DEFAULT 'EN_SEGUIMIENTO'
);

CREATE TABLE IF NOT EXISTS misiones_prospecto (
    id SERIAL PRIMARY KEY,
    prospecto_id INTEGER NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    descripcion TEXT NULL,
    meta_cantidad INTEGER NOT NULL,
    progreso_actual INTEGER NOT NULL DEFAULT 0,
    fecha_limite DATE NULL
);

CREATE TABLE IF NOT EXISTS compras_manuales (
    id SERIAL PRIMARY KEY,
    membresia_id INTEGER NOT NULL,
    club_id INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    monto NUMERIC(10, 2) NOT NULL,
    fecha DATE NOT NULL DEFAULT CURRENT_DATE,
    registrada_por_host_id INTEGER NOT NULL
);

ALTER TABLE asistencias
ADD COLUMN IF NOT EXISTS nota TEXT;

ALTER TABLE prospectos
ADD CONSTRAINT fk_prospectos_club
FOREIGN KEY (club_id)
REFERENCES clubes(id)
ON DELETE CASCADE;

ALTER TABLE prospectos
ADD CONSTRAINT fk_prospectos_referido
FOREIGN KEY (referido_por_membresia_id)
REFERENCES membresias(id)
ON DELETE SET NULL;

ALTER TABLE misiones_prospecto
ADD CONSTRAINT fk_misiones_prospecto_prospecto
FOREIGN KEY (prospecto_id)
REFERENCES prospectos(id)
ON DELETE CASCADE;

ALTER TABLE compras_manuales
ADD CONSTRAINT fk_compras_manuales_membresia
FOREIGN KEY (membresia_id)
REFERENCES membresias(id)
ON DELETE CASCADE;

ALTER TABLE compras_manuales
ADD CONSTRAINT fk_compras_manuales_club
FOREIGN KEY (club_id)
REFERENCES clubes(id)
ON DELETE CASCADE;

ALTER TABLE compras_manuales
ADD CONSTRAINT fk_compras_manuales_host
FOREIGN KEY (registrada_por_host_id)
REFERENCES usuarios(id)
ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_prospectos_club ON prospectos(club_id);
CREATE INDEX IF NOT EXISTS idx_misiones_prospecto_prospecto ON misiones_prospecto(prospecto_id);
CREATE INDEX IF NOT EXISTS idx_compras_manuales_membresia_fecha ON compras_manuales(membresia_id, fecha DESC);
