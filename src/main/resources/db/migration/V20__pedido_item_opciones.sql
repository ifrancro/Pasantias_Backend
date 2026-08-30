-- V20: selecciones estructuradas por ítem de pedido (snapshots + FK nullable).
-- Aditivo: pedidos anteriores quedan con opciones vacías en lectura.

CREATE TABLE IF NOT EXISTS pedido_item_opciones (
    id                      SERIAL PRIMARY KEY,
    pedido_item_id          INTEGER NOT NULL REFERENCES pedido_items(id) ON DELETE CASCADE,
    grupo_id                INTEGER REFERENCES producto_grupos_opciones(id) ON DELETE SET NULL,
    opcion_id               INTEGER REFERENCES producto_opciones(id) ON DELETE SET NULL,
    grupo_nombre_snapshot   VARCHAR(100) NOT NULL,
    opcion_nombre_snapshot  VARCHAR(100) NOT NULL,
    grupo_orden_snapshot    INTEGER NOT NULL,
    opcion_orden_snapshot   INTEGER NOT NULL,
    cantidad                INTEGER NOT NULL,
    CONSTRAINT chk_pio_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_pio_grupo_orden CHECK (grupo_orden_snapshot >= 0),
    CONSTRAINT chk_pio_opcion_orden CHECK (opcion_orden_snapshot >= 0)
);

COMMENT ON TABLE pedido_item_opciones IS
    'Selección congelada de opciones por ítem. FKs nullable: el catálogo puede eliminarse sin borrar historia.';
COMMENT ON COLUMN pedido_item_opciones.grupo_id IS
    'Referencia viva opcional; ON DELETE SET NULL. El nombre histórico está en grupo_nombre_snapshot.';
COMMENT ON COLUMN pedido_item_opciones.opcion_id IS
    'Referencia viva opcional; ON DELETE SET NULL. El nombre histórico está en opcion_nombre_snapshot.';

CREATE INDEX IF NOT EXISTS idx_pio_pedido_item
    ON pedido_item_opciones (pedido_item_id);

CREATE INDEX IF NOT EXISTS idx_pio_opcion
    ON pedido_item_opciones (opcion_id)
    WHERE opcion_id IS NOT NULL;

-- Una fila por opción viva por ítem; NULL opcion_id no colisiona (histórico tras borrado).
CREATE UNIQUE INDEX IF NOT EXISTS uq_pio_item_opcion
    ON pedido_item_opciones (pedido_item_id, opcion_id)
    WHERE opcion_id IS NOT NULL;
