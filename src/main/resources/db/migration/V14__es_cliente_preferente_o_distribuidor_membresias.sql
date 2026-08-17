-- Declaración legal capturada en la activación de socio:
-- ¿Usted, su cónyuge o pareja de vida actualmente es cliente preferente
-- o distribuidor independiente de Herbalife?
-- true = SÍ, false = NO.
-- NULL = registro histórico (nunca respondió). Nullable a propósito:
-- sin valor por defecto, para no falsear socios ya activados.

ALTER TABLE membresias
    ADD COLUMN IF NOT EXISTS es_cliente_preferente_o_distribuidor BOOLEAN;

COMMENT ON COLUMN membresias.es_cliente_preferente_o_distribuidor IS
    'Declaracion legal en activacion: true=SI (cliente preferente o distribuidor independiente Herbalife), false=NO, NULL=historico';
