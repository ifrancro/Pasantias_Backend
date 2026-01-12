# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY . .
RUN mvn -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto (Render usa PORT automáticamente, pero Spring Boot usa 9090 por defecto)
# Si Render asigna otro puerto, se puede sobrescribir con SERVER_PORT
EXPOSE 9090

# Variables de entorno que Render debe configurar:
# - JWT_SECRET (requerida)
# - SERVER_PORT (opcional, Render puede asignar automáticamente)
# - SPRING_DATASOURCE_URL (para la base de datos PostgreSQL en Render)
# - SPRING_DATASOURCE_USERNAME
# - SPRING_DATASOURCE_PASSWORD

ENTRYPOINT ["java","-jar","/app/app.jar"]

