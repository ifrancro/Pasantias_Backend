-- Parches previos a Flyway baseline=12 para tests.
-- Simula el estado de una BD que ya recibió V2–V12 vía Hibernate/scripts
-- manuales, sin historial Flyway. Solo para classpath de test.

-- V9
CREATE TABLE IF NOT EXISTS sabores (
    id            SERIAL PRIMARY KEY,
    hub_id        INTEGER NOT NULL REFERENCES hubs(id),
    nombre        VARCHAR(100) NOT NULL,
    activo        BOOLEAN DEFAULT true,
    created_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE(hub_id, nombre)
);

CREATE TABLE IF NOT EXISTS producto_sabores (
    id            SERIAL PRIMARY KEY,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER NOT NULL REFERENCES sabores(id) ON DELETE CASCADE,
    UNIQUE(producto_id, sabor_id)
);

CREATE TABLE IF NOT EXISTS club_producto_sabores (
    id            SERIAL PRIMARY KEY,
    club_id       INTEGER NOT NULL REFERENCES clubes(id) ON DELETE CASCADE,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER NOT NULL REFERENCES sabores(id) ON DELETE CASCADE,
    disponible    BOOLEAN DEFAULT true,
    created_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE(club_id, producto_id, sabor_id)
);

-- V10
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

CREATE TABLE IF NOT EXISTS combo_items (
    id            SERIAL PRIMARY KEY,
    combo_id      INTEGER NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    producto_id   INTEGER NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    sabor_id      INTEGER REFERENCES sabores(id) ON DELETE SET NULL,
    cantidad      INTEGER NOT NULL DEFAULT 1,
    UNIQUE(combo_id, producto_id, sabor_id)
);

-- V11 / V12
ALTER TABLE pedido_items ADD COLUMN IF NOT EXISTS combo_id INTEGER REFERENCES combos(id) ON DELETE SET NULL;
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS tipo_pago VARCHAR(30) NULL;

-- Tabla usada por la app (creada históricamente por Hibernate, no por V2–V12)
CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP
);
