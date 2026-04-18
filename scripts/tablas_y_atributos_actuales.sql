-- Script: Tablas y atributos actuales (PostgreSQL)
-- Uso:
--   psql -d HerbalifeClubesDB -f scripts/tablas_y_atributos_actuales.sql
-- o ejecutar cada SELECT por separado.

-- 1) Todas las tablas base del esquema public
SELECT
  t.table_name
FROM information_schema.tables t
WHERE t.table_schema = 'public'
  AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name;

-- 2) Detalle completo de atributos por tabla
SELECT
  c.table_name AS tabla,
  c.ordinal_position AS orden_columna,
  c.column_name AS atributo,
  c.data_type AS tipo_dato,
  c.udt_name AS tipo_interno,
  c.character_maximum_length AS largo_maximo,
  c.numeric_precision AS precision_numerica,
  c.numeric_scale AS escala_numerica,
  c.is_nullable AS permite_null,
  c.column_default AS valor_default,
  CASE WHEN pk.column_name IS NOT NULL THEN 'SI' ELSE 'NO' END AS es_pk,
  CASE WHEN fk.column_name IS NOT NULL THEN 'SI' ELSE 'NO' END AS es_fk,
  fk.foreign_table_name AS fk_tabla_referenciada,
  fk.foreign_column_name AS fk_columna_referenciada
FROM information_schema.columns c
LEFT JOIN (
  SELECT
    ku.table_schema,
    ku.table_name,
    ku.column_name
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage ku
    ON tc.constraint_name = ku.constraint_name
   AND tc.table_schema = ku.table_schema
  WHERE tc.constraint_type = 'PRIMARY KEY'
) pk
  ON c.table_schema = pk.table_schema
 AND c.table_name = pk.table_name
 AND c.column_name = pk.column_name
LEFT JOIN (
  SELECT
    ku.table_schema,
    ku.table_name,
    ku.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage ku
    ON tc.constraint_name = ku.constraint_name
   AND tc.table_schema = ku.table_schema
  JOIN information_schema.constraint_column_usage ccu
    ON tc.constraint_name = ccu.constraint_name
   AND tc.table_schema = ccu.table_schema
  WHERE tc.constraint_type = 'FOREIGN KEY'
) fk
  ON c.table_schema = fk.table_schema
 AND c.table_name = fk.table_name
 AND c.column_name = fk.column_name
WHERE c.table_schema = 'public'
ORDER BY c.table_name, c.ordinal_position;

