-- V13: índices para listados paginados de pedidos y membresías.
-- Motivo: ORDER BY fecha + id y filtros por club/membresía deben usar índice
-- en lugar de full scan cuando crezcan los volúmenes.
-- Nota: ILIKE '%texto%' de búsqueda global no se beneficia de B-tree; no se
-- activa pg_trgm en esta fase.

CREATE INDEX IF NOT EXISTS idx_pedidos_club_fecha_id
    ON pedidos (club_id, fecha_pedido DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_pedidos_membresia_fecha_id
    ON pedidos (membresia_id, fecha_pedido DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_membresias_club_fecha_id
    ON membresias (club_id, fecha_registro DESC, id DESC);
