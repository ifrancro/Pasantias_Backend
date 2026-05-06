-- Soportar ventas en mostrador para clientes externos (sin membresia).
ALTER TABLE pedidos
ALTER COLUMN membresia_id DROP NOT NULL;
