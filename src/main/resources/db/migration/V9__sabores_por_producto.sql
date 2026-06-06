-- ============================================================
-- V9: Sabores por producto
-- Permite que cada producto tenga sabores asignados y que cada
-- club gestione qué sabores ofrece para cada producto.
-- ============================================================

-- 1. Catálogo de sabores (a nivel de Hub)
CREATE TABLE IF NOT EXISTS sabores (
    id            SERIAL PRIMARY KEY,
    hub_id        INTEGER NOT NULL REFERENCES hubs(id),
    nombre        VARCHAR(100) NOT NULL,
    activo        BOOLEAN DEFAULT true,
    created_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE(hub_id, nombre)
);

COMMENT ON TABLE  sabores IS 'Catálogo de sabores disponibles en un Hub';
COMMENT ON COLUMN sabores.nombre IS 'Nombre del sabor: Vainilla, Chocolate, Fresa, etc.';

-- 2. Relación producto <-> sabor (qué sabores puede tener un producto)
CREATE TABLE IF NOT EXISTS producto_sabores (
    id            SERIAL PRIMARY KEY,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER NOT NULL REFERENCES sabores(id) ON DELETE CASCADE,
    UNIQUE(producto_id, sabor_id)
);

COMMENT ON TABLE producto_sabores IS 'Sabores asignados a cada producto (definido por Admin/Hub)';

-- 3. Disponibilidad de sabores por club (el anfitrión elige cuáles ofrece)
CREATE TABLE IF NOT EXISTS club_producto_sabores (
    id            SERIAL PRIMARY KEY,
    club_id       INTEGER NOT NULL REFERENCES clubes(id) ON DELETE CASCADE,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER NOT NULL REFERENCES sabores(id) ON DELETE CASCADE,
    disponible    BOOLEAN DEFAULT true,
    created_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE(club_id, producto_id, sabor_id)
);

COMMENT ON TABLE club_producto_sabores IS 'Sabores que cada club ofrece por producto (gestionado por anfitrión)';
