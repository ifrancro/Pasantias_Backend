# Plan: eliminar los 500 por `LazyInitializationException` (solución de fondo)

## Contexto

Cinco endpoints devuelven `500 Internal Server Error` con el mensaje genérico `"Error interno del servidor"`
(`PATCH /api/clubes/{id}/activar|desactivar|aprobar`, `GET /api/membresias/{id}`, `GET /api/productos`).

No son tres bugs: es **un solo defecto estructural repetido**. El proyecto corre con
`spring.jpa.open-in-view=false` ([application.properties](src/main/resources/application.properties)), y los
mappers son clases estáticas que desreferencian relaciones `LAZY`:

```java
// ClubMapper.java:11 — el chequeo != null PASA (el proxy no es null),
// getNombre() dispara la inicialización sin sesión → LazyInitializationException
dto.setHubNombre(club.getHub() != null ? club.getHub().getNombre() : null);
```

La excepción cae en `handleGenericException` de
[GlobalExceptionHandler.java:210](src/main/java/com/example/herbalife_clubes/exceptions/GlobalExceptionHandler.java:210)
y se enmascara como 500 genérico.

**Resultado esperado:** los endpoints reportados responden correctamente, la clase de error queda cerrada en
los tres módulos, y quedan tests-guía que impiden que reaparezca.

---

## La causa raíz tiene DOS mitades

Esto es lo más importante del plan, porque `@EntityGraph` por sí solo **no** arregla las rutas de escritura.

### Mitad 1 — Lecturas: falta el fetch (`@EntityGraph` / `JOIN FETCH`)

`findById()` sin grafo devuelve proxies. Al cerrarse la sesión, el mapper revienta.
Aplica a **Membresías** y **Productos**, cuyos repositorios no tienen un solo `@EntityGraph`.

### Mitad 2 — Escrituras: falta `@Transactional`

Clubes **ya tiene** el `@EntityGraph` (commit `0c0972a`), y aun así los `PATCH` fallan. El motivo:

1. `clubRepository.findById(id)` corre en la transacción propia de `SimpleJpaRepository`. Carga `hub` y
   `anfitrion` inicializados, commitea y **cierra el contexto de persistencia** → entidad *detached*.
2. `clubRepository.save(club)` sobre una entidad detached ejecuta `em.merge()`. `merge` recarga la entidad por
   ID **sin aplicar el `@EntityGraph`**, y devuelve una **instancia gestionada nueva** cuyas asociaciones son
   proxies sin inicializar. Esa transacción también cierra.
3. `ClubMapper.mapClubToClubDTO(updatedClub)` mapea el resultado del `merge` → proxy sin sesión → **boom**.

Por eso `GET /api/clubes/{id}` funciona (tiene `@Transactional(readOnly = true)`) y los `PATCH` no.
El commit `0c0972a` arregló solo las rutas de lectura y dejó las de escritura intactas.

Con `@Transactional` en el método, los tres pasos comparten **un solo** contexto de persistencia: `findById`
devuelve una entidad *gestionada*, `save` es esencialmente un no-op (el dirty checking hace el flush), y el
mapeo ocurre con la sesión todavía abierta.

> **Regla del proyecto que este plan establece:** todo método de servicio que devuelva un DTO debe declarar
> `@Transactional` (con `readOnly = true` si no escribe), y toda consulta que alimente un mapper debe declarar
> su `@EntityGraph`. Las dos cosas, siempre.

---

## FASE 1 — Los 3 módulos reportados (esto es lo que se implementa)

### 1.1 Productos — el más roto (repositorio sin ningún grafo, servicio sin ninguna transacción)

**[ProductoRepository.java](src/main/java/com/example/herbalife_clubes/repositories/ProductoRepository.java)**
— añadir a los 10 finders el grafo que `ProductoMapper` necesita:

```java
@Override
@EntityGraph(attributePaths = {"hub", "clubCreador"})
Optional<Producto> findById(Integer id);
```

Métodos a anotar: `findAll`, `findById` (ambos con `@Override`), `findByHubId`,
`findByHubIdAndActivoTrue`, `findByClubCreadorId`, `findByEstadoAprobacion`, `findByEstadoAprobacionNot`,
`findByEstadoAprobacionAndClubCreadorId`, `findByHubIdAndTipoAndEstadoAprobacion`,
`findByClubCreadorIdAndTipoAndEstadoAprobacion`.

`Producto.hub` es `LAZY` **y** `nullable = false`: siempre es un proxy. Por eso `GET /api/productos` falla el
100% de las veces, en el primer elemento de la lista.

**[ProductoServiceImpl.java](src/main/java/com/example/herbalife_clubes/serviceimpls/ProductoServiceImpl.java)**
— tiene **cero** `@Transactional` en 23 métodos. Añadir `readOnly = true` a los 15 de lectura
(`getProducto`, `getProductoPublico`, `getProductos`, `getProductosPublicos`, `getProductosByClub*`,
`getProductosPendientes|Aprobados|Rechazados`, `getProductosByHub`, …) y `@Transactional` a las 8 escrituras
(`createProducto*`, `updateProducto`, `cambiarEstadoAprobacion`, `activarProducto`, `desactivarProducto`,
`toggleDisponibilidadEnClub`).

### 1.2 Membresías

**[MembresiaRepository.java](src/main/java/com/example/herbalife_clubes/repositories/MembresiaRepository.java)**
— grafo con **salto anidado**, porque el mapper lee `referidoPorMembresia.getUsuario().getNombre()`:

```java
@EntityGraph(attributePaths = {"usuario", "club", "nivel", "referidoPorMembresia.usuario"})
```

Aplicar a `findById` (`@Override`), `findAll` (`@Override`), `findByUsuarioId`, `findByClubId`,
`findByNumeroSocio`, `findByReferidoPorMembresiaId`.

Además, `findWithUsuarioClubByIds` ya trae `JOIN FETCH m.usuario` y `m.club` pero **no** `nivel` ni
`referidoPorMembresia.usuario`. Hoy no falla (esos métodos sí son transaccionales) pero produce **N+1**.
Ampliar la consulta con `LEFT JOIN FETCH m.nivel` — `LEFT` porque `nivel_id` es nullable y un `JOIN FETCH`
normal descartaría filas.

**[MembresiaServiceImpl.java](src/main/java/com/example/herbalife_clubes/serviceimpls/MembresiaServiceImpl.java)**
— solo 2 de 14 métodos son transaccionales. Añadir la anotación a `getMembresia`, `getMembresiaByUsuario`,
`getMembresiasByClub`, `getEstadoCombo`, `buscarMiembrosGlobal`, `getArbolReferidos` (lectura) y a
`createMembresia`, `cambiarEstado`, `cambiarNivel`, `actualizarPuntos`, `recalcularPuntosPorAsistencias`
(escritura).

### 1.3 Clubes — solo falta la mitad 2

**[ClubServiceImpl.java](src/main/java/com/example/herbalife_clubes/serviceimpls/ClubServiceImpl.java)** —
el repositorio ya está bien; **no se toca**. Añadir `@Transactional` a los 6 métodos de escritura:
`createClub`, `updateClub`, `aprobarClub`, `rechazarClub`, `activarClub`, `desactivarClub`.

`aprobarClub` gana algo extra: hoy hace tres transacciones sueltas (guardar club → cambiar rol del anfitrión →
enviar notificación). Si la notificación falla, el club queda aprobado y el rol a medias. Con `@Transactional`
pasa a ser atómico.

### 1.4 Tests-guía (replicando el patrón que ya existe en el repo)

El commit `0c0972a` dejó un andamiaje que conviene imitar tal cual:

| Tipo | Modelo a copiar | Nuevos |
|---|---|---|
| Reflexión sobre `@EntityGraph` (sin BD, siempre corre) | [ClubRepositoryEntityGraphTest.java](src/test/java/com/example/herbalife_clubes/repositories/ClubRepositoryEntityGraphTest.java) | `ProductoRepositoryEntityGraphTest`, `MembresiaRepositoryEntityGraphTest` |
| Integración real con Testcontainers + `Hibernate.isInitialized(...)` | [ClubRepositoryFetchIT.java](src/test/java/com/example/herbalife_clubes/repositories/ClubRepositoryFetchIT.java) | `ProductoRepositoryFetchIT`, `MembresiaRepositoryFetchIT` |
| Mapeo en el servicio (Mockito) | `ClubServiceMappingTest.java` | extender con las rutas `activar`/`desactivar`/`aprobar` |

Y un guard nuevo y barato que cubre la mitad 2, que hoy no tiene red de seguridad:
**`ServiceTransactionalGuardTest`** — por reflexión, afirma que todo método `public` de `ClubServiceImpl`,
`ProductoServiceImpl` y `MembresiaServiceImpl` que devuelve un DTO declara `@Transactional`
(`@Transactional` tiene retención `RUNTIME`, así que es verificable sin BD ni Docker).

Los ITs se auto-omiten sin Docker vía `@EnabledIf("dockerAvailable")`, y el `pom.xml` no filtra por tags,
así que **no hace falta tocar la configuración de build**.

---

## FASE 2 — Resto de módulos (documentado, NO se implementa ahora)

Mismo defecto, mismo remedio. Inventario verificado (relaciones `LAZY` que cada mapper desreferencia):

| Módulo | `attributePaths` del `@EntityGraph` | `@Transactional` en el service |
|---|---|---|
| Pedido | `membresia`, `club`, `producto`, `combo` | 5 de N ya lo tienen — completar |
| Consumo | `membresia`, `club`, `asistencia`, `pedido` | 1 — completar |
| Asistencia | `membresia`, `club` | 1 — completar |
| Notificacion | `hub`, `club`, `usuario`, `pedido` | **0** |
| Evento | `hub`, `club` | **0** |
| Hub | `admin` | **0** |
| FotoClub | `club` | **0** |
| SoporteTicket | `usuario` | 2 — completar |
| Logro | `clubCreador` | 2 — completar |
| Usuario | — (`Usuario.rol` es `EAGER`) | **0**, pero sin riesgo de lazy |

`UsuarioMapper` es el único seguro hoy: `Usuario.rol` está mapeado `EAGER`. Los demás tienen exactamente el
mismo agujero que Productos.

Sugerencia de orden para la Fase 2: **Pedido → Consumo → Asistencia** primero (son el núcleo transaccional
diario y comparten entidades con Membresía), y dejar Notificacion/Evento/Hub/FotoClub/SoporteTicket/Logro
para una tercera pasada.

---

## Verificación

**1. Sin Docker (rápido, siempre disponible):**

```bash
./mvnw test
```

Debe pasar la suite existente (incluidos `ClubMapperTest`, `ClubControllerTest`, `PublicClubControllerTest`)
más los nuevos tests de reflexión.

**2. Con Docker (verificación real del fetch):**

```bash
./mvnw verify
```

Levanta PostgreSQL 16 en Testcontainers. Los `*FetchIT` afirman `Hibernate.isInitialized(...)` **después** de
cerrado el contexto de persistencia y que el mapper no lanza — que es exactamente el fallo en producción.

**3. Confirmación end-to-end de los 5 endpoints del reporte** (con token ADMIN, contra local o Render):

```bash
curl -s -o /dev/null -w '%{http_code} %{url_effective}\n' -X PATCH -H "Authorization: Bearer $TOKEN" "$BASE/api/clubes/3/activar"
```

Repetir para `/desactivar`, `/aprobar`, `GET /api/membresias/1` y `GET /api/productos`. Los cinco deben
devolver `200` con el DTO poblado — en particular con `hubNombre`, `anfitrionNombre`, `clubNombre` y
`nivelNombre` **no nulos**, que es la prueba de que el fetch funciona y no solo de que dejó de explotar.

**4. Contra-regresión de N+1:** con `JPA_SHOW_SQL=true`, `GET /api/productos` debe emitir **una** consulta
con `JOIN`, no 1 + 2N.

---

## Qué mitiga y qué no

**Mitiga:**
- Los 500 de los 5 endpoints reportados y del resto de rutas de esos 3 módulos.
- El N+1 latente en los listados (ahora una consulta con `JOIN` en lugar de 1 + N).
- La no-atomicidad de `aprobarClub` / `rechazarClub`.
- Reaparición del defecto, vía los tests-guía.

**No mitiga (fuera de alcance, consciente):**
- Los módulos de la Fase 2 siguen expuestos.
- El enmascaramiento en `handleGenericException`: un `LazyInitializationException` futuro seguirá saliendo
  como 500 genérico. Vale considerar un `@ExceptionHandler(LazyInitializationException.class)` que loguee la
  ruta y la asociación — pero es una decisión aparte y no la incluyo aquí.
- No se cambia `open-in-view`. Dejarlo en `false` es lo correcto; activarlo escondería el problema.
