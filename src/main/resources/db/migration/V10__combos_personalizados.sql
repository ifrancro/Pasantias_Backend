-- ============================================================
-- V10: Combos personalizados por club
-- Permite que cada anfitrión cree combos propios agrupando
-- productos (max 3) con sabores pre-seleccionados.
-- ============================================================

-- 1. Tabla de combos (cada club crea los suyos)
CREATE TABLE IF NOT EXISTS combos (
    id            SERIAL PRIMARY KEY,
    club_id       INTEGER NOT NULL REFERENCES clubes(id) ON DELETE CASCADE,
    nombre        VARCHAR(150) NOT NULL,
    descripcion   TEXT,
    imagen_url    VARCHAR(1024),
    puntos_valor  INTEGER DEFAULT 0,
    activo        BOOLEAN DEFAULT true,
    created_at    TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE  combos IS 'Combos personalizados creados por cada anfitrión para su club';
COMMENT ON COLUMN combos.puntos_valor IS 'Puntos totales del combo (suma de sus items o valor personalizado)';

-- 2. Items que componen cada combo (max 3 productos distintos)
CREATE TABLE IF NOT EXISTS combo_items (
    id            SERIAL PRIMARY KEY,
    combo_id      INTEGER NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER REFERENCES sabores(id) ON DELETE SET NULL,
    cantidad      INTEGER NOT NULL DEFAULT 1,
    UNIQUE(combo_id, producto_id, sabor_id)
);

COMMENT ON TABLE  combo_items IS 'Productos y sabores que componen un combo';
COMMENT ON COLUMN combo_items.sabor_id IS 'Sabor pre-seleccionado (NULL si el producto no tiene sabores)';
COMMENT ON COLUMN combo_items.cantidad IS 'Cantidad de este producto en el combo';
