-- Permitir productos repetidos dentro del mismo pedido.
-- Caso de uso: mismo producto con notas distintas (ej. "sin azucar" y "con miel").
ALTER TABLE pedido_items
DROP CONSTRAINT IF EXISTS uk_pedido_items_pedido_producto;
