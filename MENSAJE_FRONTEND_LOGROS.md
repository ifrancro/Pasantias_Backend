# 🏆 Cambio: Sistema de Logros por Asistencias
## ⚠️ Para el Equipo Frontend
### Cambio Implementado
**Los logros ahora se otorgan AUTOMÁTICAMENTE** cuando un socio registra una asistencia y cumple con el requisito de asistencias.
### ¿Qué cambió?

1. **Campo `tipoRequisito` ahora es INT** (antes era String)
   - Representa la **cantidad de asistencias requeridas** para obtener el logro
   - Ejemplo: `tipoRequisito: 5` = se necesita 5 asistencias

2. **Evaluación automática de logros**
   - Al registrar una asistencia, el backend evalúa automáticamente si el socio cumple requisitos para nuevos logros
   - Ya no necesitas llamar manualmente al endpoint de evaluación

3. **Los logros se basan en `puntos_acumulados`**
   - `puntos_acumulados` = cantidad total de asistencias
   - Si `puntos_acumulados >= tipoRequisito` → se otorga el logro

### Endpoints

#### Obtener logros de un socio:
```http
GET /api/membresia-logros/membresia/{membresiaId}
```

**Response:**
```json
[
  {
    "id": 1,
    "membresiaId": 1,
    "logro": {
      "id": 1,
      "nombre": "Primera Visita",
      "descripcion": "Completa 5 asistencias",
      "iconoUrl": "...",
      "tipoRequisito": 5  // ← Ahora es INT (cantidad de asistencias)
    },
    "fechaObtencion": "2024-01-15T10:30:00"
  }
]
```

#### Evaluar logros manualmente (opcional):
```http
POST /api/membresia-logros/evaluar/{membresiaId}
```
**Nota:** Ya no es necesario llamar esto manualmente, se hace automáticamente al registrar asistencia.

### Flujo Recomendado

```dart
// 1. Registrar asistencia
await registrarAsistencia(membresiaId: id, clubId: clubId);

// 2. Recargar membresía (para puntos actualizados)
final membresia = await obtenerMembresia(id);

// 3. Obtener logros del socio (puede haber nuevos logros)
final logros = await obtenerLogros(id);

// 4. Mostrar notificación si hay logros nuevos
if (logros.length > logrosAnteriores.length) {
  mostrarNotificacionLogro(logros.last);
}
```

### Cambios en el Modelo

**LogroDTO:**
```dart
class LogroDTO {
  int id;
  String nombre;
  String descripcion;
  String iconoUrl;
  int tipoRequisito;  // ← Cambió de String a int
}
```

### Ejemplo de Uso

**Admin crea logro:**
- nombre: "Primera Visita"
- descripcion: "Completa 5 asistencias"
- tipoRequisito: **5** (INT - cantidad de asistencias)

**Sistema:**
- Cuando socio tiene 5 asistencias (`puntos_acumulados >= 5`)
- Se otorga automáticamente el logro
- Se guarda en `membresia_logros`

### ⚠️ Acciones Requeridas

- [ ] Actualizar modelo `LogroDTO`: `tipoRequisito` de `String` a `int`
- [ ] Actualizar parseo JSON para manejar `tipoRequisito` como número
- [ ] Después de registrar asistencia, obtener logros para mostrar nuevos logros
- [ ] Mostrar notificación cuando se obtiene un nuevo logro

### Notas

- Los logros se otorgan automáticamente, no necesitas llamar al endpoint de evaluación
- `tipoRequisito` ahora es un número (cantidad de asistencias requeridas)
- Los logros se basan en `puntos_acumulados` que equivale al conteo total de asistencias

---

**Fecha:** [Fecha actual]  
**Versión Backend:** [Versión actual]

