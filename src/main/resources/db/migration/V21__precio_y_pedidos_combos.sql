-- V21: precio propio del combo + pedido_combos (snapshot) + vínculo en pedido_items.

ALTER TABLE combos
    ADD COLUMN IF NOT EXISTS precio DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE combos
    DROP CONSTRAINT IF EXISTS chk_combos_precio;

ALTER TABLE combos
    ADD CONSTRAINT chk_combos_precio CHECK (precio >= 0);

COMMENT ON COLUMN combos.precio IS
    'Precio de venta único del combo definido por el club (no override).';

CREATE TABLE IF NOT EXISTS pedido_combos (
    id                      SERIAL PRIMARY KEY,
    pedido_id               INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    combo_id                INTEGER REFERENCES combos(id) ON DELETE SET NULL,
    combo_nombre_snapshot   VARCHAR(150) NOT NULL,
    cantidad                INTEGER NOT NULL,
    precio_unitario_snapshot DECIMAL(10, 2) NOT NULL,
    subtotal_snapshot       DECIMAL(10, 2) NOT NULL,
    puntos_valor_snapshot   INTEGER NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_pc_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_pc_precio_unitario CHECK (precio_unitario_snapshot >= 0),
    CONSTRAINT chk_pc_subtotal CHECK (subtotal_snapshot >= 0)
);

COMMENT ON TABLE pedido_combos IS
    'Línea comercial congelada de un combo en un pedido. El dinero vive aquí, no en los componentes.';

CREATE INDEX IF NOT EXISTS idx_pedido_combos_pedido
    ON pedido_combos (pedido_id);

CREATE INDEX IF NOT EXISTS idx_pedido_combos_combo
    ON pedido_combos (combo_id)
    WHERE combo_id IS NOT NULL;

ALTER TABLE pedido_items
    ADD COLUMN IF NOT EXISTS pedido_combo_id INTEGER REFERENCES pedido_combos(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_pedido_items_pedido_combo
    ON pedido_items (pedido_combo_id)
    WHERE pedido_combo_id IS NOT NULL;
