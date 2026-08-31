CREATE UNIQUE INDEX IF NOT EXISTS uq_pedidos_client_order_id
    ON pedidos (client_order_id)
    WHERE client_order_id IS NOT NULL;
