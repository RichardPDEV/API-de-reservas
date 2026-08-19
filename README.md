# API de Reservas

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Build-Maven-blue.svg)](https://maven.apache.org/)
[![Cache](https://img.shields.io/badge/Cache-InMemory-blue.svg)](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#cache)
[![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791.svg)](https://www.postgresql.org/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-Ready-0db7ed.svg)](https://www.testcontainers.org/)

API REST y frontend web para gestionar reservas con reglas de negocio (capacidad, solapes, cancelación FREE/LATE), disponibilidad diaria cacheada en memoria y migraciones con Flyway. Incluye CI con GitHub Actions y una guía de despliegue productivo con JAR, Nginx y PostgreSQL dedicado.

- Cálculo de día y claves de caché normalizadas en UTC.

---

## Contenido
- [Características](#características)
- [Stack](#stack)
- [Requisitos](#requisitos)
- [Inicio rápido](#inicio-rápido)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Docker / Docker Compose](#docker--docker-compose)
- [Producción](#producción)
- [Tests](#tests)
- [CI (GitHub Actions)](#ci-github-actions)
- [API (endpoints básicos)](#api-endpoints-básicos)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Solución de problemas](#solución-de-problemas)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

---

## Características
- Disponibilidad por recurso y día con caché en memoria (`availability`).
- Invalidación automática de caché al crear o cancelar reservas.
- Reglas de negocio: capacidad, detección de solapes, cancelación FREE vs LATE según política.
- DTOs y controladores REST aislando la lógica de negocio.
- Migraciones con Flyway y pruebas de integración con Testcontainers.

---

## Stack
- Java 21, Spring Boot 3.x, Spring Data JPA, Spring Cache (simple)
- PostgreSQL
- Flyway para migraciones
- JUnit 5, Testcontainers
- Maven

---

## Requisitos
- JDK 21+
- Maven 3.9+
- Docker (recomendado para Postgres y Testcontainers)
- PostgreSQL 15+ (la imagen de desarrollo y el script de PostgreSQL usan 15)

---

## Inicio rápido

1) Copia la configuración local:
```bash
cp .env.example .env
```

2) Levanta dependencias (opcional con Docker Compose):
```bash
docker compose up -d
```

3) Ejecuta la aplicación con el perfil local:
```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

4) Ejecuta tests:
```bash
./mvnw test
```

5) Empaqueta el artefacto:
```bash
./mvnw -DskipTests=true package
```

---

## Configuración

La configuración común está en `src/main/resources/application.yml`; los perfiles están en:
- `application-local.yml`: desarrollo local y Docker Compose.
- `application-staging.yml`: entorno de staging con secretos externos obligatorios.
- `application-prod.yml`: producción real, sin defaults inseguros y con validación de arranque.

Variables principales:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `APP_JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`
- `APP_COOKIE_SECURE`, `APP_COOKIE_SAMESITE`, opcionalmente `APP_COOKIE_DOMAIN`
- `MAIL_HOST=smtp.resend.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=apikey`
- `MAIL_PASSWORD=tu-resend-api-key`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS=true`
- `MAIL_FROM=reservas@tu-dominio.com`

---

## Ejecución

- Desarrollo local:
  ```bash
  SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
  ```
- Staging:
  ```bash
  SPRING_PROFILES_ACTIVE=staging ./mvnw spring-boot:run
  ```
- Producción:
  ```bash
  SPRING_PROFILES_ACTIVE=prod java -jar target/API-de-reservas-0.0.1-SNAPSHOT.jar
  ```

La producción requiere variables externas reales. No copies `.env` de desarrollo al servidor.
Consulta [README.prod.md](README.prod.md) para TLS, Nginx, cookies, health checks, backups, migraciones y el orden de despliegue.

---

## Docker / Docker Compose

Comandos principales:
```bash
# Build + up
docker compose up --build -d

# Ver estado
docker compose ps

# Detener
docker compose down

# Detener y borrar volúmenes
docker compose down -v
```

Compose es únicamente para desarrollo local y activa el perfil `local`. No debe usarse como despliegue de producción ni expone una base de datos gestionada.

Variables de Compose:
- `DB_URL=jdbc:postgresql://postgres:5432/reservas`
- `DB_USERNAME=reservas`
- `DB_PASSWORD=reservas`

---

## Tests

- Ejecutar todas las pruebas:
  ```bash
  ./mvnw test
  ```
- Ejecutar un test concreto:
  ```bash
  ./mvnw -Dtest=ApiSmokeTest test
  ```

Notas:
- Algunos tests usan Testcontainers (requiere Docker activo).
- Para pruebas livianas puedes usar H2 y/o `spring.cache.type=simple` en un perfil de test.

---

## CI (GitHub Actions)

Workflow: `.github/workflows/ci.yml`
- Se ejecuta en `push` y `pull_request`.
- Java Temurin 21.
- Cache de Maven.
- Paso principal:
  ```bash
  mvn -B -q verify
  ```
Opcionales: publicar artefactos del build, reportes JUnit o cobertura (se pueden añadir pasos con `actions/upload-artifact`).

---

## API (endpoints básicos)

Base URL local: `http://localhost:8080`

- POST `/api/reservations`
  - Crea una reserva, valida capacidad/solapes y limpia caché de días afectados.
  - Ejemplo:
    ```bash
    curl -X POST http://localhost:8080/api/reservations \
      -H "Content-Type: application/json" \
      -d '{
        "resourceId": 1,
        "customerName": "Ana",
        "customerEmail": "ana@example.com",
        "partySize": 4,
        "startTime": "2025-01-01T18:00:00Z",
        "endTime": "2025-01-01T20:00:00Z"
      }'
    ```

- POST `/api/reservations/{id}/cancel`
  - Cancela (FREE → CANCELLED, LATE → LATE_CANCELLED) e invalida caché.
    ```bash
    curl -X POST http://localhost:8080/api/reservations/123/cancel \
      -H "Content-Type: application/json" \
      -d '{ "reason": "Cambio de planes" }'
    ```

- GET `/api/resources/{resourceId}/reservations?date=YYYY-MM-DD`
  - Lista reservas del día (UTC).

- GET `/api/resources/{resourceId}/availability?date=YYYY-MM-DD`
  - Ventanas libres cacheadas para el día (UTC).

En local, Swagger está disponible en `http://localhost:8080/swagger-ui.html`. En producción se deshabilita por seguridad.

## Producción

El despliegue real no usa Docker Compose: construye el JAR y el frontend, ejecuta la API en `127.0.0.1:8080`, termina TLS con Nginx y sirve `frontend/dist` como contenido estático.

```bash
./mvnw -DskipTests package
cd frontend
npm install
npm run build
```

Antes de iniciar la API, configura `SPRING_PROFILES_ACTIVE=prod` y todas las variables obligatorias. Verifica `GET /actuator/health/readiness` antes de enviar tráfico. La guía completa, ejemplos Nginx y procedimiento de backup están en [README.prod.md](README.prod.md).


## Solución de problemas

- La aplicación no usa Redis actualmente. Si algo requiere caché persistente en un futuro, puedes habilitar Redis con `spring.cache.type=redis`.

- Diferencias horarias:
  - Tiempos en ISO-8601 con zona (`Z`/offset).
  - Día y claves de caché normalizados a UTC.

- Testcontainers lento en primer uso:
  - Descarga de imágenes; las siguientes ejecuciones serán más rápidas.

---

## Contribuir
- Crea una rama `feat/mi-cambio`, ejecuta `./mvnw clean verify` y abre PR.
- Estilo de commits sugerido: Conventional Commits.

---
