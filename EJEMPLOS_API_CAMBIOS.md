# Ejemplos de Uso - Cambios QR, Beneficios y Asistencias

## 1. Validación QR de Socio

### Endpoint: `POST /api/qr/validar-socio`

**Request:**
```json
{
  "qr": "SOC-12345678",
  "clubId": 1
}
```

**Response (Socio válido):**
```json
{
  "membresiaId": 1,
  "numeroSocio": "SOC-12345678",
  "nombreCompleto": "Juan Pérez",
  "estado": "ACTIVA",
  "nivelNombre": "Bronce",
  "rachaActual": 5,
  "rachaMaxima": 10,
  "mensaje": "Socio válido y activo. Puede gozar de beneficios en todos los clubes.",
  "valido": true
}
```

**Response (Socio no encontrado):**
```json
{
  "valido": false,
  "mensaje": "Socio no encontrado con el QR proporcionado"
}
```

**Response (Socio inactivo):**
```json
{
  "membresiaId": 1,
  "numeroSocio": "SOC-12345678",
  "valido": false,
  "mensaje": "El socio no está activo. Estado actual: INACTIVA"
}
```

---

## 2. Registrar Asistencia (Global)

### Endpoint: `POST /api/asistencias/registrar?membresiaId=1&clubId=2`

**Request:**
```
POST /api/asistencias/registrar?membresiaId=1&clubId=2
```

**Response (Éxito):**
```json
{
  "id": 15,
  "membresiaId": 1,
  "membresiaNumeroSocio": "SOC-12345678",
  "clubId": 2,
  "clubNombre": "Club Centro",
  "fechaHora": "2024-01-15T10:30:00",
  "fechaDia": "2024-01-15",
  "estado": "CONFIRMADA",
  "rachaActual": 6,
  "rachaMaxima": 10
}
```

**Response (Error - Ya registrado hoy):**
```json
{
  "error": "Ya existe una asistencia registrada para este socio hoy. No se puede registrar asistencia duplicada en el mismo día."
}
```

**Response (Error - Club inactivo):**
```json
{
  "error": "El club no está activo. Estado actual: PENDIENTE"
}
```

**Notas:**
- El socio puede registrar asistencia en CUALQUIER club activo (sin restricción de HUB)
- Solo se permite 1 asistencia por día por socio (global)
- La racha se actualiza automáticamente:
  - Si la última asistencia fue ayer → racha_actual++
  - Si la última asistencia fue antes de ayer → racha_actual = 1
  - racha_maxima se actualiza si racha_actual > racha_maxima

---

## 3. Crear Pedido (Multi-item)

### Endpoint: `POST /api/pedidos?membresiaId=1&clubId=2&productoId=3`

**Request:**
```json
{
  "horarioDeseado": "14:00",
  "tipoConsumo": "PARA_LLEVAR",
  "cantidad": 2,
  "observaciones": "Sin azúcar"
}
```

**Response (Éxito):**
```json
{
  "id": 25,
  "membresiaId": 1,
  "clubId": 2,
  "horarioDeseado": "14:00",
  "tipoConsumo": "PARA_LLEVAR",
  "estado": "RECIBIDO",
  "fechaPedido": "2024-01-15T10:35:00",
  "items": [
    {
      "id": 1,
      "productoId": 3,
      "productoNombre": "Batido Proteico",
      "cantidad": 2,
      "nota": "Sin azúcar"
    }
  ]
}
```

**Response (Error - Producto no disponible):**
```json
{
  "error": "El producto no está disponible en este club"
}
```

**Response (Error - Club inactivo):**
```json
{
  "error": "El club destino no está activo. Estado actual: PENDIENTE"
}
```

**Notas:**
- Se eliminó la validación de HUB (ya no se requiere que el club destino sea del mismo HUB)
- Se mantiene la validación de disponibilidad del producto en el club (club_productos.disponible=true)
- Se valida que el club destino esté activo
- Se valida que el socio esté activo

---

## 4. Listar Asistencias por Socio

### Endpoint: `GET /api/asistencias/socio/1`

**Response:**
```json
[
  {
    "id": 15,
    "membresiaId": 1,
    "membresiaNumeroSocio": "SOC-12345678",
    "clubId": 2,
    "clubNombre": "Club Centro",
    "fechaHora": "2024-01-15T10:30:00",
    "fechaDia": "2024-01-15",
    "estado": "CONFIRMADA",
    "rachaActual": 6,
    "rachaMaxima": 10
  },
  {
    "id": 14,
    "membresiaId": 1,
    "membresiaNumeroSocio": "SOC-12345678",
    "clubId": 1,
    "clubNombre": "Club Norte",
    "fechaHora": "2024-01-14T09:00:00",
    "fechaDia": "2024-01-14",
    "estado": "CONFIRMADA",
    "rachaActual": 6,
    "rachaMaxima": 10
  }
]
```

---

## 5. Gamificación - Logros por Racha

Los logros se otorgan automáticamente cuando la racha alcanza umbrales específicos:
- **Racha 3 días**: Logro con `tipoRequisito = "RACHA_3"`
- **Racha 7 días**: Logro con `tipoRequisito = "RACHA_7"`
- **Racha 14 días**: Logro con `tipoRequisito = "RACHA_14"`

**Nota:** Los logros deben existir previamente en la tabla `logros` con el `tipoRequisito` correspondiente.

---

## Resumen de Cambios

### ✅ Cambios Implementados:

1. **Migración SQL**: Agregados campos `racha_actual`, `racha_maxima`, `ultima_asistencia_dia` a `membresias`
2. **Asistencias Globales**: 
   - Eliminada restricción de HUB/club
   - 1 asistencia por día por socio (global)
   - Racha global por socio
3. **Validación QR**: 
   - Endpoint `/api/qr/validar-socio`
   - Sin restricciones de club/HUB
   - Valida socio activo
4. **Pedidos**: 
   - Eliminada validación HUB
   - Mantiene validación de disponibilidad por club
5. **Gamificación**: Otorga logros automáticamente en umbrales de racha (3, 7, 14 días)

### ⚠️ Notas Importantes:

- El constraint único de `asistencias` cambió de `(membresia_id, club_id, fecha_dia)` a `(membresia_id, fecha_dia)`
- La racha es global: un socio puede asistir a diferentes clubes y mantener su racha
- Los beneficios son globales: un socio activo puede usar servicios en cualquier club activo

