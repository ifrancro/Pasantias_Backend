# 🧪 Datos de Prueba para Swagger/Postman

Este documento contiene todos los JSON necesarios para probar el sistema en Swagger, organizados por flujo de usuario.

---

## ⚠️ IMPORTANTE: IDs de Roles

**Antes de usar estos datos, debes obtener los IDs de los roles desde tu base de datos.**

Los roles típicos son:
- `ADMIN` (generalmente ID: 1)
- `SOCIO` (generalmente ID: 2)  
- `ANFITRION` (generalmente ID: 3)

**Para obtener los IDs reales**, consulta la tabla `roles` en tu base de datos o usa el endpoint `/api/usuarios` después de crear usuarios y verificar sus roles.

---

## 🔐 1. Usuario Administrador Corporativo

### 1.1 Registrar Admin

**Endpoint:** `POST /api/auth/register`

**Headers:** `Content-Type: application/json`

**Body:**
```json
{
  "nombre": "Carlos",
  "apellido": "Ramírez",
  "email": "admin@herbalife.com",
  "password": "Admin123!",
  "rolId": 1,
  "telefono": "+57 300 123 4567",
  "fechaNacimiento": "1985-03-15",
  "redesSociales": "@admin_herbalife"
}
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "admin@herbalife.com",
  "nombre": "Carlos",
  "apellido": "Ramírez",
  "rolNombre": "ADMIN"
}
```

**Nota:** Guarda el `token` y el `userId` para usar en requests posteriores.

---

### 1.2 Login Admin

**Endpoint:** `POST /api/auth/login`

**Headers:** `Content-Type: application/json`

**Body:**
```json
{
  "email": "admin@herbalife.com",
  "password": "Admin123!"
}
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "admin@herbalife.com",
  "nombre": "Carlos",
  "apellido": "Ramírez",
  "rolNombre": "ADMIN"
}
```

---

## 👤 2. Usuario Anfitrión (Flujo Completo)

### 2.1 Registrar Anfitrión (Inicial como USUARIO/SOCIO)

**Endpoint:** `POST /api/auth/register`

**Headers:** `Content-Type: application/json`

**Body (sin rolId, se asigna SOCIO por defecto):**
```json
{
  "nombre": "María",
  "apellido": "González",
  "email": "maria.anfitrion@email.com",
  "password": "Anfitrion123!",
  "telefono": "+57 310 987 6543",
  "fechaNacimiento": "1990-07-22",
  "redesSociales": "@maria_club_herbalife"
}
```

**O si quieres especificar rol SOCIO:**
```json
{
  "nombre": "María",
  "apellido": "González",
  "email": "maria.anfitrion@email.com",
  "password": "Anfitrion123!",
  "rolId": 2,
  "telefono": "+57 310 987 6543",
  "fechaNacimiento": "1990-07-22",
  "redesSociales": "@maria_club_herbalife"
}
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 2,
  "email": "maria.anfitrion@email.com",
  "nombre": "María",
  "apellido": "González",
  "rolNombre": "SOCIO"
}
```

**Nota:** Guarda el `userId` (ej: 2) y el `token` para usar después.

---

### 2.2 Login Anfitrión

**Endpoint:** `POST /api/auth/login`

**Headers:** `Content-Type: application/json`

**Body:**
```json
{
  "email": "maria.anfitrion@email.com",
  "password": "Anfitrion123!"
}
```

---

### 2.3 Crear HUB (Primero, como Admin)

**Endpoint:** `POST /api/hubs?adminId=1`

**Headers:** 
- `Content-Type: application/json`
- `Authorization: Bearer {token_del_admin}`

**Body:**
```json
{
  "nombre": "HUB Valle del Cauca",
  "descripcion": "Hub regional para el departamento del Valle del Cauca, incluyendo Cali y municipios aledaños",
  "estado": "ACTIVO"
}
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "adminId": 1,
  "adminNombre": "Carlos Ramírez",
  "nombre": "HUB Valle del Cauca",
  "descripcion": "Hub regional para el departamento del Valle del Cauca, incluyendo Cali y municipios aledaños",
  "estado": "ACTIVO",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Nota:** Guarda el `id` del hub (ej: 1) para crear clubes.

---

### 2.4 Crear Club (Como Anfitrión)

**Endpoint:** `POST /api/clubes?hubId=1&anfitrionId=2`

**Headers:** 
- `Content-Type: application/json`
- `Authorization: Bearer {token_del_anfitrion}`

**Body:**
```json
{
  "nombreClub": "Club Herbalife Cali Centro",
  "direccion": "Calle 10 # 5-32, Cali, Valle del Cauca",
  "horario": "Lunes a Viernes: 8:00 AM - 6:00 PM, Sábados: 9:00 AM - 2:00 PM",
  "lat": 3.4516,
  "lng": -76.5320,
  "estado": "PENDIENTE"
}
```

**Respuesta esperada:**
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
  "estado": "PENDIENTE",
  "createdAt": "2024-01-15T11:00:00"
}
```

**Nota:** Guarda el `id` del club (ej: 1). El estado inicial es `PENDIENTE`.

---

### 2.5 Aprobar Club (Como Admin)

**Endpoint:** `PATCH /api/clubes/1/aprobar`

**Headers:** 
- `Authorization: Bearer {token_del_admin}`

**Body:** (vacío, no requiere body)

**Respuesta esperada:**
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
  "estado": "APROBADO",
  "createdAt": "2024-01-15T11:00:00"
}
```

**Nota:** Después de aprobar, el anfitrión debería cambiar su rol a `ANFITRION` (esto podría requerir un endpoint adicional o actualización manual en la base de datos).

---

## 🏃 3. Usuario Socio

### 3.1 Registrar Socio

**Endpoint:** `POST /api/auth/register`

**Headers:** `Content-Type: application/json`

**Body:**
```json
{
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan.socio@email.com",
  "password": "Socio123!",
  "telefono": "+57 320 555 7890",
  "fechaNacimiento": "1995-11-08",
  "redesSociales": "@juan_perez_fit"
}
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 3,
  "email": "juan.socio@email.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  "rolNombre": "SOCIO"
}
```

**Nota:** Guarda el `userId` (ej: 3) y el `token`.

---

### 3.2 Login Socio

**Endpoint:** `POST /api/auth/login`

**Headers:** `Content-Type: application/json`

**Body:**
```json
{
  "email": "juan.socio@email.com",
  "password": "Socio123!"
}
```

---

### 3.3 Crear Membresía (Como Anfitrión para el Socio)

**Endpoint:** `POST /api/membresias?usuarioId=3&clubId=1&nivelId=1`

**Headers:** 
- `Content-Type: application/json`
- `Authorization: Bearer {token_del_anfitrion}`

**Body:**
```json
{
  "numeroSocio": "SOC-JUAN-2024",
  "puntosAcumulados": 0,
  "referidoPor": "María González",
  "comoConocio": "Redes sociales",
  "estado": "ACTIVO"
}
```

**Respuesta esperada:**
```json
{
  "id": 1,
  "usuarioId": 3,
  "usuarioNombre": "Juan Pérez",
  "clubId": 1,
  "clubNombre": "Club Herbalife Cali Centro",
  "nivelId": 1,
  "nivelNombre": "Bronce",
  "numeroSocio": "SOC-JUAN-2024",
  "puntosAcumulados": 0,
  "referidoPor": "María González",
  "comoConocio": "Redes sociales",
  "fechaRegistro": "2024-01-15T12:00:00",
  "estado": "ACTIVO"
}
```

**Nota:** 
- `nivelId` es opcional (puedes omitirlo en el query param)
- Si no envías `numeroSocio`, se genera automáticamente

---

### 3.4 Consultar Perfil del Socio

**Endpoint:** `GET /api/usuarios/perfil/3`

**Headers:** 
- `Authorization: Bearer {token_del_socio}`

**Respuesta esperada:**
```json
{
  "id": 3,
  "rolId": 2,
  "rolNombre": "SOCIO",
  "nombre": "Juan",
  "apellido": "Pérez",
  "email": "juan.socio@email.com",
  "telefono": "+57 320 555 7890",
  "fechaNacimiento": "1995-11-08",
  "redesSociales": "@juan_perez_fit",
  "estado": "ACTIVO"
}
```

---

## 🏢 4. Club de Prueba Adicional

### 4.1 Segundo Club en el mismo HUB

**Endpoint:** `POST /api/clubes?hubId=1&anfitrionId=2`

**Headers:** 
- `Content-Type: application/json`
- `Authorization: Bearer {token_del_anfitrion}`

**Body:**
```json
{
  "nombreClub": "Club Herbalife Cali Norte",
  "direccion": "Avenida 6N # 28N-45, Cali, Valle del Cauca",
  "horario": "Lunes a Sábado: 7:00 AM - 7:00 PM",
  "lat": 3.4678,
  "lng": -76.5234,
  "estado": "PENDIENTE"
}
```

---

## 🌐 5. HUB de Prueba Adicional

### 5.1 Segundo HUB

**Endpoint:** `POST /api/hubs?adminId=1`

**Headers:** 
- `Content-Type: application/json`
- `Authorization: Bearer {token_del_admin}`

**Body:**
```json
{
  "nombre": "HUB Bogotá",
  "descripcion": "Hub para la ciudad de Bogotá y municipios cercanos de Cundinamarca",
  "estado": "ACTIVO"
}
```

---

## 📋 Resumen de IDs de Ejemplo

Para referencia rápida, estos son los IDs que se usarán en los ejemplos:

| Entidad | ID | Descripción |
|---------|-----|-------------|
| **Usuario Admin** | 1 | admin@herbalife.com |
| **Usuario Anfitrión** | 2 | maria.anfitrion@email.com |
| **Usuario Socio** | 3 | juan.socio@email.com |
| **HUB Valle del Cauca** | 1 | HUB principal |
| **HUB Bogotá** | 2 | HUB secundario |
| **Club Cali Centro** | 1 | Club del anfitrión |
| **Club Cali Norte** | 2 | Segundo club |
| **Membresía Juan** | 1 | Membresía del socio |

**⚠️ IMPORTANTE:** Estos IDs son ejemplos. **Reemplázalos con los IDs reales** que obtengas de tu base de datos al crear las entidades.

---

## 🔑 Credenciales de Prueba Resumidas

| Rol | Email | Password | Rol Nombre |
|-----|-------|----------|------------|
| **Admin** | admin@herbalife.com | Admin123! | ADMIN |
| **Anfitrión** | maria.anfitrion@email.com | Anfitrion123! | SOCIO → ANFITRION |
| **Socio** | juan.socio@email.com | Socio123! | SOCIO |

---

## 🚀 Flujo de Prueba Recomendado

1. **Crear Admin** → Registrar y login
2. **Crear HUB** → Como admin
3. **Crear Anfitrión** → Registrar como usuario normal
4. **Crear Club** → Como anfitrión (estado PENDIENTE)
5. **Aprobar Club** → Como admin (estado APROBADO)
6. **Crear Socio** → Registrar como usuario normal
7. **Crear Membresía** → Como anfitrión, asociar socio al club
8. **Consultar Perfiles** → Verificar datos de cada usuario

---

## 📝 Notas Adicionales

- **Autenticación JWT:** Todos los endpoints protegidos requieren el header `Authorization: Bearer {token}`
- **Roles:** Los IDs de roles deben obtenerse de la base de datos antes de usar
- **Estados:** Los estados típicos son: `ACTIVO`, `PENDIENTE`, `APROBADO`, `RECHAZADO`, `INACTIVO`
- **Fechas:** Usar formato ISO 8601: `YYYY-MM-DD`
- **Coordenadas GPS:** Latitud y longitud como números decimales (BigDecimal)

---

¡Listo para probar! 🎉

