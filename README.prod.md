# Producción con servidor propio

Si ya tienes el servidor y los subdominios preparados, este proyecto está listo para desplegarse **sin Docker Compose** y con validaciones estrictas en el perfil `prod`.

## 1) Variables de entorno

Copia los ejemplos y reemplaza los valores reales:

```bash
cp .env.example .env
cp frontend/.env.production.example frontend/.env.production
```

## 2) Variables obligatorias en producción

Con `SPRING_PROFILES_ACTIVE=prod`, la aplicación **no arranca** si falta alguna de estas variables o si usan valores inseguros:

| Variable | Requisito |
|----------|-----------|
| `DB_URL` | JDBC de PostgreSQL (sin default) |
| `DB_USERNAME` | Usuario de base de datos |
| `DB_PASSWORD` | Contraseña segura; rechaza `changeme`, `password`, `postgres`, `reservas` |
| `APP_JWT_SECRET` | Mínimo 32 caracteres, aleatoria; no placeholders |
| `APP_CORS_ALLOWED_ORIGINS` | Origen(es) **HTTPS** del frontend; no `localhost` |
| `APP_COOKIE_SECURE` | Debe ser `true` (HTTPS obligatorio) |
| `APP_COOKIE_SAMESITE` | `None`, `Strict` o `Lax`; usa `None` si frontend y API están en dominios distintos |
| `MAIL_HOST` | Servidor SMTP real (no `localhost`) |
| `MAIL_PASSWORD` | Credencial SMTP/API key real; no placeholders |
| `MAIL_FROM` | Remitente verificado en tu proveedor (no `@example.com`) |

### HTTPS y proxy inverso

- Termina TLS en Nginx delante del JAR y del frontend estático.
- La API usa `forward-headers-strategy: framework` para respetar `X-Forwarded-Proto`, `X-Forwarded-Host` y `X-Forwarded-Port`.
- Expón la API **solo en localhost** (`127.0.0.1:8080`); Nginx es la única entrada pública.
- Con cookies cross-site (`SameSite=None`), **`APP_COOKIE_SECURE=true` es obligatorio** (la cookie lleva `Secure`; el navegador solo la envía por HTTPS).

Ejemplos de configuración Nginx (copiar y ajustar dominios/certificados):

| Servicio | Archivo |
|----------|---------|
| API (TLS → JAR en `:8080`) | `deploy/nginx/api.conf.example` |
| Frontend estático (TLS → `dist/`) | `deploy/nginx/frontend.conf.example` |

Tras copiar los `.conf`, valida y recarga:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

El bloque HTTP de cada ejemplo redirige con `301` a HTTPS (incluye desafío ACME en `/.well-known/acme-challenge/`).

### CORS

- Define **solo** el origen del frontend en producción, por ejemplo:
  `APP_CORS_ALLOWED_ORIGINS=https://reservas.tu-dominio.com`
- No uses orígenes `http://` ni `localhost` en prod.

### Cookies de refresh token

Ejemplo cuando frontend y API están en subdominios distintos:

```bash
APP_COOKIE_SECURE=true
APP_COOKIE_SAMESITE=None
APP_COOKIE_DOMAIN=.tu-dominio.com
```

### OpenAPI / Swagger

Con `SPRING_PROFILES_ACTIVE=prod`, la documentación interactiva del API **no está expuesta**:

- `springdoc.api-docs.enabled=false` y `springdoc.swagger-ui.enabled=false` en `application-prod.yml`
- `SecurityConfig` responde **403 Forbidden** a `/v3/api-docs/**` y `/swagger-ui/**`
- `OpenApiConfig` solo se carga fuera del perfil `prod`

En desarrollo local (sin perfil `prod`), Swagger sigue disponible en `http://localhost:8080/swagger-ui.html`.

## Health checks (Docker / K8s / balanceador)

No uses `/api/health` (endpoint eliminado; siempre devolvía `"OK"` sin comprobar dependencias).

Con `SPRING_PROFILES_ACTIVE=prod`, Spring Actuator expone probes públicos (sin autenticación):

| Probe | URL | Uso |
|-------|-----|-----|
| **Readiness** | `GET /actuator/health/readiness` | Balanceador / K8s readiness — incluye conexión a PostgreSQL |
| **Liveness** | `GET /actuator/health/liveness` | K8s liveness — proceso vivo |

- Respuesta **200** con `"status":"UP"` → instancia sana.
- Respuesta **503** con `"status":"DOWN"` → no enviar tráfico (p. ej. DB caída).

### Docker

El `HEALTHCHECK` del Dockerfile apunta a `/actuator/health/readiness`.

### Kubernetes (ejemplo)

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Nginx / balanceador

Configura el health check contra `/actuator/health/readiness` y considera la instancia caída si el código HTTP no es 2xx.

## 3) Configuración recomendada para tu subdominio

- Frontend: `https://reservas.tu-dominio.com`
- API: `https://api.reservas.tu-dominio.com`

### Backend (`.env` en el servidor)

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080

DB_URL=jdbc:postgresql://tu-host:5432/reservas
DB_USERNAME=reservas
DB_PASSWORD=tu-password-segura
APP_JWT_SECRET=una-clave-larga-y-aleatoria-de-al-menos-32-chars

APP_CORS_ALLOWED_ORIGINS=https://reservas.tu-dominio.com
APP_COOKIE_SECURE=true
APP_COOKIE_SAMESITE=None
APP_COOKIE_DOMAIN=.tu-dominio.com

MAIL_HOST=smtp.resend.com
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=tu-resend-api-key
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_FROM=reservas@tu-dominio.com
```

### Frontend (`frontend/.env.production`)

```bash
VITE_API_BASE_URL=https://api.reservas.tu-dominio.com
```

## 4) Build del backend

```bash
./mvnw -DskipTests package
```

## 5) Build del frontend

```bash
cd frontend
npm install
npm run build
```

## 6) Publicación en tu servidor

Sube el JAR generado por el backend y los archivos de `frontend/dist` a tus rutas correspondientes.

1. **API:** ejecuta el JAR escuchando en `127.0.0.1:8080` (no expongas el puerto 8080 a Internet).
2. **Nginx API:** activa `deploy/nginx/api.conf.example` apuntando a `http://127.0.0.1:8080`.
3. **Frontend:** copia `frontend/dist` a la ruta `root` del bloque HTTPS en `deploy/nginx/frontend.conf.example`.

Verifica redirección HTTP→HTTPS y cookies:

```bash
curl -I http://api.reservas.tu-dominio.com/actuator/health/liveness   # debe devolver 301 → https://...
curl -I https://api.reservas.tu-dominio.com/actuator/health/liveness  # 200
# Tras login, Set-Cookie debe incluir Secure, HttpOnly y SameSite=None (si usas subdominios distintos)
```

## 7) PostgreSQL dedicado (opcional)

Para levantar solo la base de datos en el servidor:

```bash
DB_PORT=5432 DB_PASSWORD=tu-password-segura ./deploy/run-postgres.sh
```

## Docker Compose vs producción real

`docker-compose.yml` está pensado para **desarrollo local** (perfil por defecto, cookies no seguras, CORS localhost). **No** activa `SPRING_PROFILES_ACTIVE=prod`. Para producción real sigue esta guía y despliega el JAR con las variables obligatorias anteriores.
