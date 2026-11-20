# Glifo — Backend API

Backend **monolítico** de Glifo en **Kotlin + Spring Boot**. Es el servidor del modelo
**cliente-servidor**: la app Android (cliente) le hace peticiones HTTP/JSON y el backend
aplica las reglas de negocio y guarda los datos en PostgreSQL.

## Stack

| | |
|---|---|
| Lenguaje | Kotlin 2.4 (JVM 21) |
| Framework | Spring Boot 4.1 (Spring Web, Spring Data JPA, Spring Security) |
| ORM | Hibernate 7, incluido en Spring Data JPA |
| Base de datos | PostgreSQL |
| Build | Gradle 9.7.1 con Kotlin DSL (`build.gradle.kts`) |
| Plugins de Kotlin | `kotlin-spring` (abre las clases de Spring) y `kotlin-jpa` (constructor vacío para las entidades) |

## Arquitectura: 3 capas, organizadas por dominio

El proyecto sigue la arquitectura de **tres capas** vista en clase. La capa de
presentación es la app Android; en este repo están las otras dos, más la capa HTTP que
las expone:

| Capa (clase) | Paquete en cada dominio | Qué hace |
|---|---|---|
| — (API REST) | `controller` + `dto` | Recibe la petición HTTP, valida el JSON y responde |
| **Middle Tier** (lógica de negocio) | `service` | Reglas, validaciones y cálculos |
| **Data Tier** (acceso a datos) | `repository` + `entity` | Patrón Repository (`JpaRepository`) y entidades JPA |

Cada **dominio** tiene su propio paquete con esas capas adentro:

```
cr.ac.una.glifo
├── GlifoApplication.kt      @SpringBootApplication: punto de entrada
├── user/                    usuarios, roles, privilegios, login/registro
├── course/                  cursos, matrículas, temario
├── note/                    apuntes y sus páginas
├── study/                   ítems de estudio, intentos, repaso
├── glossary/                sugerencias de glosario
├── ops/                     dispositivos, cola de sync, notificaciones push FCM, costos de IA
├── ai/                      orquestación de IA (pendiente de labs posteriores)
├── security/                JWT + Spring Security
├── config/                  CORS
└── common/                  respuesta estándar (ApiResponse), errores, health
```

## Cloud deployment

See [the cloud deployment runbook](docs/cloud-deployment.md) for the Docker artifact,
required environment variables, non-destructive cloud profile and pending deployment gates.
This preparation does not imply a live deployment.

## Cómo correrlo

### 1. Crear la base de datos (pgAdmin)

Igual que en clase, la base se crea **antes** de correr el backend:

1. Clic derecho en *Login/Group Roles* → *Create* → *Login/Group Role*
   - Nombre: `glifo`
   - Password: `12345`
   - Privilegios: todos
2. Clic derecho en *Databases* → *Create* → *Database*
   - Nombre: `glifo`
   - Owner: `glifo`

O con SQL, conectado como `postgres`:

```sql
CREATE ROLE glifo WITH LOGIN SUPERUSER PASSWORD '12345';
CREATE DATABASE glifo OWNER glifo;
```

PostgreSQL tiene que estar escuchando en `localhost:5432`.

O con Docker, sin instalar PostgreSQL (crea el mismo usuario y base):

```bash
docker compose up -d
```

### 2. Levantar el backend

```bash
./gradlew bootRun
```

Necesita un JDK 17 o superior para ejecutar Gradle; el código compila con JDK 21, que
Gradle busca solo (sirve el que instala IntelliJ). El JDK que trae Android Studio funciona.

Al arrancar, Hibernate **crea todas las tablas** a partir de las entidades y ejecuta
`src/main/resources/import.sql` (roles, privilegios, cuentas demo y un curso demo con
datos JSONB).

> La base tiene que estar en **UTF-8** (lo normal al instalar PostgreSQL en Windows): el
> curso demo usa acentos y símbolos como `ε` y `δ`.

> `spring.jpa.hibernate.ddl-auto=create` **borra y recrea las tablas en cada arranque**.
> Es lo recomendado mientras se modifican las entidades; cuando haya datos que conservar,
> cambiarlo a `update` en `application.properties`.

### 3. Probar

```bash
curl http://localhost:8080/api/v1/health
```

Cuentas demo (las mismas que muestra la pantalla de login de la app):

| Rol | Correo | Contraseña |
|---|---|---|
| Student | `estudiante@est.una.ac.cr` | `Estudiante123` |
| Teacher | `docente@una.ac.cr` | `Docente123` |
| Admin | `admin@una.ac.cr` | `Admin1234` |

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"estudiante@est.una.ac.cr","password":"Estudiante123"}'
```

### Tests

```bash
./gradlew test
```

Las pruebas que usan base de datos corren contra **`glifo_test`** (perfil `test`), así no
borran los datos de desarrollo. Hay que crearla una vez, conectado como `postgres`:

```sql
CREATE DATABASE glifo_test OWNER glifo;
```

- `ApiContractJsonTest`: verifica que el JSON coincide con el que espera la app. No
  necesita base de datos.
- `WebApiStatusCodesTest`: hace peticiones HTTP reales (MockMvc + login JWT) y verifica el
  código semántico de cada caso: 200, 201 con `Location`, 204, 400 con `subErrors`, 401,
  403, 404, 405, 409 y 415.
- `GlifoApplicationTests`: levanta toda la aplicación, así que necesita la base `glifo_test`.
- `DatabaseSchemaTest`: contra la base `glifo_test`, verifica las 22 tablas, las 7 columnas
  JSONB, que los `CHECK`/`UNIQUE` rechacen datos inválidos, el borrado en cascada y
  consultas con operadores JSONB sobre el curso demo.

## Contrato con la app Android

La API reproduce el mock de Mockoon con el que se desarrolló la app
(`mockoon/Glifo.json` en el repo del frontend), así que la app puede cambiar de Mockoon a
este backend sin modificar sus DTOs. Desde el emulador, la URL es
`http://10.0.2.2:8080/api/v1/`.

| Método | Ruta | Auth | Éxito | Errores |
|---|---|---|---|---|
| GET | `/api/v1/health` | — | 200 `{data: {status, service, timestamp}}` | — |
| POST | `/api/v1/auth/register` | — | 201 `{token, user}` | 400, 409 (correo ya registrado), 415 |
| POST | `/api/v1/auth/login` | — | 200 `{token, user}` | 400, 401 (credenciales), 415 |
| GET | `/api/v1/courses` | JWT | 200 `{data: [curso]}` (cursos del usuario) | 401 |
| POST | `/api/v1/courses` | JWT (`COURSE_WRITE`) | 201 `{data: curso}` + `Location` | 400, 403, 415 |
| GET | `/api/v1/courses/{id}` | JWT | 200 `{data: curso}` | 400, 404 |
| POST | `/api/v1/enrollments` | JWT | 201 `{data: matrícula}` | 400, 404 (código inválido), 409 (ya inscrito) |
| GET | `/api/v1/notes?courseId=` | JWT | 200 `{data: [apunte]}` | 400 |
| POST | `/api/v1/notes` | JWT | 201 `{data: apunte}` + `Location` | 400, 404 (curso), 415 |
| GET | `/api/v1/notes/{id}` | JWT | 200 `{data: apunte}` (incluye `content` JSONB) | 400, 404 |
| PUT | `/api/v1/notes/{id}` | JWT | 200 `{data: apunte}` (`title`, `class_date`, `syllabus_topic_id`) | 400, 404, 415 |
| PATCH | `/api/v1/notes/{id}/status` | JWT | 200 `{data: apunte}`; `{status}` sigue DRAFT → PROCESSING → READY, o ARCHIVED | 400 (estado desconocido), 404, 409 (transición no permitida) |
| DELETE | `/api/v1/notes/{id}` | JWT | 204 sin cuerpo | 400, 404 |
| POST | `/api/v1/notes/{id}/pages` | JWT | 201 `{data: página}` | 400, 404, 409 (ese `page_index` ya existe: la cola offline lo toma como "ya estaba") |
| GET | `/api/v1/notes/{id}/pages` | JWT | 200 `{data: [página]}` con `regions` y `quality_metrics` JSONB | 400, 404 |
| GET | `/api/v1/devices` | JWT | 200 `{data: [dispositivo]}` | 401 |
| POST | `/api/v1/devices` | JWT | 201 / 200 `{data: dispositivo}` (registra o reconcilia por `installation_id`) | 400, 401, 415 |
| PUT | `/api/v1/devices/{id}/token` | JWT | 200 `{data: dispositivo}` (rota `fcm_token` conservando `device_id`) | 400, 401, 403, 404 |
| DELETE | `/api/v1/devices/{id}` | JWT | 204 sin cuerpo (desvincula `fcm_token` al cerrar sesión sin borrar la fila) | 400, 401, 403, 404 |
| POST | `/api/v1/sync/complete` | JWT | 201 / 200 `{data: resultado}` (cierra cola offline y envía push FCM al `device_id`) | 400, 401, 403, 404, 409 (cola incompleta) |

Además, para cualquier ruta: **404** si no existe, **405** si el método no está permitido,
**415** si el `Content-Type` no es `application/json`, y **500** ante un error inesperado
(sin detalles internos: esos quedan en el log del servidor).

Convenciones:

- Las rutas salen de `application.properties` (`url.api.v1`, `url.notes`, …), así una
  futura `v2` no rompe a los clientes de la `v1`.
- Login y registro devuelven `{token, user}` directo; el resto va dentro de `data`.
- `user.role` es `"Student"`, `"Teacher"` o `"Admin"`.
- Los campos de cursos y apuntes van en `snake_case` (`join_code`, `course_id`,
  `class_date`); los de usuario, en `camelCase` (`firstName`).
- El token se manda como `Authorization: Bearer <token>`. Sin token la API responde 401.

### DTOs y validación

Cada recurso tiene **DTOs de entrada** (`*Request`), con solo los campos que el cliente
puede enviar, y **DTOs de salida** (`*Response`), con solo lo que el cliente necesita ver.
Las entidades JPA nunca salen de la API. Los mappers son funciones de extensión de Kotlin
(`Note.toResponse()`, en los paquetes `mapper`).

Los DTOs de entrada se validan con Bean Validation (`@Valid` en el controller y
`@NotBlank`, `@Size`, `@Pattern`, `@Positive`… en los campos). Los `@Size` coinciden con el
largo de cada columna, así un texto demasiado largo es un 400 y no un error de base de
datos. Los ids de ruta también se validan (`@PathVariable @Positive id`).

### Formato de error (`ApiError`)

Todos los errores, incluidos los 401/403 del filtro de seguridad, tienen esta forma:

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "El título es obligatorio",
  "timestamp": "2026-10-12T18:30:00Z",
  "path": "/api/v1/notes",
  "subErrors": [
    { "field": "title", "rejectedValue": "", "message": "El título es obligatorio" }
  ]
}
```

`message` va siempre, en español, y es lo que muestra la app. `subErrors` aparece solo en
los errores de validación. `code` es estable (`VALIDATION_FAILED`, `NOT_FOUND`,
`CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`, …) para que el cliente
decida qué hacer sin depender del texto.

## Base de datos (PostgreSQL + JSONB)

22 tablas: 16 del núcleo (acceso, cursos, apuntes, estudio) y 6 de operaciones
(`ai_calls`, `ai_avoided_calls`, `sync_queue`, `devices`, `notifications`,
`coverage_snapshots`). El detalle
del modelo está en `Glifo_UML_Modeling.md` §4–§5 de la documentación del equipo.

- **El esquema sale de las entidades.** Igual que en clase, no hay scripts de migración:
  cada entidad declara en `@Table` sus `UNIQUE` compuestos, índices y `CHECK` (los enums
  son `VARCHAR` con `CHECK`), y `@OnDelete` define los borrados en cascada.
- **JSONB solo donde la forma varía y nadie filtra por dentro:** `notes.content`,
  `note_pages.regions`, `note_pages.quality_metrics`, `study_items.payload`,
  `attempts.response`, `sync_queue.payload` y `notifications.payload`. En la entidad son
  `String` con `@JdbcTypeCode(SqlTypes.JSON)`; sin esa anotación Postgres rechaza el
  INSERT porque recibe `varchar` para una columna `jsonb`.
- `study_items` lleva `CHECK (payload ->> 'kind' = kind)`: la columna `kind` sirve para
  filtrar y el campo dentro del JSON para deserializar; el `CHECK` las mantiene iguales.

Consultas de ejemplo sobre el curso demo (útiles para la defensa, desde pgAdmin):

```sql
-- Qué tipo de ítem hay en cada fila, sin tablas de preguntas/opciones
SELECT id, payload ->> 'kind' AS kind FROM study_items;

-- La opción correcta de la pregunta de selección única
SELECT opt ->> 'text' FROM study_items, jsonb_array_elements(payload -> 'options') opt
WHERE kind = 'MULTIPLE_CHOICE' AND (opt ->> 'isCorrect')::boolean;

-- Regiones que el pipeline no pudo leer
SELECT r ->> 'id', r ->> 'reason' FROM note_pages, jsonb_array_elements(regions) r
WHERE (r ->> 'isUncertain')::boolean;
```

### Esquema de referencia

`docs/db/schema.sql` es el DDL que genera Hibernate, para leerlo sin levantar el
backend. No se ejecuta. Si se cambian las entidades, se regenera con:

```bash
./gradlew bootRun --args="--server.port=18080 --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=docs/db/schema.sql --spring.jpa.properties.hibernate.hbm2ddl.delimiter=; --spring.jpa.properties.hibernate.format_sql=true"
```

### Adaptación no destructiva de una base existente (Issue #48)

Si se cuenta con una base PostgreSQL existente en modo `update` o `validate` con registros previos en `devices` y `notifications`, se puede adaptar sin perder filas ejecutando:

```sql
ALTER TABLE devices
    ADD COLUMN IF NOT EXISTS installation_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

UPDATE devices
SET installation_id = 'legacy-device-' || id
WHERE installation_id IS NULL;

ALTER TABLE devices
    ALTER COLUMN installation_id SET NOT NULL,
    ALTER COLUMN fcm_token TYPE VARCHAR(512),
    ALTER COLUMN fcm_token DROP NOT NULL;

ALTER TABLE devices
    DROP CONSTRAINT IF EXISTS uq_devices_user_installation,
    ADD CONSTRAINT uq_devices_user_installation UNIQUE (user_id, installation_id);

CREATE INDEX IF NOT EXISTS idx_devices_user_active ON devices (user_id, is_active);

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS device_id BIGINT REFERENCES devices(id),
    ADD COLUMN IF NOT EXISTS idempotency_key UUID UNIQUE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fcm_message_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_error VARCHAR(500),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS ck_notifications_status,
    ADD CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'ACCEPTED', 'FAILED_RETRYABLE', 'FAILED_PERMANENT'));

CREATE INDEX IF NOT EXISTS idx_notifications_device_status ON notifications (device_id, status);
```

## Pendiente para otros issues

- **#24 (Entidades JPA + Repository):** revisión de entidades y relaciones, y carga de
  datos de prueba con `@SpringBootTest` + `@Sql`.
