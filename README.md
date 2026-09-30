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
├── ops/                     dispositivos, cola de sync, notificaciones, costos de IA
├── ai/                      orquestación de IA (pendiente de labs posteriores)
├── security/                JWT + Spring Security
├── config/                  CORS
└── common/                  respuesta estándar (ApiResponse), errores, health
```

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

### 2. Levantar el backend

```bash
./gradlew bootRun
```

Necesita un JDK 17 o superior para ejecutar Gradle; el código compila con JDK 21, que
Gradle busca solo (sirve el que instala IntelliJ). El JDK que trae Android Studio funciona.

Al arrancar, Hibernate **crea todas las tablas** a partir de las entidades y ejecuta
`src/main/resources/import.sql` (roles, privilegios y cuentas demo).

> ⚠️ `spring.jpa.hibernate.ddl-auto=create` **borra y recrea las tablas en cada arranque**.
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

- `ApiContractJsonTest`: verifica que el JSON coincide con el que espera la app. No
  necesita base de datos.
- `GlifoApplicationTests`: levanta toda la aplicación, así que necesita la base `glifo`.

## Contrato con la app Android

La API reproduce el mock de Mockoon con el que se desarrolló la app
(`mockoon/Glifo.json` en el repo del frontend), así que la app puede cambiar de Mockoon a
este backend sin modificar sus DTOs. Desde el emulador, la URL es
`http://10.0.2.2:8080/api/v1/`.

| Método | Ruta | Auth | Respuesta |
|---|---|---|---|
| GET | `/api/v1/health` | — | `{data: {status, service, timestamp}}` |
| POST | `/api/v1/auth/register` | — | `{token, user}` |
| POST | `/api/v1/auth/login` | — | `{token, user}` |
| GET | `/api/v1/courses` | JWT | `{data: [curso]}` (cursos del usuario) |
| POST | `/api/v1/courses` | JWT (`COURSE_WRITE`) | `{data: curso}` |
| GET | `/api/v1/courses/{id}` | JWT | `{data: curso}` |
| POST | `/api/v1/enrollments` | JWT | `{data: matrícula}` |
| GET | `/api/v1/notes?courseId=` | JWT | `{data: [apunte]}` |
| POST | `/api/v1/notes` | JWT | `{data: apunte}` |
| GET | `/api/v1/notes/{id}` | JWT | `{data: apunte}` |
| DELETE | `/api/v1/notes/{id}` | JWT | 204 sin cuerpo |

Convenciones:

- Login y registro devuelven `{token, user}` directo; el resto va dentro de `data`.
- `user.role` es `"Student"`, `"Teacher"` o `"Admin"`.
- Los campos de cursos y apuntes van en `snake_case` (`join_code`, `course_id`,
  `class_date`); los de usuario, en `camelCase` (`firstName`).
- Los errores son `{"message": "..."}` en español, con 400, 401, 403, 404 o 409.
- El token se manda como `Authorization: Bearer <token>`. Sin token la API responde 401.

## Pendiente para otros issues

- **#23 (PostgreSQL + JSONB):** `db/migration/V1__baseline.sql` y Flyway siguen en el
  proyecto pero **desactivados** (`spring.flyway.enabled=false`), porque ahora el esquema
  lo genera Hibernate. Las columnas JSONB de las entidades se mapean como `String` con
  `columnDefinition = "jsonb"`; para escribir en ellas hace falta
  `@JdbcTypeCode(SqlTypes.JSON)`.
- **#24 (Entidades JPA + Repository):** revisión de entidades y relaciones, y carga de
  datos de prueba con `@SpringBootTest` + `@Sql`.
