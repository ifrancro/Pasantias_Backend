# 🚀 Guía Rápida - Setup Backend Herbalife Clubes

## 📋 Prerequisitos
- Java 17+
- Maven
- PostgreSQL 14+ (puerto 5432) o MySQL 8.0+ (puerto 3306)

## ⚙️ Configuración (5 minutos)

### 1️⃣ Clonar y abrir proyecto
```bash
git clone <tu-repo-url>
cd Pasantias_Backend
```

### 2️⃣ Crear base de datos PostgreSQL (recomendado) o MySQL

**PostgreSQL:**
```sql
CREATE DATABASE HerbalifeClubesDB;
```

**MySQL:**
```sql
CREATE DATABASE HerbalifeClubesDB;
```

### 3️⃣ Crear archivo `.env` en la raíz del proyecto
```bash
# Windows PowerShell:
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
"JWT_SECRET=$secret" | Out-File -Encoding ascii .env
```

**O manualmente:** Crea `.env` con:
```
JWT_SECRET=tu_clave_base64_aqui
```

Para generar clave: usa un generador online de base64 (64 caracteres) o ejecuta el comando de PowerShell arriba.

### 4️⃣ Verificar `application.properties`
Asegúrate que tenga (PostgreSQL por defecto):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/HerbalifeClubesDB
spring.datasource.username=postgres
spring.datasource.password=postgres
```

**Si usas MySQL localmente**, cambia a:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/HerbalifeClubesDB
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### 5️⃣ Ejecutar proyecto
```bash
# Windows:
.\mvnw.cmd spring-boot:run

# Linux/Mac:
./mvnw spring-boot:run
```

## ✅ Verificar que funciona
- Backend corriendo: `http://localhost:9090`
- Swagger UI: `http://localhost:9090/swagger-ui.html`
- Endpoint de prueba: `GET http://localhost:9090/api/auth/me` (requiere JWT)

## 🔑 Endpoints principales
- `POST /api/auth/register` - Registrar usuario
- `POST /api/auth/login` - Login (devuelve JWT)
- `GET /api/auth/me` - Perfil del usuario autenticado

## ⚠️ Notas
- El `.env` NO se sube a git (está en .gitignore)
- Cada desarrollador debe crear su propio `.env`
- La base de datos se crea automáticamente con `ddl-auto=update`

