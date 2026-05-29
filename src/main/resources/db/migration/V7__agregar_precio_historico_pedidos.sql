ALTER TABLE pedido_items
ADD COLUMN IF NOT EXISTS precio_unitario DECIMAL(10, 2) DEFAULT 0.00;

ALTER TABLE pedido_items
ADD COLUMN IF NOT EXISTS subtotal DECIMAL(10, 2) DEFAULT 0.00;

-- Backfill: tomar precio actual del producto para registros históricos existentes.
UPDATE pedido_items pi
SET precio_unitario = COALESCE(
    (SELECT pr.precio FROM productos pr WHERE pr.id = pi.producto_id),
    0.00
)
WHERE pi.precio_unitario IS NULL OR pi.precio_unitario = 0.00;

UPDATE pedido_items pi
SET subtotal = COALESCE(pi.precio_unitario, 0.00) * COALESCE(pi.cantidad, 1)
WHERE pi.subtotal IS NULL OR pi.subtotal = 0.00;
