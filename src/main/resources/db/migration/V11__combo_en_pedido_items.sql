-- V11: Vincular pedido_items con combos para trazabilidad (idempotente).
ALTER TABLE pedido_items ADD COLUMN IF NOT EXISTS combo_id INTEGER REFERENCES combos(id) ON DELETE SET NULL;
