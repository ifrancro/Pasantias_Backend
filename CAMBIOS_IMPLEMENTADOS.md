# ✅ Cambios Implementados en el Backend

## 📋 Resumen de Cambios

Se han implementado 3 cambios principales solicitados:

1. ✅ **Rol USUARIO_BASICO agregado**
2. ✅ **Endpoints públicos para ubicaciones de clubes**
3. ✅ **Swagger configurado para endpoints públicos**

---

## 1️⃣ Rol USUARIO_BASICO

### Cambios Realizados:

#### ✅ DataInitializer actualizado
- Se agregó inicialización automática del rol `USUARIO_BASICO`
- El rol se crea automáticamente si no existe al arrancar la aplicación
- Es idempotente (no duplica si ya existe)

#### ✅ AuthServiceImpl actualizado
- **Antes:** Rol por defecto era `SOCIO`
- **Ahora:** Rol por defecto es `USUARIO_BASICO`
- **Seguridad:** El campo `rolId` enviado en el request de registro se **IGNORA completamente**
- Solo los administradores pueden asignar roles superiores (esto debe hacerse desde el backend directamente o mediante endpoints administrativos específicos)

### Flujo de Registro:

**POST** `/api/auth/register`

**Request Body:**
```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan@email.com",
  "password": "Password123!",
  "telefono": "+57 300 123 4567",
  "fechaNacimiento": "1995-11-08",
  "redesSociales": "@juan_perez"
}
```

**Nota:** El campo `rolId` si se envía será ignorado. Siempre se asignará `USUARIO_BASICO`.

**Response esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 5,
  "email": "juan@email.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  "rolNombre": "USUARIO_BASICO"
}
```

---

## 2️⃣ Endpoints Públicos para Ubicaciones de Clubes

### Nuevos Endpoints Creados:

#### ✅ GET `/api/public/clubes`
Obtiene la lista de todos los clubes con estado `ACTIVO`.

**Método:** `GET`  
**Autenticación:** ❌ **NO REQUERIDA** (Público)  
**Headers:** Ninguno requerido

**Response esperada:**
```json
[
  {
    "id": 1,
    "hubId": 1,
    "hubNombre": "HUB Valle del Cauca",
    "anfitrionId": 2,
    "anfitrionNombre": "María González",
    "nombreClub": "Club Herbalife Cali Centro",
    "direccion": "Calle 10 # 5-32, Cali, Valle del Cauca",
    "horario": "Lunes a Viernes: 8:00 AM - 6:00 PM, Sábados: 9:00 AM - 2:00 PM",
    "lat": 3.4516,
    "lng": -76.5320,
    "estado": "ACTIVO",
    "createdAt": "2024-01-15T11:00:00"
  },
  {
    "id": 2,
    "hubId": 1,
    "hubNombre": "HUB Valle del Cauca",
    "anfitrionId": 2,
    "anfitrionNombre": "María González",
    "nombreClub": "Club Herbalife Cali Norte",
    "direccion": "Avenida 6N # 28N-45, Cali, Valle del Cauca",
    "horario": "Lunes a Sábado: 7:00 AM - 7:00 PM",
    "lat": 3.4678,
    "lng": -76.5234,
    "estado": "ACTIVO",
    "createdAt": "2024-01-16T10:00:00"
  }
]
```

**Nota:** Solo se devuelven clubes con `estado: "ACTIVO"`. Los clubes con otros estados (PENDIENTE, APROBADO, RECHAZADO, INACTIVO) no se muestran.

---

#### ✅ GET `/api/public/clubes/{id}`
Obtiene el detalle de un club específico solo si está `ACTIVO`.

**Método:** `GET`  
**Autenticación:** ❌ **NO REQUERIDA** (Público)  
**Headers:** Ninguno requerido  
**Path Variable:** `id` (Integer) - ID del club

**Ejemplo:** `GET /api/public/clubes/1`

**Response esperada (si el club está ACTIVO):**
```json
{
  "id": 1,
  "hubId": 1,
  "hubNombre": "HUB Valle del Cauca",
  "anfitrionId": 2,
  "anfitrionNombre": "María González",
  "nombreClub": "Club Herbalife Cali Centro",
  "direccion": "Calle 10 # 5-32, Cali, Valle del Cauca",
  "horario": "Lunes a Viernes: 8:00 AM - 6:00 PM, Sábados: 9:00 AM - 2:00 PM",
  "lat": 3.4516,
  "lng": -76.5320,
  "estado": "ACTIVO",
  "createdAt": "2024-01-15T11:00:00"
}
```

**Response esperada (si el club NO está ACTIVO o no existe):**
```json
{
  "success": false,
  "message": "Club activo no encontrado con id: 1",
  "data": null,
  "timestamp": "2024-01-20T10:30:00"
}
```

**Status Code:** `404 Not Found`

---

### Cambios en el Repositorio:

Se agregaron nuevos métodos en `ClubRepository`:

```java
List<Club> findByEstado(String estado);
Optional<Club> findByIdAndEstado(Integer id, String estado);
```

### Cambios en el Service:

Se agregaron nuevos métodos en `ClubService` y `ClubServiceImpl`:

```java
List<ClubDTO> getClubesActivos();
ClubDTO getClubActivo(Integer clubId);
```

---

### Cambios en SecurityConfig:

Se actualizó `SecurityConfig` para permitir acceso público a los endpoints públicos:

```java
.requestMatchers(
    "/api/auth/login",
    "/api/auth/register",
    "/api/public/**",  // ← NUEVO: Endpoints públicos
    "/swagger-ui/**",
    "/v3/api-docs/**",
    // ...
).permitAll()
```

---

## 3️⃣ Endpoints Protegidos vs Públicos

### 🔓 **Endpoints Públicos (Sin Autenticación):**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | Iniciar sesión |
| `GET` | `/api/public/clubes` | Listar clubes activos |
| `GET` | `/api/public/clubes/{id}` | Detalle de club activo |
| `GET` | `/swagger-ui/**` | Documentación Swagger |
| `GET` | `/v3/api-docs/**` | Especificación OpenAPI |

---

### 🔒 **Endpoints Protegidos (Requieren JWT):**

#### Endpoints de Clubes (Administración):
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/clubes` | Crear nuevo club (requiere autenticación) |
| `GET` | `/api/clubes` | Listar todos los clubes (incluyendo no activos) |
| `GET` | `/api/clubes/{id}` | Detalle de cualquier club (cualquier estado) |
| `PUT` | `/api/clubes/{id}` | Actualizar club |
| `PATCH` | `/api/clubes/{id}/aprobar` | Aprobar club (admin) |
| `PATCH` | `/api/clubes/{id}/rechazar` | Rechazar club (admin) |
| `PATCH` | `/api/clubes/{id}/activar` | Activar club |
| `PATCH` | `/api/clubes/{id}/desactivar` | Desactivar club |

**Nota:** Los endpoints de administración (`/api/clubes/**`) siguen protegidos. Solo los endpoints públicos (`/api/public/clubes/**`) son accesibles sin autenticación.

---

## 🧪 Pruebas en Swagger

### Verificar que los Endpoints Públicos Funcionan:

1. **Abrir Swagger:**
   ```
   https://tu-api.onrender.com/swagger-ui.html
   ```

2. **Probar sin autenticación:**
   - Buscar el grupo `Public Club Controller`
   - Probar `GET /api/public/clubes` - Debe funcionar sin token
   - Probar `GET /api/public/clubes/{id}` - Debe funcionar sin token

3. **Verificar que endpoints protegidos siguen protegidos:**
   - Intentar `POST /api/clubes` sin token - Debe retornar `401 Unauthorized`
   - Hacer login primero, obtener token
   - Usar el token para acceder a endpoints protegidos

---

## 📊 Comparación: Endpoints Públicos vs Protegidos

| Característica | `/api/public/clubes` | `/api/clubes` |
|----------------|---------------------|---------------|
| **Autenticación** | ❌ No requerida | ✅ JWT requerido |
| **Estados mostrados** | Solo `ACTIVO` | Todos los estados |
| **Uso** | Visitantes/público | Administradores/anfitriones |
| **Información sensible** | No incluye datos internos | Incluye todos los datos |
| **Swagger** | Visible sin autenticación | Requiere token |

---

## 🎯 Casos de Uso

### Caso 1: Visitante sin login quiere ver clubes cercanos
```
GET /api/public/clubes
→ Ve todos los clubes activos con ubicación (lat/lng)
→ Puede mostrar en un mapa
→ No necesita crear cuenta
```

### Caso 2: Usuario registrado (USUARIO_BASICO) busca club
```
GET /api/public/clubes/{id}
→ Ve detalles del club activo
→ Puede decidir si quiere unirse (requeriría membresía)
```

### Caso 3: Anfitrión quiere ver todos sus clubes (incluso pendientes)
```
GET /api/clubes?hubId=1 (con token JWT)
→ Ve todos los clubes del hub, incluyendo pendientes/aprobados
→ Puede gestionar sus clubes
```

---

## ✅ Checklist de Implementación

- [x] Rol `USUARIO_BASICO` creado en `DataInitializer`
- [x] `AuthServiceImpl` actualizado para asignar `USUARIO_BASICO` por defecto
- [x] Campo `rolId` en register se ignora (no puede elevar permisos)
- [x] Métodos agregados en `ClubRepository` para buscar por estado
- [x] Métodos agregados en `ClubService` para obtener clubes activos
- [x] `PublicClubController` creado con endpoints públicos
- [x] `SecurityConfig` actualizado para permitir `/api/public/**`
- [x] Endpoints protegidos mantienen autenticación
- [x] Swagger muestra endpoints públicos correctamente

---

## 🚀 Próximos Pasos para Deploy

1. **Hacer commit y push:**
   ```bash
   git add .
   git commit -m "feat: Agregar rol USUARIO_BASICO y endpoints públicos para clubes"
   git push origin main
   ```

2. **Render desplegará automáticamente**

3. **Verificar en Swagger:**
   - Probar endpoints públicos sin autenticación
   - Verificar que endpoints protegidos siguen funcionando con token

4. **Verificar roles:**
   - Registrar un nuevo usuario
   - Confirmar que se asigna rol `USUARIO_BASICO`
   - Verificar en logs que el rol se inicializó correctamente

---

¡Listo para probar! 🎉

