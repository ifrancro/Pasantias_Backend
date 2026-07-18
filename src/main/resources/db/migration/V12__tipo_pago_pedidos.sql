-- V12: tipo de pago en pedidos (idempotente para reintentos seguros).
ALTER TABLE pedidos ADD COLUMN IF NOT EXISTS tipo_pago VARCHAR(30) NULL;
