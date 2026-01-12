# 📚 Documentación de Endpoints API - Herbalife Clubes

**URL Base:** `https://clubs-api.onrender.com`

**Swagger UI:** `https://clubs-api.onrender.com/swagger-ui.html`

---

## 🔐 Autenticación

Todos los endpoints (excepto `/api/auth/register` y `/api/auth/login`) requieren autenticación JWT.

**Header requerido:**
```
Authorization: Bearer <token>
```

---

## 1. 🔑 Autenticación (`/api/auth`)

### POST `/api/auth/register`
Registrar nuevo usuario

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "password123",
  "nombre": "Juan Pérez",
  "apellido": "García",
  "telefono": "3001234567"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": { ... }
}
```

---

### POST `/api/auth/login`
Iniciar sesión

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "usuario": { ... }
}
```

---

### GET `/api/auth/me`
Obtener usuario autenticado

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "usuario@example.com",
  "nombre": "Juan",
  "apellido": "Pérez",
  ...
}
```

---

## 2. 👥 Usuarios (`/api/usuarios`)

### GET `/api/usuarios`
Listar todos los usuarios

**Response:** `200 OK` - Lista de usuarios

---

### GET `/api/usuarios/{id}`
Obtener usuario por ID

**Response:** `200 OK` - UsuarioDTO

---

### GET `/api/usuarios/perfil/{usuarioId}`
Obtener perfil de usuario

**Response:** `200 OK` - UsuarioDTO

---

### PUT `/api/usuarios/perfil/{usuarioId}`
Actualizar perfil de usuario

**Request Body:** UsuarioDTO

**Response:** `200 OK` - UsuarioDTO actualizado

---

### PATCH `/api/usuarios/{id}/desactivar`
Desactivar usuario

**Response:** `200 OK` - UsuarioDTO desactivado

---

## 3. 🏢 Hubs (`/api/hubs`)

### POST `/api/hubs?adminId={adminId}`
Crear nuevo hub

**Query Params:**
- `adminId` (required): ID del administrador

**Request Body:** HubDTO

**Response:** `201 CREATED` - HubDTO

---

### GET `/api/hubs`
Listar todos los hubs

**Response:** `200 OK` - Lista de HubDTO

---

### GET `/api/hubs/{id}`
Obtener hub por ID

**Response:** `200 OK` - HubDTO

---

### PUT `/api/hubs/{id}`
Actualizar hub

**Request Body:** HubDTO

**Response:** `200 OK` - HubDTO actualizado

---

### DELETE `/api/hubs/{id}`
Eliminar hub

**Response:** `200 OK` - "Hub eliminado exitosamente"

---

### PATCH `/api/hubs/{id}/activar`
Activar hub

**Response:** `200 OK` - HubDTO activado

---

### PATCH `/api/hubs/{id}/inactivar`
Inactivar hub

**Response:** `200 OK` - HubDTO inactivado

---

## 4. 🏠 Clubes (`/api/clubes`)

### POST `/api/clubes?hubId={hubId}&anfitrionId={anfitrionId}`
Crear nuevo club

**Query Params:**
- `hubId` (required): ID del hub
- `anfitrionId` (required): ID del anfitrión

**Request Body:** ClubDTO

**Response:** `201 CREATED` - ClubDTO

---

### GET `/api/clubes`
Listar todos los clubes

**Query Params (opcional):**
- `hubId`: Filtrar por hub

**Response:** `200 OK` - Lista de ClubDTO

---

### GET `/api/clubes/{id}`
Obtener club por ID

**Response:** `200 OK` - ClubDTO

---

### PUT `/api/clubes/{id}`
Actualizar club

**Request Body:** ClubDTO

**Response:** `200 OK` - ClubDTO actualizado

---

### PATCH `/api/clubes/{id}/aprobar`
Aprobar club

**Response:** `200 OK` - ClubDTO aprobado

---

### PATCH `/api/clubes/{id}/rechazar`
Rechazar club

**Response:** `200 OK` - ClubDTO rechazado

---

### PATCH `/api/clubes/{id}/activar`
Activar club

**Response:** `200 OK` - ClubDTO activado

---

### PATCH `/api/clubes/{id}/desactivar`
Desactivar club

**Response:** `200 OK` - ClubDTO desactivado

---

## 5. 📸 Fotos de Club (`/api/fotos-club`)

### POST `/api/fotos-club/subir?clubId={clubId}&urlFoto={urlFoto}&tipo={tipo}`
Subir foto de club

**Query Params:**
- `clubId` (required): ID del club
- `urlFoto` (required): URL de la foto
- `tipo` (optional): Tipo de foto

**Response:** `201 CREATED` - FotoClubDTO

---

### GET `/api/fotos-club/club/{clubId}`
Listar fotos de un club

**Response:** `200 OK` - Lista de FotoClubDTO

---

### DELETE `/api/fotos-club/{id}`
Eliminar foto

**Response:** `200 OK` - "Foto eliminada exitosamente"

---

## 6. 🛍️ Productos (`/api/productos`)

### POST `/api/productos?clubId={clubId}`
Crear nuevo producto

**Query Params:**
- `clubId` (required): ID del club

**Request Body:** ProductoDTO

**Response:** `201 CREATED` - ProductoDTO

---

### GET `/api/productos`
Listar todos los productos

**Query Params (opcional):**
- `clubId`: Filtrar por club

**Response:** `200 OK` - Lista de ProductoDTO

---

### GET `/api/productos/{id}`
Obtener producto por ID

**Response:** `200 OK` - ProductoDTO

---

### PUT `/api/productos/{id}`
Actualizar producto

**Request Body:** ProductoDTO

**Response:** `200 OK` - ProductoDTO actualizado

---

### PATCH `/api/productos/{id}/activar`
Activar producto

**Response:** `200 OK` - ProductoDTO activado

---

### PATCH `/api/productos/{id}/desactivar`
Desactivar producto

**Response:** `200 OK` - ProductoDTO desactivado

---

## 7. 👤 Membresías (`/api/membresias`)

### POST `/api/membresias?usuarioId={usuarioId}&clubId={clubId}&nivelId={nivelId}`
Crear nueva membresía

**Query Params:**
- `usuarioId` (required): ID del usuario
- `clubId` (required): ID del club
- `nivelId` (optional): ID del nivel socio

**Request Body:** MembresiaDTO

**Response:** `201 CREATED` - MembresiaDTO

---

### GET `/api/membresias/{id}`
Obtener membresía por ID

**Response:** `200 OK` - MembresiaDTO

---

### GET `/api/membresias/usuario/{usuarioId}`
Obtener membresía de un usuario

**Response:** `200 OK` - MembresiaDTO

---

### GET `/api/membresias/club/{clubId}`
Listar membresías de un club

**Response:** `200 OK` - Lista de MembresiaDTO

---

### PATCH `/api/membresias/{id}/estado?estado={estado}`
Cambiar estado de membresía

**Query Params:**
- `estado` (required): Nuevo estado

**Response:** `200 OK` - MembresiaDTO actualizado

---

### PATCH `/api/membresias/{id}/nivel?nivelId={nivelId}`
Cambiar nivel de membresía

**Query Params:**
- `nivelId` (required): ID del nuevo nivel

**Response:** `200 OK` - MembresiaDTO actualizado

---

### PATCH `/api/membresias/{id}/puntos?puntos={puntos}`
Actualizar puntos de membresía

**Query Params:**
- `puntos` (required): Cantidad de puntos

**Response:** `200 OK` - MembresiaDTO actualizado

---

## 8. 📦 Pedidos (`/api/pedidos`)

### POST `/api/pedidos?membresiaId={membresiaId}&clubId={clubId}&productoId={productoId}`
Crear nuevo pedido

**Query Params:**
- `membresiaId` (required): ID de la membresía
- `clubId` (required): ID del club
- `productoId` (required): ID del producto

**Request Body:** PedidoDTO

**Response:** `201 CREATED` - PedidoDTO

---

### GET `/api/pedidos/{id}`
Obtener pedido por ID

**Response:** `200 OK` - PedidoDTO

---

### GET `/api/pedidos/socio/{membresiaId}`
Listar pedidos de un socio

**Response:** `200 OK` - Lista de PedidoDTO

---

### GET `/api/pedidos/club/{clubId}`
Listar pedidos de un club

**Response:** `200 OK` - Lista de PedidoDTO

---

### PATCH `/api/pedidos/{id}/estado?estado={estado}`
Actualizar estado de pedido

**Query Params:**
- `estado` (required): Nuevo estado

**Response:** `200 OK` - PedidoDTO actualizado

---

### PATCH `/api/pedidos/{id}/cancelar`
Cancelar pedido

**Response:** `200 OK` - PedidoDTO cancelado

---

## 9. 📅 Eventos (`/api/eventos`)

### POST `/api/eventos?hubId={hubId}&clubId={clubId}`
Crear nuevo evento

**Query Params:**
- `hubId` (optional): ID del hub
- `clubId` (optional): ID del club

**Request Body:** EventoDTO

**Response:** `201 CREATED` - EventoDTO

---

### GET `/api/eventos`
Listar eventos

**Query Params (opcional):**
- `hubId`: Filtrar por hub
- `clubId`: Filtrar por club

**Response:** `200 OK` - Lista de EventoDTO

---

### GET `/api/eventos/{id}`
Obtener evento por ID

**Response:** `200 OK` - EventoDTO

---

### PUT `/api/eventos/{id}`
Actualizar evento

**Request Body:** EventoDTO

**Response:** `200 OK` - EventoDTO actualizado

---

### DELETE `/api/eventos/{id}`
Eliminar evento

**Response:** `200 OK` - "Evento eliminado exitosamente"

---

## 10. ✅ Asistencias (`/api/asistencias`)

### POST `/api/asistencias/registrar?membresiaId={membresiaId}&clubId={clubId}&qrClub={qrClub}`
Registrar asistencia

**Query Params:**
- `membresiaId` (required): ID de la membresía
- `clubId` (required): ID del club
- `qrClub` (optional): Código QR del club

**Response:** `201 CREATED` - AsistenciaDTO

---

### GET `/api/asistencias/socio/{membresiaId}`
Listar asistencias de un socio

**Response:** `200 OK` - Lista de AsistenciaDTO

---

### GET `/api/asistencias/club/{clubId}`
Listar asistencias de un club

**Response:** `200 OK` - Lista de AsistenciaDTO

---

### GET `/api/asistencias`
Listar todas las asistencias

**Response:** `200 OK` - Lista de AsistenciaDTO

---

## 11. 🍽️ Consumos (`/api/consumos`)

### POST `/api/consumos/registrar?membresiaId={membresiaId}&clubId={clubId}&productoId={productoId}&descripcion={descripcion}&cantidad={cantidad}&precioReferencial={precioReferencial}&asistenciaId={asistenciaId}`
Registrar consumo

**Query Params:**
- `membresiaId` (required): ID de la membresía
- `clubId` (required): ID del club
- `productoId` (optional): ID del producto
- `descripcion` (optional): Descripción del consumo
- `cantidad` (optional): Cantidad
- `precioReferencial` (optional): Precio referencial
- `asistenciaId` (optional): ID de la asistencia

**Response:** `201 CREATED` - ConsumoDTO

---

### GET `/api/consumos/socio/{membresiaId}`
Obtener historial de consumos de un socio

**Response:** `200 OK` - Lista de ConsumoDTO

---

### GET `/api/consumos/club/{clubId}`
Obtener historial de consumos de un club

**Response:** `200 OK` - Lista de ConsumoDTO

---

### GET `/api/consumos/asistencia/{asistenciaId}`
Obtener consumos de una asistencia

**Response:** `200 OK` - Lista de ConsumoDTO

---

## 12. 🏆 Logros (`/api/logros`)

### POST `/api/logros`
Crear nuevo logro

**Request Body:** LogroDTO

**Response:** `201 CREATED` - LogroDTO

---

### GET `/api/logros`
Listar todos los logros

**Response:** `200 OK` - Lista de LogroDTO

---

### GET `/api/logros/{id}`
Obtener logro por ID

**Response:** `200 OK` - LogroDTO

---

### PUT `/api/logros/{id}`
Actualizar logro

**Request Body:** LogroDTO

**Response:** `200 OK` - LogroDTO actualizado

---

### DELETE `/api/logros/{id}`
Eliminar logro

**Response:** `200 OK` - "Logro eliminado exitosamente"

---

### PATCH `/api/logros/{id}/activar`
Activar logro

**Response:** `200 OK` - LogroDTO activado

---

### PATCH `/api/logros/{id}/inactivar`
Inactivar logro

**Response:** `200 OK` - LogroDTO inactivado

---

## 13. 🎖️ Membresía-Logros (`/api/membresia-logros`)

### POST `/api/membresia-logros/evaluar/{membresiaId}`
Evaluar logros automáticamente para una membresía

**Response:** `200 OK` - "Logros evaluados automáticamente"

---

### GET `/api/membresia-logros/membresia/{membresiaId}`
Listar logros de una membresía

**Response:** `200 OK` - Lista de MembresiaLogro

---

## 14. 📊 Niveles Socio (`/api/niveles-socio`)

### POST `/api/niveles-socio`
Crear nuevo nivel socio

**Request Body:** NivelSocioDTO

**Response:** `201 CREATED` - NivelSocioDTO

---

### GET `/api/niveles-socio`
Listar todos los niveles socio

**Response:** `200 OK` - Lista de NivelSocioDTO

---

### GET `/api/niveles-socio/{id}`
Obtener nivel socio por ID

**Response:** `200 OK` - NivelSocioDTO

---

### PUT `/api/niveles-socio/{id}`
Actualizar nivel socio

**Request Body:** NivelSocioDTO

**Response:** `200 OK` - NivelSocioDTO actualizado

---

### DELETE `/api/niveles-socio/{id}`
Eliminar nivel socio

**Response:** `200 OK` - "Nivel socio eliminado exitosamente"

---

## 15. 🔔 Notificaciones (`/api/notificaciones`)

### POST `/api/notificaciones/enviar?hubId={hubId}&clubId={clubId}&usuarioId={usuarioId}&pedidoId={pedidoId}`
Enviar notificación

**Query Params (opcional):**
- `hubId`: ID del hub
- `clubId`: ID del club
- `usuarioId`: ID del usuario
- `pedidoId`: ID del pedido

**Request Body:** NotificacionDTO

**Response:** `201 CREATED` - NotificacionDTO

---

### GET `/api/notificaciones/usuario/{usuarioId}`
Obtener historial de notificaciones de un usuario

**Response:** `200 OK` - Lista de NotificacionDTO

---

### GET `/api/notificaciones/hub/{hubId}`
Obtener historial de notificaciones de un hub

**Response:** `200 OK` - Lista de NotificacionDTO

---

### GET `/api/notificaciones/club/{clubId}`
Obtener historial de notificaciones de un club

**Response:** `200 OK` - Lista de NotificacionDTO

---

### GET `/api/notificaciones`
Obtener historial completo de notificaciones

**Response:** `200 OK` - Lista de NotificacionDTO

---

## 16. 🎫 Soporte Tickets (`/api/soporte-tickets`)

### POST `/api/soporte-tickets?usuarioId={usuarioId}`
Crear nuevo ticket de soporte

**Query Params:**
- `usuarioId` (required): ID del usuario

**Request Body:** SoporteTicketDTO

**Response:** `201 CREATED` - SoporteTicketDTO

---

### GET `/api/soporte-tickets/{id}`
Obtener ticket por ID

**Response:** `200 OK` - SoporteTicketDTO

---

### GET `/api/soporte-tickets/usuario/{usuarioId}`
Listar tickets de un usuario

**Response:** `200 OK` - Lista de SoporteTicketDTO

---

### GET `/api/soporte-tickets`
Listar todos los tickets

**Response:** `200 OK` - Lista de SoporteTicketDTO

---

### PATCH `/api/soporte-tickets/{id}/responder?respuestaAdmin={respuestaAdmin}`
Responder ticket

**Query Params:**
- `respuestaAdmin` (required): Respuesta del administrador

**Response:** `200 OK` - SoporteTicketDTO actualizado

---

### PATCH `/api/soporte-tickets/{id}/estado?estado={estado}`
Cambiar estado de ticket

**Query Params:**
- `estado` (required): Nuevo estado

**Response:** `200 OK` - SoporteTicketDTO actualizado

---

## 📝 Notas Importantes

1. **Autenticación JWT**: Todos los endpoints (excepto register y login) requieren el header:
   ```
   Authorization: Bearer <token>
   ```

2. **CORS**: Todos los endpoints tienen CORS habilitado para `*` (cualquier origen)

3. **Swagger UI**: Puedes ver y probar todos los endpoints en:
   ```
   https://clubs-api.onrender.com/swagger-ui.html
   ```

4. **Códigos de Estado HTTP**:
   - `200 OK`: Operación exitosa
   - `201 CREATED`: Recurso creado exitosamente
   - `400 BAD REQUEST`: Error en la solicitud
   - `401 UNAUTHORIZED`: No autenticado
   - `404 NOT FOUND`: Recurso no encontrado
   - `500 INTERNAL SERVER ERROR`: Error del servidor

5. **Formato de Fechas**: Las fechas se manejan en formato ISO 8601 (ej: `2024-01-15T10:30:00`)

---

## 🚀 Ejemplo de Uso Completo

```javascript
// 1. Registrar usuario
const registerResponse = await fetch('https://clubs-api.onrender.com/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'usuario@example.com',
    password: 'password123',
    nombre: 'Juan',
    apellido: 'Pérez'
  })
});
const { token } = await registerResponse.json();

// 2. Obtener clubes (con autenticación)
const clubesResponse = await fetch('https://clubs-api.onrender.com/api/clubes', {
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
});
const clubes = await clubesResponse.json();
```

---

**¡Listo para desarrollar el frontend! 🎉**

