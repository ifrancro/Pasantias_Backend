-- =====================================================
-- RECREACION COMPLETA DE ESQUEMA (PostgreSQL)
-- Proyecto: herbalife_clubes
-- =====================================================
-- Uso:
--   psql -d HerbalifeClubesDB -f scripts/recrear_bd_completa.sql
--
-- Nota:
-- - Este script crea tablas, constraints e indices.
-- - No inserta datos semilla.
-- =====================================================

BEGIN;

-- =========================
-- 1) Tablas base
-- =========================

CREATE TABLE IF NOT EXISTS roles (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuarios (
  id SERIAL PRIMARY KEY,
  rol_id INTEGER NOT NULL,
  nombre VARCHAR(255),
  apellido VARCHAR(255),
  email VARCHAR(255) UNIQUE,
  password_hash VARCHAR(255),
  telefono VARCHAR(255),
  fecha_nacimiento DATE,
  redes_sociales VARCHAR(255),
  estado VARCHAR(255),
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hubs (
  id SERIAL PRIMARY KEY,
  admin_id INTEGER NOT NULL,
  nombre VARCHAR(255),
  descripcion TEXT,
  estado VARCHAR(255),
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS niveles_socio (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(255),
  visitas_requeridas INTEGER,
  descripcion_beneficios TEXT
);

CREATE TABLE IF NOT EXISTS clubes (
  id SERIAL PRIMARY KEY,
  hub_id INTEGER NOT NULL,
  anfitrion_id INTEGER NOT NULL,
  nombre_club VARCHAR(255),
  direccion VARCHAR(255),
  horario VARCHAR(255),
  lat NUMERIC(10,8),
  lng NUMERIC(11,8),
  estado VARCHAR(255),
  prefijo_socio VARCHAR(255),
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS productos (
  id SERIAL PRIMARY KEY,
  hub_id INTEGER NOT NULL,
  club_creador_id INTEGER,
  nombre VARCHAR(255) NOT NULL,
  descripcion TEXT,
  imagen_url VARCHAR(1024),
  ingredientes TEXT,
  puntos_valor INTEGER,
  tipo VARCHAR(50),
  estado_aprobacion VARCHAR(50),
  activo BOOLEAN,
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS logros (
  id SERIAL PRIMARY KEY,
  club_creador_id INTEGER,
  nombre VARCHAR(255),
  descripcion TEXT,
  icono_url VARCHAR(255),
  puntos_recompensa INTEGER,
  fecha_inicio DATE,
  fecha_fin DATE,
  estado_aprobacion VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS requisitos_logro (
  id SERIAL PRIMARY KEY,
  logro_id INTEGER NOT NULL,
  tipo_metrica VARCHAR(50) NOT NULL,
  cantidad_esperada INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS membresias (
  id SERIAL PRIMARY KEY,
  usuario_id INTEGER NOT NULL UNIQUE,
  club_id INTEGER NOT NULL,
  nivel_id INTEGER,
  numero_socio VARCHAR(255) UNIQUE,
  puntos_acumulados INTEGER,
  referido_por_membresia_id INTEGER,
  como_conocio VARCHAR(255),
  fecha_registro TIMESTAMP,
  estado VARCHAR(255),
  racha_actual INTEGER,
  racha_maxima INTEGER,
  ultima_asistencia_dia DATE
);

CREATE TABLE IF NOT EXISTS pedidos (
  id SERIAL PRIMARY KEY,
  membresia_id INTEGER NOT NULL,
  club_id INTEGER NOT NULL,
  tipo_consumo VARCHAR(30) NOT NULL,
  tiempo_estimado_minutos INTEGER,
  observaciones TEXT,
  estado VARCHAR(30) NOT NULL,
  fecha_pedido TIMESTAMP,
  producto_id INTEGER NOT NULL,
  cantidad INTEGER
);

CREATE TABLE IF NOT EXISTS pedido_items (
  id SERIAL PRIMARY KEY,
  pedido_id INTEGER NOT NULL,
  producto_id INTEGER NOT NULL,
  cantidad INTEGER NOT NULL,
  nota TEXT
);

CREATE TABLE IF NOT EXISTS asistencias (
  id SERIAL PRIMARY KEY,
  membresia_id INTEGER NOT NULL,
  club_id INTEGER NOT NULL,
  fecha_hora TIMESTAMP,
  fecha_dia DATE,
  estado VARCHAR(255),
  nota TEXT,
  CONSTRAINT uk_asistencias_membresia_fecha UNIQUE (membresia_id, fecha_dia)
);

CREATE TABLE IF NOT EXISTS consumos (
  id SERIAL PRIMARY KEY,
  membresia_id INTEGER NOT NULL,
  club_id INTEGER NOT NULL,
  asistencia_id INTEGER,
  pedido_id INTEGER,
  descripcion TEXT,
  fecha_hora TIMESTAMP,
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notificaciones (
  id SERIAL PRIMARY KEY,
  titulo VARCHAR(255),
  mensaje TEXT,
  tipo_segmentacion VARCHAR(255),
  hub_id INTEGER,
  club_id INTEGER,
  usuario_id INTEGER,
  pedido_id INTEGER,
  fecha_envio TIMESTAMP
);

CREATE TABLE IF NOT EXISTS fotos_club (
  id SERIAL PRIMARY KEY,
  club_id INTEGER NOT NULL,
  url_foto VARCHAR(255),
  tipo VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS eventos (
  id SERIAL PRIMARY KEY,
  hub_id INTEGER,
  club_id INTEGER,
  nombre VARCHAR(255),
  fecha_evento DATE,
  descripcion TEXT
);

CREATE TABLE IF NOT EXISTS soporte_tickets (
  id SERIAL PRIMARY KEY,
  usuario_id INTEGER NOT NULL,
  tipo_solicitud VARCHAR(255),
  asunto VARCHAR(255),
  mensaje TEXT,
  estado VARCHAR(255),
  respuesta_admin TEXT,
  fecha_creacion TIMESTAMP,
  fecha_respuesta TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_productos (
  id SERIAL PRIMARY KEY,
  club_id INTEGER NOT NULL,
  producto_id INTEGER NOT NULL,
  disponible BOOLEAN,
  created_at TIMESTAMP,
  CONSTRAINT uk_club_productos_club_producto UNIQUE (club_id, producto_id)
);

CREATE TABLE IF NOT EXISTS membresia_logros (
  id SERIAL PRIMARY KEY,
  membresia_id INTEGER NOT NULL,
  logro_id INTEGER NOT NULL,
  fecha_obtencion TIMESTAMP,
  CONSTRAINT uk_membresia_logros_membresia_logro UNIQUE (membresia_id, logro_id)
);

CREATE TABLE IF NOT EXISTS prospectos (
  id SERIAL PRIMARY KEY,
  club_id INTEGER NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  telefono VARCHAR(50) NOT NULL,
  referido_por_membresia_id INTEGER,
  fecha_creacion DATE NOT NULL DEFAULT CURRENT_DATE,
  estado VARCHAR(50) NOT NULL DEFAULT 'EN_SEGUIMIENTO'
);

CREATE TABLE IF NOT EXISTS misiones_prospecto (
  id SERIAL PRIMARY KEY,
  prospecto_id INTEGER NOT NULL,
  nombre VARCHAR(255) NOT NULL,
  descripcion TEXT,
  meta_cantidad INTEGER NOT NULL,
  progreso_actual INTEGER NOT NULL DEFAULT 0,
  fecha_limite DATE
);

CREATE TABLE IF NOT EXISTS compras_manuales (
  id SERIAL PRIMARY KEY,
  membresia_id INTEGER NOT NULL,
  club_id INTEGER NOT NULL,
  descripcion TEXT NOT NULL,
  monto NUMERIC(10,2) NOT NULL,
  fecha DATE NOT NULL DEFAULT CURRENT_DATE,
  registrada_por_host_id INTEGER NOT NULL
);

-- =========================
-- 2) Foreign keys
-- =========================

ALTER TABLE usuarios
  ADD CONSTRAINT fk_usuarios_rol
  FOREIGN KEY (rol_id) REFERENCES roles(id);

ALTER TABLE hubs
  ADD CONSTRAINT fk_hubs_admin
  FOREIGN KEY (admin_id) REFERENCES usuarios(id);

ALTER TABLE clubes
  ADD CONSTRAINT fk_clubes_hub
  FOREIGN KEY (hub_id) REFERENCES hubs(id);

ALTER TABLE clubes
  ADD CONSTRAINT fk_clubes_anfitrion
  FOREIGN KEY (anfitrion_id) REFERENCES usuarios(id);

ALTER TABLE productos
  ADD CONSTRAINT fk_productos_hub
  FOREIGN KEY (hub_id) REFERENCES hubs(id);

ALTER TABLE productos
  ADD CONSTRAINT fk_productos_club_creador
  FOREIGN KEY (club_creador_id) REFERENCES clubes(id) ON DELETE SET NULL;

ALTER TABLE logros
  ADD CONSTRAINT fk_logros_club_creador
  FOREIGN KEY (club_creador_id) REFERENCES clubes(id) ON DELETE SET NULL;

ALTER TABLE requisitos_logro
  ADD CONSTRAINT fk_requisitos_logro_logro
  FOREIGN KEY (logro_id) REFERENCES logros(id) ON DELETE CASCADE;

ALTER TABLE membresias
  ADD CONSTRAINT fk_membresias_usuario
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE membresias
  ADD CONSTRAINT fk_membresias_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE membresias
  ADD CONSTRAINT fk_membresias_nivel
  FOREIGN KEY (nivel_id) REFERENCES niveles_socio(id);

ALTER TABLE membresias
  ADD CONSTRAINT fk_membresias_referido_por
  FOREIGN KEY (referido_por_membresia_id) REFERENCES membresias(id) ON DELETE SET NULL;

ALTER TABLE pedidos
  ADD CONSTRAINT fk_pedidos_membresia
  FOREIGN KEY (membresia_id) REFERENCES membresias(id);

ALTER TABLE pedidos
  ADD CONSTRAINT fk_pedidos_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE pedidos
  ADD CONSTRAINT fk_pedidos_producto
  FOREIGN KEY (producto_id) REFERENCES productos(id);

ALTER TABLE pedido_items
  ADD CONSTRAINT fk_pedido_items_pedido
  FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE;

ALTER TABLE pedido_items
  ADD CONSTRAINT fk_pedido_items_producto
  FOREIGN KEY (producto_id) REFERENCES productos(id);

ALTER TABLE asistencias
  ADD CONSTRAINT fk_asistencias_membresia
  FOREIGN KEY (membresia_id) REFERENCES membresias(id);

ALTER TABLE asistencias
  ADD CONSTRAINT fk_asistencias_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE consumos
  ADD CONSTRAINT fk_consumos_membresia
  FOREIGN KEY (membresia_id) REFERENCES membresias(id);

ALTER TABLE consumos
  ADD CONSTRAINT fk_consumos_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE consumos
  ADD CONSTRAINT fk_consumos_asistencia
  FOREIGN KEY (asistencia_id) REFERENCES asistencias(id);

ALTER TABLE consumos
  ADD CONSTRAINT fk_consumos_pedido
  FOREIGN KEY (pedido_id) REFERENCES pedidos(id);

ALTER TABLE notificaciones
  ADD CONSTRAINT fk_notificaciones_hub
  FOREIGN KEY (hub_id) REFERENCES hubs(id);

ALTER TABLE notificaciones
  ADD CONSTRAINT fk_notificaciones_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE notificaciones
  ADD CONSTRAINT fk_notificaciones_usuario
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE notificaciones
  ADD CONSTRAINT fk_notificaciones_pedido
  FOREIGN KEY (pedido_id) REFERENCES pedidos(id);

ALTER TABLE fotos_club
  ADD CONSTRAINT fk_fotos_club_club
  FOREIGN KEY (club_id) REFERENCES clubes(id) ON DELETE CASCADE;

ALTER TABLE eventos
  ADD CONSTRAINT fk_eventos_hub
  FOREIGN KEY (hub_id) REFERENCES hubs(id);

ALTER TABLE eventos
  ADD CONSTRAINT fk_eventos_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE soporte_tickets
  ADD CONSTRAINT fk_soporte_tickets_usuario
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id);

ALTER TABLE club_productos
  ADD CONSTRAINT fk_club_productos_club
  FOREIGN KEY (club_id) REFERENCES clubes(id);

ALTER TABLE club_productos
  ADD CONSTRAINT fk_club_productos_producto
  FOREIGN KEY (producto_id) REFERENCES productos(id);

ALTER TABLE membresia_logros
  ADD CONSTRAINT fk_membresia_logros_membresia
  FOREIGN KEY (membresia_id) REFERENCES membresias(id);

ALTER TABLE membresia_logros
  ADD CONSTRAINT fk_membresia_logros_logro
  FOREIGN KEY (logro_id) REFERENCES logros(id);

ALTER TABLE prospectos
  ADD CONSTRAINT fk_prospectos_club
  FOREIGN KEY (club_id) REFERENCES clubes(id) ON DELETE CASCADE;

ALTER TABLE prospectos
  ADD CONSTRAINT fk_prospectos_referido
  FOREIGN KEY (referido_por_membresia_id) REFERENCES membresias(id) ON DELETE SET NULL;

ALTER TABLE misiones_prospecto
  ADD CONSTRAINT fk_misiones_prospecto_prospecto
  FOREIGN KEY (prospecto_id) REFERENCES prospectos(id) ON DELETE CASCADE;

ALTER TABLE compras_manuales
  ADD CONSTRAINT fk_compras_manuales_membresia
  FOREIGN KEY (membresia_id) REFERENCES membresias(id) ON DELETE CASCADE;

ALTER TABLE compras_manuales
  ADD CONSTRAINT fk_compras_manuales_club
  FOREIGN KEY (club_id) REFERENCES clubes(id) ON DELETE CASCADE;

ALTER TABLE compras_manuales
  ADD CONSTRAINT fk_compras_manuales_host
  FOREIGN KEY (registrada_por_host_id) REFERENCES usuarios(id);

-- =========================
-- 3) Checks (enums en Java)
-- =========================

ALTER TABLE pedidos
  ADD CONSTRAINT chk_pedidos_tipo_consumo
  CHECK (tipo_consumo IN ('PARA_RECOGER', 'EN_LUGAR'));

ALTER TABLE pedidos
  ADD CONSTRAINT chk_pedidos_estado
  CHECK (estado IN ('RECIBIDO', 'PREPARANDO', 'LISTO', 'ENTREGADO', 'CANCELADO'));

-- =========================
-- 4) Indices utiles
-- =========================

CREATE INDEX IF NOT EXISTS idx_productos_tipo ON productos(tipo);
CREATE INDEX IF NOT EXISTS idx_productos_club_creador ON productos(club_creador_id);
CREATE INDEX IF NOT EXISTS idx_membresias_referido_por ON membresias(referido_por_membresia_id);
CREATE INDEX IF NOT EXISTS idx_logros_club_creador ON logros(club_creador_id);
CREATE INDEX IF NOT EXISTS idx_logros_estado_aprobacion ON logros(estado_aprobacion);
CREATE INDEX IF NOT EXISTS idx_logros_fechas_vigencia ON logros(fecha_inicio, fecha_fin);
CREATE INDEX IF NOT EXISTS idx_requisitos_logro_logro ON requisitos_logro(logro_id);
CREATE INDEX IF NOT EXISTS idx_prospectos_club ON prospectos(club_id);
CREATE INDEX IF NOT EXISTS idx_misiones_prospecto_prospecto ON misiones_prospecto(prospecto_id);
CREATE INDEX IF NOT EXISTS idx_compras_manuales_membresia_fecha ON compras_manuales(membresia_id, fecha DESC);

COMMIT;
