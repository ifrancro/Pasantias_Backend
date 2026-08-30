-- V18: definición estructural de grupos de opciones por producto.
-- Aditivo: no toca sabores, combos ni ítems de pedido.
-- Disponibilidad por club, selección en pedido y recargos se implementan después.

CREATE TABLE IF NOT EXISTS producto_grupos_opciones (
    id                 SERIAL PRIMARY KEY,
    producto_id        INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    nombre             VARCHAR(100) NOT NULL,
    orden              INTEGER NOT NULL DEFAULT 0,
    min_selecciones    INTEGER NOT NULL DEFAULT 0,
    max_selecciones    INTEGER,
    permite_repetir    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_pgo_min_selecciones CHECK (min_selecciones >= 0),
    CONSTRAINT chk_pgo_max_selecciones CHECK (max_selecciones IS NULL OR max_selecciones >= min_selecciones),
    CONSTRAINT chk_pgo_orden CHECK (orden >= 0),
    CONSTRAINT uq_pgo_producto_nombre UNIQUE (producto_id, nombre)
);

COMMENT ON TABLE producto_grupos_opciones IS
    'Grupos de opciones de un producto (Sabores, Consistencia, etc.). Definición estructural, no disponibilidad de club.';
COMMENT ON COLUMN producto_grupos_opciones.max_selecciones IS
    'NULL = sin tope. Si permite_repetir=false no puede superar el número de opciones.';
COMMENT ON COLUMN producto_grupos_opciones.permite_repetir IS
    'true = en el pedido se puede elegir la misma opción más de una vez. No duplica filas en la definición.';

CREATE TABLE IF NOT EXISTS producto_opciones (
    id         SERIAL PRIMARY KEY,
    grupo_id   INTEGER NOT NULL REFERENCES producto_grupos_opciones(id) ON DELETE CASCADE,
    nombre     VARCHAR(100) NOT NULL,
    orden      INTEGER NOT NULL DEFAULT 0,
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_po_orden CHECK (orden >= 0),
    CONSTRAINT uq_po_grupo_nombre UNIQUE (grupo_id, nombre)
);

COMMENT ON TABLE producto_opciones IS
    'Opciones de un grupo. activo es catálogo; la disponibilidad operativa por club se implementa después.';
