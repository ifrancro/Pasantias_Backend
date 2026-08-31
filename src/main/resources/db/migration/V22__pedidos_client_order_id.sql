-- ORD-SYNC-001: idempotencia de creación de pedidos vía clientOrderId (UUID v4 del cliente).

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS client_order_id VARCHAR(36) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pedidos_client_order_id
    ON pedidos (client_order_id)
    WHERE client_order_id IS NOT NULL;
