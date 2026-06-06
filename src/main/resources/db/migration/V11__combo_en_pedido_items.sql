-- V11: Vincular pedido_items con combos para trazabilidad
ALTER TABLE pedido_items ADD COLUMN combo_id INTEGER REFERENCES combos(id) ON DELETE SET NULL;
