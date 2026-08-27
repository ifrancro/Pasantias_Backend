# 🚀 Guía de Despliegue en Render - Backend Herbalife Clubes

Esta guía te ayudará a desplegar tu backend Spring Boot en Render usando Docker.

## 📋 Prerequisitos

1. ✅ Repositorio Git conectado a Render (ya lo tienes)
2. ✅ Dockerfile en la raíz del proyecto (ya creado)
3. ✅ Cuenta en Render (gratis o de pago)

---

## 🗄️ Paso 1: Crear Base de Datos PostgreSQL en Render

### 1.1 Crear el servicio de base de datos

1. Ve a tu dashboard de Render: https://dashboard.render.com
2. Click en **"New +"** → **"PostgreSQL"**
3. Configura:
   - **Name**: `herbalife-clubes-db` (o el nombre que prefieras)
   - **Database**: `HerbalifeClubesDB` (o déjalo por defecto)
   - **User**: Se genera automáticamente
   - **Region**: Elige la más cercana (ej: `Oregon (US West)`)
   - **PostgreSQL Version**: La más reciente (ej: `16`)
   - **Plan**: Free tier (para empezar)

4. Click en **"Create Database"**

### 1.2 Obtener las credenciales de conexión

Una vez creada la base de datos:

1. Ve a la pestaña **"Info"** de tu base de datos
2. Copia estos valores (los necesitarás después):
   - **Internal Database URL** (para Render) - **USA ESTA**
   - **External Database URL** (si necesitas conectar desde fuera)
   - **Host**
   - **Port** (PostgreSQL usa 5432 por defecto)
   - **Database**
   - **User**
   - **Password**

**Ejemplo de URL interna de PostgreSQL:**
```
postgres://usuario:password@dbserver.xxxxx.render.com:5432/HerbalifeClubesDB
```

**Formato para Spring Boot (jdbc:postgresql://):**
```
jdbc:postgresql://dbserver.xxxxx.render.com:5432/HerbalifeClubesDB
```

---

## 🐳 Paso 2: Crear Web Service en Render

### 2.1 Crear nuevo servicio

1. En el dashboard de Render, click en **"New +"** → **"Web Service"**
2. Conecta tu repositorio:
   - Si es la primera vez: **"Connect account"** (GitHub/GitLab/Bitbucket)
   - Selecciona tu repositorio: `Pasantias_Backend`
   - Click en **"Connect"**

### 2.2 Configurar el servicio

Completa el formulario:

- **Name**: `herbalife-clubes-backend` (o el nombre que prefieras)
- **Region**: Misma región que la base de datos (ej: `Oregon (US West)`)
- **Branch**: `main` (o la rama que uses)
- **Root Directory**: Dejar vacío (raíz del proyecto)
- **Runtime**: **Docker** (importante: selecciona Docker)
- **Dockerfile Path**: `Dockerfile` (dejar por defecto)
- **Docker Context**: Dejar vacío

### 2.3 Configurar variables de entorno

En la sección **"Environment Variables"**, agrega estas variables:

#### Variables requeridas:

1. **JWT_SECRET**
   - **Formato**: Base64 válido.
   - **Longitud**: al decodificar debe contener **al menos 64 bytes (512 bits)** — requerido por HS512.
   - **Cómo generar** (recomendado: 64 bytes aleatorios):
     ```bash
     openssl rand -base64 64
     ```
     PowerShell equivalente:
     ```powershell
     $bytes = New-Object byte[] 64
     [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
     [Convert]::ToBase64String($bytes)
     ```
   - ⚠️ No copies una clave de ejemplo a producción; genera una nueva para cada entorno.

2. **SPRING_DATASOURCE_URL**
   - **Valor**: Convierte la **Internal Database URL** de Render al formato JDBC
   - **Formato**: `jdbc:postgresql://host:puerto/database`
   - **Cómo convertir**: 
     - Render te da: `postgres://usuario:password@host:5432/database`
     - Convierte a: `jdbc:postgresql://host:5432/database`
     - **Ejemplo**: `jdbc:postgresql://dbserver.xxxxx.render.com:5432/HerbalifeClubesDB`
   - ⚠️ **Importante**: 
     - Usa la URL **INTERNA** (no la externa) para mejor rendimiento
     - **NO incluyas** usuario:password en la URL JDBC (se usan las variables separadas)

3. **SPRING_DATASOURCE_USERNAME**
   - **Valor**: El usuario de la base de datos (de Render)

4. **SPRING_DATASOURCE_PASSWORD**
   - **Valor**: La contraseña de la base de datos (de Render)

#### Variables opcionales:

5. **PORT** (opcional)
   - Render lo asigna automáticamente
   - No necesitas configurarlo manualmente

6. **SPRING_PROFILES_ACTIVE** (opcional)
   - Si quieres usar un perfil específico: `production`

### 2.4 Configurar el plan y despliegue

- **Plan**: Free tier (para empezar)
- **Auto-Deploy**: `Yes` (se despliega automáticamente al hacer push)

### 2.5 Crear el servicio

Click en **"Create Web Service"**

---

## ⏳ Paso 3: Esperar el despliegue

1. Render comenzará a construir la imagen Docker
2. Esto puede tomar **5-10 minutos** la primera vez
3. Puedes ver el progreso en la pestaña **"Logs"**
4. Verás mensajes como:
   ```
   Building Docker image...
   Step 1/10 : FROM maven:3.9-eclipse-temurin-17 AS build
   ...
   ```

### ✅ Verificar que el despliegue fue exitoso

Busca en los logs:
- ✅ `Started HerbalifeClubesApplication`
- ✅ `Tomcat started on port(s): XXXX`
- ✅ Sin errores de conexión a la base de datos

---

## 🔍 Paso 4: Verificar que funciona

### 4.1 Obtener la URL de tu servicio

1. En el dashboard de Render, ve a tu Web Service
2. En la parte superior verás la URL: `https://herbalife-clubes-backend.onrender.com`
3. Copia esta URL

### 4.2 Probar endpoints

#### Swagger UI:
```
https://tu-servicio.onrender.com/swagger-ui.html
```

#### Endpoint de prueba (requiere autenticación):
```bash
GET https://tu-servicio.onrender.com/api/auth/me
```

#### Registrar un usuario:
```bash
POST https://tu-servicio.onrender.com/api/auth/register
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123",
  "nombre": "Test User"
}
```

#### Login:
```bash
POST https://tu-servicio.onrender.com/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "password123"
}
```

---

## 🛠️ Solución de Problemas Comunes

### ❌ Error: "JWT_SECRET no encontrada" / clave demasiado corta

**Solución**: Configura `JWT_SECRET` en Render como Base64 válido. Al decodificar debe tener **≥ 64 bytes** (usa `openssl rand -base64 64`).

### ❌ Error: "Cannot connect to database"

**Solución**: 
1. Verifica que `SPRING_DATASOURCE_URL` use la URL **INTERNA** de Render
2. Verifica que `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` sean correctos
3. Asegúrate de que la base de datos esté en la misma región que el Web Service

### ❌ Error: "Port already in use"

**Solución**: No debería pasar, pero si ocurre, Render asigna el puerto automáticamente. Verifica que no hayas configurado `PORT` manualmente.

### ❌ El servicio se detiene después de unos minutos (Free tier)

**Solución**: En el plan gratuito, Render "duerme" el servicio después de 15 minutos de inactividad. La primera petición puede tardar ~30 segundos en despertar. Para evitar esto, considera:
- Usar un servicio de "ping" cada 10 minutos
- Actualizar a un plan de pago

### ❌ Build falla con error de Maven

**Solución**: 
1. Verifica que el `Dockerfile` esté en la raíz del proyecto
2. Verifica que `pom.xml` esté presente
3. Revisa los logs completos en Render para ver el error específico

---

## 📝 Notas Importantes

1. **Variables de entorno**: Nunca subas `.env` a Git. Render usa variables de entorno del dashboard.

2. **Base de datos**: 
   - En producción, considera cambiar `ddl-auto=update` a `validate` o `none` para mayor seguridad
   - Haz backups regulares de tu base de datos

3. **Logs**: 
   - Los logs están disponibles en tiempo real en el dashboard de Render
   - Útiles para debugging

4. **Actualizaciones**: 
   - Con `Auto-Deploy: Yes`, cada push a la rama principal despliega automáticamente
   - Puedes desactivarlo y hacer deploys manuales si prefieres

5. **Dominio personalizado**: 
   - Puedes configurar un dominio personalizado en la pestaña "Settings" → "Custom Domain"

---

## 🎉 ¡Listo!

Tu backend debería estar funcionando en Render. La URL será algo como:
```
https://herbalife-clubes-backend.onrender.com
```

**Swagger UI**: `https://herbalife-clubes-backend.onrender.com/swagger-ui.html`

---

## 📞 Soporte

Si tienes problemas:
1. Revisa los logs en Render
2. Verifica todas las variables de entorno
3. Asegúrate de que la base de datos esté corriendo
4. Consulta la documentación de Render: https://render.com/docs

