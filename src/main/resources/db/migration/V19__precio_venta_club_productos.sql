-- V19: override comercial de precio por club.
-- Producto.precio sigue siendo el precio BASE / sugerido.
-- club_productos.precio_venta NULL = usar el precio base.
-- No backfill a 0: 0 significaría override gratuito, no "usar base".
-- No toca productos.precio, pedidos ni combos.

ALTER TABLE club_productos
    ADD COLUMN IF NOT EXISTS precio_venta DECIMAL(10, 2) NULL;

ALTER TABLE club_productos
    DROP CONSTRAINT IF EXISTS chk_club_productos_precio_venta;

ALTER TABLE club_productos
    ADD CONSTRAINT chk_club_productos_precio_venta
    CHECK (precio_venta IS NULL OR precio_venta >= 0);

COMMENT ON COLUMN club_productos.precio_venta IS
    'Override comercial del club. NULL = usar productos.precio. No dispara revisión.';
