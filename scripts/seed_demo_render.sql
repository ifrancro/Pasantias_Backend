-- =====================================================
-- DATOS DE PRUEBA — Render / PostgreSQL (esquema actual)
-- =====================================================
--
-- ORDEN EN BD NUEVA (vacía):
--   1) scripts/recrear_bd_completa.sql   ← crea tablas, FKs e índices
--   2) scripts/seed_demo_render.sql      ← este archivo (limpia + inserta demo)
--
-- NO ejecutar en producción con datos reales.
-- Password demo para todos: password  (hash bcrypt abajo)
--
-- =====================================================

BEGIN;

-- Limpiar todo (incluye tablas nuevas: prospectos, misiones, compras_manuales, requisitos_logro)
TRUNCATE TABLE
  compras_manuales,
  misiones_prospecto,
  prospectos,
  membresia_logros,
  requisitos_logro,
  pedido_items,
  pedidos,
  asistencias,
  consumos,
  notificaciones,
  fotos_club,
  eventos,
  soporte_tickets,
  club_productos,
  membresias,
  logros,
  productos,
  clubes,
  hubs,
  usuarios,
  niveles_socio,
  roles
RESTART IDENTITY CASCADE;

-- =====================================================
-- 1) Catálogos base
-- =====================================================

INSERT INTO roles (id, nombre) VALUES
  (1, 'ADMIN'),
  (2, 'SOCIO'),
  (3, 'ANFITRION'),
  (4, 'USUARIO_BASICO');

INSERT INTO niveles_socio (id, nombre, visitas_requeridas, descripcion_beneficios) VALUES
  (1, 'SOCIO_NUEVO', 0, 'Beneficios básicos'),
  (2, 'SOCIO_CONSTANTE', 10, 'Beneficios por constancia'),
  (3, 'SOCIO_VIP', 25, 'Beneficios VIP');

-- password: password
INSERT INTO usuarios (id, rol_id, nombre, apellido, email, password_hash, telefono, fecha_nacimiento, estado, created_at)
VALUES
  (1, 1, 'Admin', 'Corporativo', 'admin@demo.com', '$2a$10$XUjXbhyb4qzmtmKRhJkeie9OAo7vjLUtYy0/kCKIBjFii9DMdnSGS', '70000001', '1990-01-01', 'ACTIVO', NOW()),
  (2, 3, 'Andrea', 'Anfitriona', 'anfitrion@demo.com', '$2a$10$XUjXbhyb4qzmtmKRhJkeie9OAo7vjLUtYy0/kCKIBjFii9DMdnSGS', '70000002', '1998-06-15', 'ACTIVO', NOW()),
  (3, 2, 'Carlos', 'Soto', 'socio1@demo.com', '$2a$10$XUjXbhyb4qzmtmKRhJkeie9OAo7vjLUtYy0/kCKIBjFii9DMdnSGS', '70000003', '2000-03-20', 'ACTIVO', NOW()),
  (4, 2, 'María', 'Rojas', 'socio2@demo.com', '$2a$10$XUjXbhyb4qzmtmKRhJkeie9OAo7vjLUtYy0/kCKIBjFii9DMdnSGS', '70000004', '2001-09-05', 'ACTIVO', NOW()),
  (5, 4, 'Pedro', 'Visitante', 'basico@demo.com', '$2a$10$XUjXbhyb4qzmtmKRhJkeie9OAo7vjLUtYy0/kCKIBjFii9DMdnSGS', '70000005', '2002-01-10', 'ACTIVO', NOW());

INSERT INTO hubs (id, admin_id, nombre, descripcion, estado, created_at)
VALUES
  (1, 1, 'HUB Santa Cruz', 'Administración Santa Cruz', 'ACTIVO', NOW());

INSERT INTO clubes (id, hub_id, anfitrion_id, nombre_club, direccion, horario, lat, lng, estado, prefijo_socio, created_at)
VALUES
  (1, 1, 2, 'Club Herbalife Demo 1', 'Av. Ejemplo #123, Santa Cruz', 'Lun-Sab 07:00-11:00 / 16:00-20:00', -17.77181708, -63.18720420, 'ACTIVO', 'SC', NOW());

-- Productos: el id 5 es un Combo (es_combo=true, requerido para validar asistencia).
-- El origen sigue siendo GLOBAL para que aparezca en el menú del club.
INSERT INTO productos (id, hub_id, nombre, descripcion, activo, tipo, es_combo, puntos_valor, precio, estado_aprobacion, created_at)
VALUES
  (1, 1, 'Batido Nutricional', 'Batido nutricional Herbalife', true, 'GLOBAL', false, 10, 20.00, 'APROBADO', NOW()),
  (2, 1, 'Té Energético', 'Té energético concentrado', true, 'GLOBAL', false, 5, 12.00, 'APROBADO', NOW()),
  (3, 1, 'Aloe Concentrado', 'Bebida de aloe concentrado', true, 'GLOBAL', false, 5, 10.00, 'APROBADO', NOW()),
  (4, 1, 'Proteína Personalizada', 'Batido con proteína adicional', true, 'GLOBAL', false, 15, 25.00, 'APROBADO', NOW()),
  (5, 1, 'Combo Batido + Té', 'Paquete combo para consumo en club', true, 'GLOBAL', true, 15, 30.00, 'APROBADO', NOW());

INSERT INTO club_productos (club_id, producto_id, disponible, created_at)
VALUES
  (1, 1, true, NOW()),
  (1, 2, true, NOW()),
  (1, 3, true, NOW()),
  (1, 4, true, NOW()),
  (1, 5, true, NOW());

INSERT INTO membresias (id, usuario_id, club_id, nivel_id, numero_socio, puntos_acumulados, como_conocio, fecha_registro, estado, racha_actual, racha_maxima)
VALUES
  (1, 3, 1, 1, 'SC-000001', 0, 'Facebook', NOW(), 'ACTIVA', 0, 0);

INSERT INTO membresias (id, usuario_id, club_id, nivel_id, numero_socio, puntos_acumulados, referido_por_membresia_id, como_conocio, fecha_registro, estado, racha_actual, racha_maxima)
VALUES
  (2, 4, 1, 1, 'SC-000002', 0, 1, 'Referido', NOW(), 'ACTIVA', 0, 0);

-- =====================================================
-- 2) Pedido ENTREGADO con COMBO (Carlos ya puede registrar asistencia)
-- =====================================================

INSERT INTO pedidos (id, membresia_id, club_id, tipo_consumo, observaciones, estado, fecha_pedido, producto_id, cantidad)
VALUES
  (1, 1, 1, 'EN_LUGAR', 'Combo consumido en club', 'ENTREGADO', NOW(), 5, 1);

INSERT INTO pedido_items (pedido_id, producto_id, cantidad, nota, precio_unitario, subtotal)
VALUES
  (1, 5, 1, 'Combo estándar', 30.00, 30.00);

-- =====================================================
-- 3) Ciclo de vida (prospecto + misión) — opcional demo
-- =====================================================

INSERT INTO prospectos (id, club_id, nombre, telefono, referido_por_membresia_id, fecha_creacion, estado)
VALUES
  (1, 1, 'Ana García', '77712345', 1, CURRENT_DATE, 'EN_SEGUIMIENTO');

INSERT INTO misiones_prospecto (prospecto_id, nombre, descripcion, meta_cantidad, progreso_actual, fecha_limite)
VALUES
  (1, 'Consumir 3 combos en una semana', 'Debe consumir el combo al menos 3 veces', 3, 0, CURRENT_DATE + 7);

-- =====================================================
-- 4) Logro de ejemplo con requisitos compuestos (esquema nuevo)
-- =====================================================

INSERT INTO logros (id, club_creador_id, nombre, descripcion, puntos_recompensa, fecha_inicio, fecha_fin, estado_aprobacion)
VALUES
  (1, 1, 'Socio constante', 'Asiste 5 veces al club', 50, CURRENT_DATE - 30, CURRENT_DATE + 365, 'APROBADO');

INSERT INTO requisitos_logro (logro_id, tipo_metrica, cantidad_esperada)
VALUES
  (1, 'ASISTENCIA', 5);

-- =====================================================
-- 5) Secuencias PostgreSQL
-- =====================================================

SELECT setval('roles_id_seq', (SELECT COALESCE(MAX(id), 1) FROM roles));
SELECT setval('niveles_socio_id_seq', (SELECT COALESCE(MAX(id), 1) FROM niveles_socio));
SELECT setval('usuarios_id_seq', (SELECT COALESCE(MAX(id), 1) FROM usuarios));
SELECT setval('hubs_id_seq', (SELECT COALESCE(MAX(id), 1) FROM hubs));
SELECT setval('clubes_id_seq', (SELECT COALESCE(MAX(id), 1) FROM clubes));
SELECT setval('productos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM productos));
SELECT setval('membresias_id_seq', (SELECT COALESCE(MAX(id), 1) FROM membresias));
SELECT setval('pedidos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM pedidos));
SELECT setval('pedido_items_id_seq', (SELECT COALESCE(MAX(id), 1) FROM pedido_items));
SELECT setval('prospectos_id_seq', (SELECT COALESCE(MAX(id), 1) FROM prospectos));
SELECT setval('misiones_prospecto_id_seq', (SELECT COALESCE(MAX(id), 1) FROM misiones_prospecto));
SELECT setval('logros_id_seq', (SELECT COALESCE(MAX(id), 1) FROM logros));
SELECT setval('requisitos_logro_id_seq', (SELECT COALESCE(MAX(id), 1) FROM requisitos_logro));

COMMIT;

-- =====================================================
-- Cuentas demo
-- =====================================================
-- admin@demo.com      / password  (ADMIN)
-- anfitrion@demo.com  / password  (ANFITRION, club 1)
-- socio1@demo.com     / password  (SOCIO Carlos, SC-000001, ya consumió COMBO)
-- socio2@demo.com     / password  (SOCIO María, SC-000002, sin combo aún)
-- basico@demo.com     / password  (USUARIO_BASICO, sin membresía)
