# API de Reservas — Instrucciones de desarrollo y CI

Resumen rápido

- Proyecto: API REST para reservas (Spring Boot, JPA, Redis, Flyway).
- Java: 21
- Build: Maven
- CI: GitHub Actions (`.github/workflows/ci.yml`) ejecuta `mvn -B -q verify` en `push` y `pull_request`.

## Ejecutar localmente con Maven

1. Compilar y ejecutar:

```bash
./mvnw spring-boot:run
```

2. Compilar el artefacto (sin tests):

```bash
./mvnw -DskipTests=true package
```

3. Ejecutar tests (todas):

```bash
./mvnw test
```

4. Ejecutar un test concreto (por ejemplo el smoke test):

```bash
./mvnw -Dtest=ApiSmokeTest test
```

> Nota: algunos tests pueden usar Testcontainers (Postgres/Redis) y por tanto requieren Docker para ejecutarse correctamente. Para ejecuciones livianas el proyecto incluye soporte para H2 en pruebas puntuales.

## Docker / Docker Compose

Se han añadido `Dockerfile` y `docker-compose.yml` para levantar la API junto a Postgres y Redis en red local.

Levantar los servicios con Docker Compose:

```bash
docker compose up --build
```

Levantar en segundo plano:

```bash
docker compose up --build -d
```

Parar y eliminar contenedores (sin borrar volúmenes):

```bash
docker compose down
```

Eliminar también volúmenes (data de Postgres):

```bash
docker compose down -v
```

Variables de entorno en `docker-compose.yml` (ejemplo):

- `SPRING_DATASOURCE_URL` → `jdbc:postgresql://postgres:5432/reservas`
- `SPRING_DATASOURCE_USERNAME` → `reservas`
- `SPRING_DATASOURCE_PASSWORD` → `reservas`
- `SPRING_DATA_REDIS_HOST` → `redis` (si tu app lee `spring.redis.host` puede necesitar adaptación)

Si ves problemas de conexión a Redis desde Spring Boot, revisa la propiedad que utiliza tu app (`spring.redis.host` vs `spring.data.redis.host`).

## GitHub Actions CI

Workflow añadido: `.github/workflows/ci.yml`

- Dispara en `push` y `pull_request`.
- Ejecuta en `ubuntu-latest` con Temurin Java 21.
- Usa cache de Maven (configurado por `actions/setup-java@v4`).
- Comando principal: `mvn -q -B verify` (compila, empaqueta y ejecuta tests).

Notas:
- Los runners de GitHub Actions permiten ejecutar Testcontainers; si tus tests usan Testcontainers, no es necesario añadir servicios extra en el workflow. Si prefieres usar `docker-compose` en el runner o añadir servicios dedicados (postgres/redis) en la matrix del job, avísame y lo amplio.
- Si quieres publicar artefactos (JAR) o reportes (JUnit, cobertura) puedo añadir pasos para almacenar esos artefactos como `actions/upload-artifact`.

## Problemas comunes y debugging

- Error de ApplicationContext en tests (bean no encontrado): asegúrate que el paquete raíz del `@SpringBootApplication` cubra los paquetes con tus repositorios y controladores. En pruebas podemos forzar una `@ComponentScan` de `com.example.reservas` si es necesario.
- Testcontainers falla por falta de Docker: instala Docker Desktop y asegúrate de que el daemon está corriendo.
- Si Flyway intenta conectarse a una base externa durante pruebas, desactiva Flyway en las propiedades de test: `spring.flyway.enabled=false` y usa H2 para pruebas rápidas.

## Próximos pasos sugeridos

- Añadir un `README-DEV.md` con checklist para crear una reserva completa (crear negocio → resource → reservation → cancelar) y comandos de test end-to-end.
- Añadir pasos al CI para publicar resultados de tests y reportes de cobertura.

---

Si quieres, genero ahora un `./github/workflows/ci.yml` más avanzado que incluya: cache de dependencias, subida de artefactos, y reportes JUnit/coverage. O puedo adaptar el `docker-compose.yml` para ajustar variables `SPRING_REDIS_HOST` según lo que tu app espere.