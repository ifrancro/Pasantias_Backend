# Contexto del Proyecto: Pasantias_Backend (Herbalife Clubes)

Este documento está diseñado para proporcionar contexto completo sobre la arquitectura, estructura y flujo de datos del proyecto **Pasantias_Backend** a un asistente de IA (como Claude), facilitando la depuración y resolución de errores HTTP 500.

---

## 1. Stack Tecnológico y Arquitectura General
El proyecto es una API RESTful monolítica desarrollada en **Java 17** con **Spring Boot 3**.
*   **Gestión de dependencias:** Maven (`pom.xml`)
*   **Base de Datos:** PostgreSQL
*   **Migraciones:** Flyway
*   **Mapeo de Objetos:** MapStruct y Lombok
*   **Seguridad:** Spring Security con JSON Web Tokens (JWT) y Google OAuth2
*   **Documentación:** Swagger (OpenAPI 3)

### Estructura de Directorios (`src/main/java/com/example/herbalife_clubes/`)
Sigue un patrón de arquitectura por capas estándar (MVC):
*   `controllers/`: Controladores REST. Exponen los endpoints de la API.
*   `services/`: Interfaces que definen los contratos de negocio.
*   `serviceimpls/`: Implementaciones concretas de la lógica de negocio (`@Service`).
*   `repositories/`: Interfaces que extienden de `JpaRepository` para el acceso a datos (`@Repository`).
*   `entities/`: Clases de modelo que representan las tablas de PostgreSQL (`@Entity`).
*   `dtos/`: Data Transfer Objects para request/response.
*   `mappers/`: Interfaces MapStruct para convertir entre Entidades y DTOs.
*   `security/`: Configuración de Spring Security, filtros JWT, autenticación.
*   `exceptions/`: Excepciones personalizadas y el `GlobalExceptionHandler` (`@ControllerAdvice`).

---

## 2. Modelos Principales y Relaciones (Dominio)
El dominio gira en torno a la gestión de Clubes de Nutrición (Herbalife), control de asistencia, consumo de productos y gestión de membresías de socios.

*   **Usuarios / Roles:** Un `Usuario` tiene un `Rol` (e.g., `ADMIN`, `ANFITRION`, `SOCIO`, `USUARIO_BASICO`).
*   **Hub:** Una región o grupo que agrupa varios clubes (pertenece a un Admin).
*   **Club:** Un lugar físico. Pertenece a un `Hub` y es administrado por un `Anfitrion` (Usuario).
*   **Producto / Sabor / Combo:** Productos disponibles en un Club.
*   **Membresía (Socio):** La relación entre un Usuario (Socio) y un Club específico, con un `NivelSocio` (ej. Bronce, Plata) y Puntos Acumulados.
*   **Asistencia / Consumo / Pedido:** Transacciones diarias de los socios en un club.
*   **Logros:** Sistema de gamificación / recompensas (`MembresiaLogro`).

---

## 3. Seguridad, Roles y Flujo de Autenticación
La seguridad está basada en JWT (Bearer tokens).

*   **Registro (`POST /api/auth/register`):** 
    *   **Importante:** Por defecto, cualquier usuario registrado recibe el rol **`USUARIO_BASICO`**. El campo `rolId` enviado en el request es **ignorado** por seguridad en `AuthServiceImpl`.
    *   Para crear un Admin o Anfitrión, se debe actualizar el rol posteriormente desde un usuario con permisos administrativos.
*   **Acceso Protegido vs Público:**
    *   Endpoints públicos: `/api/auth/register`, `/api/auth/login`, y `/api/public/**` (ej. para ver clubes en un mapa sin estar logueado).
    *   El resto de los endpoints requieren el header `Authorization: Bearer <token>`.

---

## 4. Estructura de Endpoints
La API expone los siguientes controladores principales (listados a alto nivel). La mayoría siguen el estándar CRUD (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`).

*   **Autenticación:** `/api/auth/*` (register, login, me)
*   **Usuarios:** `/api/usuarios/*` (perfil, desactivar)
*   **Hubs:** `/api/hubs/*`
*   **Clubes:** `/api/clubes/*` (requiere autenticación) y `/api/public/clubes/*` (público, solo clubes con estado ACTIVO).
*   **Productos / Combos / Sabores:** `/api/productos/*`, `/api/combos/*`, `/api/sabores/*`
*   **Membresías / Niveles:** `/api/membresias/*`, `/api/niveles-socio/*`
*   **Transaccionales:** `/api/asistencias/*`, `/api/consumos/*`, `/api/pedidos/*`
*   **Gamificación:** `/api/logros/*`, `/api/membresia-logros/*`
*   **Utilidades:** `/api/eventos/*`, `/api/notificaciones/*`, `/api/soporte-tickets/*`, `/api/fotos-club/*`, `/api/qr/*`, `/api/reportes/*`

Muchos endpoints de creación (POST) utilizan **Query Parameters** para resolver relaciones. Por ejemplo:
`POST /api/clubes?hubId=1&anfitrionId=2` (Body: `ClubDTO`)
`POST /api/membresias?usuarioId=3&clubId=1` (Body: `MembresiaDTO`)

---

## 5. Notas para la Depuración (Claude, lee esto con atención)
El desarrollador ha reportado **Errores HTTP 500 (Internal Server Error)** al ejecutar ciertos endpoints desde Swagger. Aquí tienes el contexto necesario para identificar el problema rápidamente:

1.  **Manejo Global de Excepciones:** 
    *   El archivo `GlobalExceptionHandler.java` maneja varias excepciones conocidas (`ResourceNotFoundException`, `DataIntegrityViolationException`, `MethodArgumentNotValidException`, etc.).
    *   Sin embargo, captura las excepciones no controladas en el método `handleGenericException(Exception ex)`.
    *   Si se retorna un HTTP 500 con el mensaje genérico *"Error interno del servidor"*, significa que se arrojó una **excepción no comprobada (Runtime) que no fue atrapada específicamente**, probablemente un `NullPointerException` (NPE), `IllegalArgumentException` no contemplada, un error en MapStruct, o fallos de cascada de Hibernate no mapeados como `DataIntegrity`.

2.  **Causas más comunes de 500 en esta arquitectura:**
    *   **Parámetros / Entidades Nulas en Mappers:** Si un endpoint recibe o devuelve una entidad a través de un DTO y un atributo relacional (ej. `club.getHub()`) es nulo, y el MapStruct intenta hacer `club.getHub().getId()`, causará un `NullPointerException`.
    *   **Violaciones de Constraints (Foreign Keys):** Aunque se intentan manejar (`handleDataIntegrity`), ciertas operaciones pueden fallar en base de datos si los `Query Parameters` como `?hubId=99` referencian IDs que no existen, y el código no valida su existencia (con `findById`) antes de persistir, pasándole un proxy vacío a Hibernate que falla en tiempo de commit.
    *   **Extracción de Usuarios de SecurityContext:** Si un endpoint intenta leer el usuario autenticado (ej. casteando el `Principal` o el `Authentication.getPrincipal()`) y el formato o el tipo no coincide, arroja `ClassCastException`.
    *   **Problemas de Estado (Estados no esperados):** Muchos flujos dependen de "Estados" (String). E.g., `ACTIVO`, `PENDIENTE`. Si un DTO carece del estado o envía un enum que no coincide y no se valida antes de grabar, la DB tira error.

**Para depurar:** Pide al usuario que te muestre los logs de la consola del backend (`log.error("Error interno no controlado ({})", ex.getClass().getSimpleName());`), ya que esto apuntará directamente a la línea exacta (Stacktrace) del NullPointerException o DataException que está causando el 500.
