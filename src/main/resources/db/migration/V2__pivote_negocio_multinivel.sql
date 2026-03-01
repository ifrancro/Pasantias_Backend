-- =====================================================
-- MIGRACIÓN: Pivote de Negocio - Red Multinivel
-- Fecha: 2024
-- Descripción: Actualización de esquema para soportar:
--   - Red multinivel de referidos
--   - Productos globales y locales con puntos
--   - Pedidos con tiempo estimado
--   - Logros con vigencia y aprobación
-- =====================================================

-- =====================================================
-- 1. TABLA: clubes
-- =====================================================
-- Añadir: prefijo_socio varchar (Ej: MDE)
ALTER TABLE clubes 
ADD COLUMN IF NOT EXISTS prefijo_socio VARCHAR(255);

COMMENT ON COLUMN clubes.prefijo_socio IS 'Prefijo para identificar socios del club (Ej: MDE)';

-- =====================================================
-- 2. TABLA: productos
-- =====================================================
-- Añadir: club_creador_id integer [nullable]
-- Añadir: ingredientes text
-- Añadir: puntos_valor integer (Por defecto 0)
-- Añadir: tipo varchar (GLOBAL | LOCAL)

ALTER TABLE productos 
ADD COLUMN IF NOT EXISTS club_creador_id INTEGER NULL,
ADD COLUMN IF NOT EXISTS ingredientes TEXT,
ADD COLUMN IF NOT EXISTS puntos_valor INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS tipo VARCHAR(50),
ADD COLUMN IF NOT EXISTS estado_aprobacion VARCHAR(50);

-- Establecer valor por defecto para puntos_valor en registros existentes
UPDATE productos 
SET puntos_valor = 0 
WHERE puntos_valor IS NULL;

-- Foreign Key: productos.club_creador_id -> clubes.id
ALTER TABLE productos 
ADD CONSTRAINT fk_productos_club_creador 
FOREIGN KEY (club_creador_id) 
REFERENCES clubes(id) 
ON DELETE SET NULL;

COMMENT ON COLUMN productos.club_creador_id IS 'Si es NULL es GLOBAL, si tiene ID es LOCAL';
COMMENT ON COLUMN productos.ingredientes IS 'Ingredientes privados del producto';
COMMENT ON COLUMN productos.puntos_valor IS 'Valor en puntos del producto (por defecto 0)';
COMMENT ON COLUMN productos.tipo IS 'Tipo de producto: GLOBAL o LOCAL';
COMMENT ON COLUMN productos.estado_aprobacion IS 'Estado de aprobación: APROBADO, PENDIENTE o RECHAZADO';

-- =====================================================
-- 3. TABLA: membresias
-- =====================================================
-- Eliminar: referido_por varchar
-- Añadir: referido_por_membresia_id integer [nullable]

-- Primero añadir la nueva columna
ALTER TABLE membresias 
ADD COLUMN IF NOT EXISTS referido_por_membresia_id INTEGER NULL;

-- Foreign Key: membresias.referido_por_membresia_id -> membresias.id (relación recursiva)
ALTER TABLE membresias 
ADD CONSTRAINT fk_membresias_referido_por 
FOREIGN KEY (referido_por_membresia_id) 
REFERENCES membresias(id) 
ON DELETE SET NULL;

-- Eliminar la columna antigua (comentado por seguridad - descomentar después de migrar datos si es necesario)
-- ALTER TABLE membresias DROP COLUMN IF EXISTS referido_por;

COMMENT ON COLUMN membresias.referido_por_membresia_id IS 'ID de la membresia que refirió a esta membresia (árbol multinivel)';

-- =====================================================
-- 4. TABLA: pedidos
-- =====================================================
-- Eliminar: horario_deseado
-- Modificar: tipo_consumo varchar (Ahora solo acepta: PARA_RECOGER | EN_LUGAR)
-- Añadir: tiempo_estimado_minutos integer [nullable]

-- Añadir nueva columna
ALTER TABLE pedidos 
ADD COLUMN IF NOT EXISTS tiempo_estimado_minutos INTEGER NULL;

-- Actualizar valores existentes de tipo_consumo si es necesario
-- Cambiar PARA_LLEVAR a PARA_RECOGER si existe
UPDATE pedidos 
SET tipo_consumo = 'PARA_RECOGER' 
WHERE tipo_consumo = 'PARA_LLEVAR';

-- Eliminar columna antigua (comentado por seguridad - descomentar después de verificar)
-- ALTER TABLE pedidos DROP COLUMN IF EXISTS horario_deseado;

COMMENT ON COLUMN pedidos.tiempo_estimado_minutos IS 'Tiempo estimado de preparación en minutos (Ej: 10, 15, 20)';
COMMENT ON COLUMN pedidos.tipo_consumo IS 'Tipo de consumo: PARA_RECOGER o EN_LUGAR';

-- =====================================================
-- 5. TABLA: logros
-- =====================================================
-- Añadir: club_creador_id integer [nullable]
-- Añadir: tipo_metrica varchar (CONSUMO | ASISTENCIA | REFERIDOS)
-- Añadir: meta_cantidad integer
-- Añadir: puntos_recompensa integer
-- Añadir: fecha_inicio date
-- Añadir: fecha_fin date
-- Añadir: estado_aprobacion varchar (PENDIENTE | APROBADO | RECHAZADO)

ALTER TABLE logros 
ADD COLUMN IF NOT EXISTS club_creador_id INTEGER NULL,
ADD COLUMN IF NOT EXISTS tipo_metrica VARCHAR(50),
ADD COLUMN IF NOT EXISTS meta_cantidad INTEGER,
ADD COLUMN IF NOT EXISTS puntos_recompensa INTEGER,
ADD COLUMN IF NOT EXISTS fecha_inicio DATE,
ADD COLUMN IF NOT EXISTS fecha_fin DATE,
ADD COLUMN IF NOT EXISTS estado_aprobacion VARCHAR(50);

-- Foreign Key: logros.club_creador_id -> clubes.id
ALTER TABLE logros 
ADD CONSTRAINT fk_logros_club_creador 
FOREIGN KEY (club_creador_id) 
REFERENCES clubes(id) 
ON DELETE SET NULL;

COMMENT ON COLUMN logros.club_creador_id IS 'Club que creó el logro (NULL si es global)';
COMMENT ON COLUMN logros.tipo_metrica IS 'Tipo de métrica: CONSUMO, ASISTENCIA o REFERIDOS';
COMMENT ON COLUMN logros.meta_cantidad IS 'Cantidad objetivo para completar el logro';
COMMENT ON COLUMN logros.puntos_recompensa IS 'Puntos que se otorgan al completar el logro';
COMMENT ON COLUMN logros.fecha_inicio IS 'Fecha de inicio de vigencia del logro';
COMMENT ON COLUMN logros.fecha_fin IS 'Fecha de fin de vigencia del logro';
COMMENT ON COLUMN logros.estado_aprobacion IS 'Estado de aprobación: PENDIENTE, APROBADO o RECHAZADO';

-- =====================================================
-- ÍNDICES PARA MEJORAR RENDIMIENTO
-- =====================================================

-- Índice para búsquedas de productos por tipo
CREATE INDEX IF NOT EXISTS idx_productos_tipo ON productos(tipo);

-- Índice para búsquedas de productos por club creador
CREATE INDEX IF NOT EXISTS idx_productos_club_creador ON productos(club_creador_id);

-- Índice para búsquedas de membresias por referido
CREATE INDEX IF NOT EXISTS idx_membresias_referido_por ON membresias(referido_por_membresia_id);

-- Índice para búsquedas de logros por club creador
CREATE INDEX IF NOT EXISTS idx_logros_club_creador ON logros(club_creador_id);

-- Índice para búsquedas de logros por estado de aprobación
CREATE INDEX IF NOT EXISTS idx_logros_estado_aprobacion ON logros(estado_aprobacion);

-- Índice para búsquedas de logros por fechas de vigencia
CREATE INDEX IF NOT EXISTS idx_logros_fechas_vigencia ON logros(fecha_inicio, fecha_fin);

-- =====================================================
-- FIN DE MIGRACIÓN
-- =====================================================

