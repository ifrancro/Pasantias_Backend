-- POINTS-ORDER-001: snapshot de puntos por pedido e idempotencia de acreditación.
-- Pedidos históricos: puntos_ganados NULL, puntos_acreditados FALSE (sin backfill automático).

ALTER TABLE pedidos
    ADD COLUMN IF NOT EXISTS puntos_ganados INTEGER NULL,
    ADD COLUMN IF NOT EXISTS puntos_acreditados BOOLEAN NOT NULL DEFAULT FALSE;
