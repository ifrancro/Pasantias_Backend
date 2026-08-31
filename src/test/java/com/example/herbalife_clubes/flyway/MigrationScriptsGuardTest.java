package com.example.herbalife_clubes.flyway;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MigrationScriptsGuardTest {

    private static final Path MIGRATIONS =
            Path.of("src/main/resources/db/migration");

    @Test
    void v13ExistsAndCreatesExpectedIndexes() throws Exception {
        Path v13 = MIGRATIONS.resolve("V13__indices_paginacion_pedidos_membresias.sql");
        assertTrue(Files.exists(v13));
        String sql = Files.readString(v13, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("idx_pedidos_club_fecha_id"));
        assertTrue(sql.contains("idx_pedidos_membresia_fecha_id"));
        assertTrue(sql.contains("idx_membresias_club_fecha_id"));
        assertTrue(sql.contains("if not exists"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void v14AddsNullableBooleanOnMembresiasWithoutDefaultFalse() throws Exception {
        Path v14 = MIGRATIONS.resolve("V14__es_cliente_preferente_o_distribuidor_membresias.sql");
        assertTrue(Files.exists(v14));
        String sql = Files.readString(v14, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table membresias"));
        assertTrue(sql.contains("es_cliente_preferente_o_distribuidor"));
        assertTrue(sql.contains("boolean"));
        assertTrue(sql.contains("if not exists"));
        assertFalse(sql.contains("default false"),
                "No usar DEFAULT false: los históricos deben quedar NULL");
        assertFalse(sql.contains("not null"),
                "La columna debe ser nullable para registros históricos");
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void v15CreatesVerificationCodesIdempotently() throws Exception {
        Path v15 = MIGRATIONS.resolve("V15__verification_codes.sql");
        assertTrue(Files.exists(v15), "Falta la migración de verification_codes");
        String sql = Files.readString(v15, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("create table if not exists verification_codes"),
                "Debe ser idempotente: la tabla ya existe en producción");
        assertTrue(sql.contains("usuario_id integer not null references usuarios(id)"));
        assertTrue(sql.contains("code varchar(6) not null"));
        assertTrue(sql.contains("expires_at timestamp not null"));
        assertTrue(sql.contains("used boolean not null default false"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("insert into"));
    }

    @Test
    void v16AddsPurposeFailedAttemptsAndPasswordResetTokens() throws Exception {
        Path v16 = MIGRATIONS.resolve("V16__verification_codes_purpose_and_password_reset.sql");
        assertTrue(Files.exists(v16), "Falta la migración V16 de password reset");
        String sql = Files.readString(v16, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("purpose varchar(32)"));
        assertTrue(sql.contains("email_verification"));
        assertTrue(sql.contains("failed_attempts integer"));
        assertTrue(sql.contains("idx_verification_codes_usuario_purpose_used"));
        assertTrue(sql.contains("idx_verification_codes_usuario_purpose_created"));
        assertTrue(sql.contains("create table if not exists password_reset_tokens"));
        assertTrue(sql.contains("token_hash"));
        assertFalse(sql.contains("insert into"));
    }

    @Test
    void migrationsDoNotContainSecrets() throws Exception {
        assertTrue(Files.isDirectory(MIGRATIONS));
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<Path> sqlFiles = files.filter(p -> p.toString().endsWith(".sql")).toList();
            assertFalse(sqlFiles.isEmpty());
            for (Path file : sqlFiles) {
                String content = Files.readString(file, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                assertFalse(content.contains("password="), file + " no debe contener password=");
                assertFalse(content.contains("jwt"), file + " no debe contener jwt");
                assertFalse(content.contains("api_key"), file + " no debe contener api_key");
                assertFalse(content.contains("begin rsa private"), file + " no debe contener claves privadas");
            }
        }
    }

    @Test
    void v17AddsNullableRevisionColumnsOnProductos() throws Exception {
        Path v17 = MIGRATIONS.resolve("V17__revision_productos.sql");
        assertTrue(Files.exists(v17), "Falta la migración V17 de revisión de productos");
        String sql = Files.readString(v17, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table productos"));
        assertTrue(sql.contains("comentario_revision"));
        assertTrue(sql.contains("revisado_por_usuario_id"));
        assertTrue(sql.contains("revisado_at"));
        assertTrue(sql.contains("if not exists"));
        assertTrue(sql.contains("references usuarios(id)"));
        assertFalse(sql.contains("comentario_revision text not null"),
                "comentario_revision debe ser nullable");
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void v18CreatesProductoGruposYOpciones() throws Exception {
        Path v18 = MIGRATIONS.resolve("V18__producto_grupos_opciones.sql");
        assertTrue(Files.exists(v18), "Falta la migración V18 de grupos de opciones");
        String sql = Files.readString(v18, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("create table if not exists producto_grupos_opciones"));
        assertTrue(sql.contains("create table if not exists producto_opciones"));
        assertTrue(sql.contains("references productos(id) on delete cascade"));
        assertTrue(sql.contains("references producto_grupos_opciones(id) on delete cascade"));
        assertTrue(sql.contains("min_selecciones"));
        assertTrue(sql.contains("max_selecciones"));
        assertTrue(sql.contains("permite_repetir"));
        assertTrue(sql.contains("uq_pgo_producto_nombre"));
        assertTrue(sql.contains("uq_po_grupo_nombre"));
        assertFalse(sql.contains("club_producto_opciones"));
        assertFalse(sql.contains("pedido_item_opciones"));
        assertFalse(sql.contains("precio_extra"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("insert into"));
    }

    @Test
    void v19AddsNullablePrecioVentaOnClubProductos() throws Exception {
        Path v19 = MIGRATIONS.resolve("V19__precio_venta_club_productos.sql");
        assertTrue(Files.exists(v19), "Falta la migración V19 de precio_venta");
        String sql = Files.readString(v19, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table club_productos"));
        assertTrue(sql.contains("precio_venta"));
        assertTrue(sql.contains("decimal(10, 2)") || sql.contains("decimal(10,2)"));
        assertTrue(sql.contains("chk_club_productos_precio_venta"));
        assertTrue(sql.contains("precio_venta is null or precio_venta >= 0"));
        assertFalse(sql.contains("not null"), "precio_venta debe ser nullable");
        assertFalse(sql.contains("default 0"), "No backfill/default a 0");
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("update productos"));
    }

    @Test
    void v20CreatesPedidoItemOpcionesWithNullableFkAndSnapshots() throws Exception {
        Path v20 = MIGRATIONS.resolve("V20__pedido_item_opciones.sql");
        assertTrue(Files.exists(v20), "Falta la migración V20 de pedido_item_opciones");
        String sql = Files.readString(v20, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("create table if not exists pedido_item_opciones"));
        assertTrue(sql.contains("references pedido_items(id) on delete cascade"));
        assertTrue(sql.contains("references producto_grupos_opciones(id) on delete set null"));
        assertTrue(sql.contains("references producto_opciones(id) on delete set null"));
        assertTrue(sql.contains("grupo_nombre_snapshot"));
        assertTrue(sql.contains("opcion_nombre_snapshot"));
        assertTrue(sql.contains("grupo_orden_snapshot"));
        assertTrue(sql.contains("opcion_orden_snapshot"));
        assertTrue(sql.contains("chk_pio_cantidad"));
        assertTrue(sql.contains("cantidad > 0"));
        assertTrue(sql.contains("uq_pio_item_opcion"));
        assertFalse(sql.contains("precio_extra"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("insert into"));
    }

    @Test
    void v21AddsComboPrecioAndPedidoCombos() throws Exception {
        Path v21 = MIGRATIONS.resolve("V21__precio_y_pedidos_combos.sql");
        assertTrue(Files.exists(v21), "Falta la migración V21 de combos/pedido_combos");
        String sql = Files.readString(v21, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table combos"));
        assertTrue(sql.contains("precio"));
        assertTrue(sql.contains("chk_combos_precio"));
        assertTrue(sql.contains("create table if not exists pedido_combos"));
        assertTrue(sql.contains("references pedidos(id) on delete cascade"));
        assertTrue(sql.contains("references combos(id) on delete set null"));
        assertTrue(sql.contains("combo_nombre_snapshot"));
        assertTrue(sql.contains("precio_unitario_snapshot"));
        assertTrue(sql.contains("subtotal_snapshot"));
        assertTrue(sql.contains("puntos_valor_snapshot"));
        assertTrue(sql.contains("alter table pedido_items"));
        assertTrue(sql.contains("pedido_combo_id"));
        assertTrue(sql.contains("references pedido_combos(id) on delete cascade"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void v22AddsNullableClientOrderIdWithPartialUniqueIndex() throws Exception {
        Path v22 = MIGRATIONS.resolve("V22__pedidos_client_order_id.sql");
        assertTrue(Files.exists(v22), "Falta la migración V22 de client_order_id");
        String sql = Files.readString(v22, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        assertTrue(sql.contains("alter table pedidos"));
        assertTrue(sql.contains("client_order_id"));
        assertTrue(sql.contains("varchar(36)"));
        assertTrue(sql.contains("if not exists"));
        assertTrue(sql.contains("uq_pedidos_client_order_id"));
        assertTrue(sql.contains("where client_order_id is not null"));
        assertFalse(sql.contains("unique(membresia_id, client_order_id"));
        assertFalse(sql.contains("insert into"));
        assertFalse(sql.contains("drop table"));
    }

    @Test
    void versionsAreUniqueAndIncludeV13AndV14() throws Exception {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            List<String> names = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("V\\d+__.+\\.sql"))
                    .sorted()
                    .toList();
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V13__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V14__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V16__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V17__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V18__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V19__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V20__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V21__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V22__")));
            assertTrue(names.stream().anyMatch(n -> n.startsWith("V2__")));
            assertFalse(names.stream().anyMatch(n -> n.startsWith("V1__")),
                    "No existe V1 histórico; no inventar una sin estrategia");
            long distinct = names.stream().map(n -> n.replaceFirst("V(\\d+)__.*", "$1")).distinct().count();
            assertEquals(names.size(), distinct, "No debe haber versiones Flyway duplicadas");
        }
    }

    @Test
    void flywayDependencyPresentInPom() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("spring-boot-starter-flyway"));
        assertTrue(pom.contains("flyway-database-postgresql"));
        assertTrue(pom.contains("testcontainers"));
    }
}
