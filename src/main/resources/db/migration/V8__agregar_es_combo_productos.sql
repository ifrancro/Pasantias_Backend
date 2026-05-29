-- Desacoplar la categoría "Combo" del origen del producto (tipo GLOBAL/LOCAL).
-- Antes el valor tipo='COMBO' se usaba a la vez como categoría y como origen, lo que
-- impedía que un combo apareciera en el menú del club (que filtra por GLOBAL/LOCAL)
-- y que un anfitrión pudiera crear combos desde la web.

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS es_combo BOOLEAN DEFAULT false;

-- Backfill: los productos que eran tipo='COMBO' pasan a marcarse como combo.
UPDATE productos
SET es_combo = true
WHERE UPPER(tipo) = 'COMBO';

-- Normalizar el origen de los combos antiguos para que aparezcan en el menú:
-- sin club creador -> GLOBAL (del hub); con club creador -> LOCAL.
UPDATE productos
SET tipo = CASE WHEN club_creador_id IS NULL THEN 'GLOBAL' ELSE 'LOCAL' END
WHERE UPPER(tipo) = 'COMBO';

-- Asegurar que no queden valores NULL.
UPDATE productos
SET es_combo = false
WHERE es_combo IS NULL;
