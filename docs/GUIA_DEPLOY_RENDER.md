# 🚀 Guía: Deploy en Render y Verificación de Roles

## 📋 Pasos para Deploy en Render

### 1️⃣ Hacer Commit y Push de los Cambios

```bash
# Verificar los archivos modificados
git status

# Agregar los nuevos archivos
git add src/main/java/com/example/herbalife_clubes/config/DataInitializer.java
git add src/main/java/com/example/herbalife_clubes/controllers/RolController.java

# Hacer commit
git commit -m "feat: Agregar inicializador de roles y controlador de roles"

# Push a tu repositorio (ajusta la rama según corresponda)
git push origin main
# o
git push origin master
```

### 2️⃣ Render Detectará los Cambios Automáticamente

Render está conectado a tu repositorio y detectará automáticamente el push. Comenzará a:
- Compilar el código
- Ejecutar el build
- Desplegar la nueva versión

### 3️⃣ El DataInitializer se Ejecutará Automáticamente

Cuando Render despliegue la aplicación, el `DataInitializer` se ejecutará al arrancar y creará los roles si no existen.

---

## ✅ Cómo Verificar que los Roles se Crearon

### Método 1: Usando el Endpoint de Roles (Recomendado)

He creado un endpoint simple para consultar los roles:

**GET** `https://tu-api.onrender.com/api/roles`

Puedes probarlo en:
- **Swagger/OpenAPI:** `https://tu-api.onrender.com/swagger-ui.html`
- **Postman/Thunder Client**
- **Navegador** (si tienes CORS configurado)

**Respuesta esperada:**
```json
[
  {
    "id": 1,
    "nombre": "ADMIN"
  },
  {
    "id": 2,
    "nombre": "SOCIO"
  },
  {
    "id": 3,
    "nombre": "ANFITRION"
  }
]
```

### Método 2: Verificar los Logs de Render

1. Ve a tu dashboard de Render
2. Selecciona tu servicio (API)
3. Ve a la pestaña **"Logs"**
4. Busca mensajes como:
   ```
   🔍 Verificando e inicializando roles básicos...
   ✅ Rol ADMIN creado
   ✅ Rol SOCIO creado
   ✅ Rol ANFITRION creado
   ✨ Inicialización de roles completada
   ```

Si los roles ya existían, verás:
   ```
   ℹ️  Rol ADMIN ya existe
   ℹ️  Rol SOCIO ya existe
   ℹ️  Rol ANFITRION ya existe
   ```

### Método 3: Consultar la Base de Datos Directamente

Si tienes acceso a tu base de datos PostgreSQL en Render:

1. **En Render:**
   - Ve a tu servicio de base de datos PostgreSQL
   - Selecciona "Connect" o "Connection Info"
   - Usa las credenciales para conectarte

2. **Ejecutar SQL:**
```sql
SELECT * FROM roles ORDER BY id;
```

**Resultado esperado:**
```
 id |   nombre   
----+------------
  1 | ADMIN
  2 | SOCIO
  3 | ANFITRION
```

---

## 🔧 Si los Roles NO se Crearon

### Posibles Causas:

1. **El DataInitializer no se ejecutó:**
   - Revisa los logs de Render
   - Verifica que el componente esté siendo escaneado por Spring

2. **Error de conexión a la base de datos:**
   - Verifica las variables de entorno en Render
   - Revisa que `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` estén configuradas

3. **Error en el código:**
   - Revisa los logs completos en Render
   - Busca errores de compilación o runtime

### Solución Manual (Temporal):

Si necesitas crear los roles manualmente mientras solucionas el problema, puedes ejecutar este SQL en tu base de datos:

```sql
-- Crear roles manualmente
INSERT INTO roles (nombre) VALUES ('ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO roles (nombre) VALUES ('SOCIO') ON CONFLICT DO NOTHING;
INSERT INTO roles (nombre) VALUES ('ANFITRION') ON CONFLICT DO NOTHING;

-- Verificar
SELECT * FROM roles ORDER BY id;
```

**Nota:** Este SQL usa `ON CONFLICT DO NOTHING` para evitar errores si los roles ya existen.

---

## 📝 Resumen

1. ✅ **Hacer git push** → Render detectará los cambios
2. ✅ **Esperar a que Render despliegue** → El DataInitializer se ejecutará
3. ✅ **Verificar los roles:**
   - Usar el endpoint: `GET /api/roles`
   - Ver logs en Render
   - Consultar la base de datos directamente

---

## 🎯 Próximos Pasos

Una vez verificados los roles, puedes:

1. **Usar los IDs de roles** en tus requests de registro
2. **Probar crear usuarios** con diferentes roles usando Swagger
3. **Actualizar tu documento de datos de prueba** con los IDs reales de los roles

¡Listo para probar! 🚀

