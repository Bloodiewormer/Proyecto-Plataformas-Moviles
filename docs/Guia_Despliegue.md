# Guía de despliegue y conexión

Cómo correr el backend de Glifo en tu máquina o en Render, y cómo generar un APK que se conecte al backend correcto.

| Escenario | Backend | Base de datos | App |
|---|---|---|---|
| Desarrollo en la PC | `./gradlew bootRun` (perfil por defecto) | PostgreSQL local en Docker | build debug en el emulador |
| Producción | Render, imagen Docker (perfil `cloud`) | Neon (PostgreSQL administrado) | APK release firmado |

---

## 1. Requisitos

- JDK 21. Android Studio ya trae uno que sirve.
- Docker Desktop, para la base local y para probar la imagen de producción.
- Android Studio con un emulador (por ejemplo, un Pixel con API 35), o un teléfono con depuración USB.
- Cliente `psql` 18, solo si vas a crear una base de datos nueva en Neon.

---

## 2. Backend en tu máquina

### 2.1 Base de datos

Con Docker, desde la raíz del repo:

```powershell
.\scripts\db-up.ps1
```

El script arranca Docker Desktop si hace falta y levanta el contenedor `glifo-postgres` con el `backend/docker-compose.yml`. Después espera a que la base acepte conexiones. Crea el usuario `glifo` con contraseña `12345` y las bases `glifo` y `glifo_test`.

Sin el script, el equivalente es:

```bash
cd backend
docker compose up -d
```

### 2.2 Levantar la API

```bash
cd backend
./gradlew bootRun
```

- La API queda en `http://localhost:8080/api/v1/`. Para comprobarla: `curl http://localhost:8080/api/v1/health`.
- El perfil por defecto usa `ddl-auto=create`. **Borra y recrea las tablas en cada arranque** y carga `import.sql` con las cuentas demo:

| Rol | Correo | Contraseña |
|---|---|---|
| Estudiante | `estudiante@est.una.ac.cr` | `Estudiante123` |
| Docente | `docente@una.ac.cr` | `Docente123` |
| Admin | `admin@una.ac.cr` | `Admin1234` |

- Las funciones de IA necesitan `OPENAI_API_KEY`. Copiá `backend/.env.example` a `backend/.env` y completala. Sin la clave, la app funciona, pero la reconstrucción con IA, el material de estudio y la adjudicación de cobertura quedan desactivados.

### 2.3 Pruebas

```bash
cd backend
./gradlew test
```

Las pruebas con base de datos usan `glifo_test` con `create-drop`, así que no tocan la base de desarrollo. Hay cinco pruebas de `AiUsageWebTest` que fallan solo en la suite completa, porque otras pruebas dejan llamadas guardadas. Si se corren solas, pasan.

---

## 3. Backend en Render

### 3.1 Cómo está armado

- Render construye `backend/Dockerfile`. La imagen arranca con `--spring.profiles.active=cloud`, no incluye `import.sql` ni cuentas demo, y corre como usuario sin privilegios.
- El perfil `cloud` usa `ddl-auto=validate`. **No crea tablas**: si el esquema de la base no coincide con las entidades, la app no arranca. Ver la sección 3.3.
- El plan gratis de Render apaga el servicio tras unos minutos sin tráfico. La primera petición después tarda hasta un minuto, y la app muestra un aviso mientras tanto.

### 3.2 Crear o actualizar el servicio

1. En Render, abrí **New > Web Service** y elegí el repo `Bloodiewormer/Proyecto-Plataformas-Moviles`. Si el servicio ya existe y apunta al repo del curso, cambialo en **Settings > Build & Deploy > Repository**.
2. **Runtime:** Docker.
3. **Root Directory:** `backend`. **Dockerfile Path:** `./Dockerfile`.
4. **Branch:** `main`. Con Auto-Deploy activado, cada merge a `main` publica una versión nueva.
5. **Health Check Path:** `/api/v1/health`.
6. En **Environment**, cargá las variables de la tabla siguiente. `backend/render.env.example` tiene la plantilla completa.

| Variable | Obligatoria | Qué poner |
|---|---|---|
| `DB_JDBC_URL` | sí | `jdbc:postgresql://HOST/neondb?sslmode=require` (sin usuario ni contraseña en la URL) |
| `DB_USERNAME` | sí | Rol de la aplicación (`glifo_runtime`), no el dueño de la base |
| `DB_PASSWORD` | sí | Contraseña de ese rol |
| `JWT_SECRET` | sí | Al menos 32 caracteres aleatorios. **Nunca** uses el valor por defecto de `application.properties`, que es público. |
| `CORS_ALLOWED_ORIGIN` | sí | Un origen HTTPS. Si solo usás la app Android, la URL del servicio sirve. |
| `OPENAI_API_KEY` | no | Activa las funciones de IA. Cada llamada tiene costo. |
| `OPENAI_MODEL` | no | Por defecto `gpt-4o-mini` |
| `GLIFO_FCM_*` | no | Notificaciones push con Firebase. Por defecto están apagadas. |
| `DB_MAX_POOL_SIZE` | no | Por defecto 5 |

Para generar un `JWT_SECRET`:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

> Si cambiás `JWT_SECRET`, todas las sesiones abiertas se cierran y los usuarios tienen que volver a entrar.

### 3.3 Base de datos en Neon

**Base ya inicializada (la actual).** No hay que hacer nada. Solo aplicá una migración nueva cuando una entidad JPA cambie. Si no, el perfil `cloud` rechaza el arranque.

**Base nueva.** Corré los scripts de `backend/docs/db/` con `psql` 18, en este orden, conectado como dueño de la base y con las credenciales en variables de entorno, nunca en la línea de comandos:

1. `cloud-bootstrap.psql`: crea las tablas, los roles de la app, los privilegios y el rol de login `glifo_runtime`. Toma la contraseña de `GLIFO_RUNTIME_PASSWORD`.
2. `049-fcm-incremental.psql`
3. `046-ai-calls-incremental.psql`
4. `043-topic-coverage-uncertain.psql`
5. `user-avatars-incremental.psql`
6. `study-items-owner-incremental.psql`

Cada script tiene un `.md` al lado que explica qué cambia y cómo revertirlo. Los scripts se niegan a correr dos veces sobre la misma base.

Ejemplo, en PowerShell:

```powershell
$env:PGHOST = "<host directo de Neon, sin -pooler>"
$env:PGDATABASE = "neondb"; $env:PGUSER = "neondb_owner"; $env:PGSSLMODE = "require"
$env:PGPASSWORD = Read-Host "Contraseña de neondb_owner"
$env:GLIFO_RUNTIME_PASSWORD = Read-Host "Contraseña nueva para glifo_runtime"
psql -v ON_ERROR_STOP=1 -f backend/docs/db/cloud-bootstrap.psql
```

La base nueva queda sin usuarios. El primer estudiante o docente se registra desde la app. Para tener un admin, registrá la cuenta y cambiale el rol directamente en la base.

### 3.4 Probar la imagen antes de publicar (opcional)

```bash
cd backend
docker build -t glifo-api:local .
docker run --rm -p 8080:8080 --env-file ../mi-render.env glifo-api:local
```

`mi-render.env` es una copia de `render.env.example` con valores de una base de prueba. Guardala fuera del repo y nunca la subas.

### 3.5 Verificar el despliegue

```bash
curl https://glifo-api.onrender.com/api/v1/health
```

Tiene que responder `200` con `"status":"UP"`. Ese endpoint no consulta la base, así que la prueba real es entrar desde la app.

---

## 4. Conectar la app al backend

La URL del backend se fija al compilar, en `android/app/build.gradle.kts`:

| Build | URL | Cómo cambiarla |
|---|---|---|
| debug | `http://10.0.2.2:8080/api/v1/` (la PC vista desde el emulador) | Editando `debugBaseUrl` |
| release | `https://glifo-api.onrender.com/api/v1/` | `android/local.properties`, propiedad Gradle o variable de entorno |

La URL de release se toma de la primera de estas que exista:

1. `glifo.release.base.url=...` en `android/local.properties`
2. `-PGLIFO_RELEASE_BASE_URL=...` al llamar a Gradle
3. La variable de entorno `GLIFO_RELEASE_BASE_URL`
4. Si no hay ninguna, Render.

**Emulador contra el backend local.** Usá el build debug. `10.0.2.2` ya apunta a tu PC.

**Teléfono físico contra el backend local.** La app solo permite HTTP sin cifrar hacia `10.0.2.2`, `127.0.0.1` y `localhost` (`res/xml/network_security_config.xml`). Para usar el teléfono:

```bash
adb reverse tcp:8080 tcp:8080
```

Después compilá con `glifo.release.base.url=http://127.0.0.1:8080/api/v1/`. Para una IP de la red local o un dominio propio, usá HTTPS, por ejemplo con un túnel como Cloudflare Tunnel o ngrok. La otra opción es agregar ese dominio al `network_security_config.xml`.

**Otro servidor en producción.** Si el backend se muda (otra URL de Render, un VPS, un dominio), cambiá `glifo.release.base.url` y generá un APK nuevo. Las versiones ya instaladas siguen apuntando a la URL vieja.

Las cuentas demo en la pantalla de login aparecen en debug. En release solo aparecen con `glifo.release.show.demo=true` en `local.properties`.

---

## 5. Generar y publicar un APK nuevo

### 5.1 Firma

El APK de release se firma con `glifo-release.jks`. Gradle toma los datos de estas variables de entorno:

```
GLIFO_RELEASE_STORE_FILE      ruta al .jks
GLIFO_RELEASE_STORE_PASSWORD
GLIFO_RELEASE_KEY_ALIAS
GLIFO_RELEASE_KEY_PASSWORD
```

Guardá el keystore y sus contraseñas fuera del repo, en un gestor de contraseñas. **Si perdés el keystore, ninguna versión nueva se va a poder instalar encima de la anterior:** cada usuario tendría que desinstalar la app y perder sus datos locales. Si faltan las variables, Gradle firma con la clave de debug, y ese APK tampoco se instala encima de uno de release.

### 5.2 Pasos

1. Subí `versionCode` en 1 y actualizá `versionName` en `android/app/build.gradle.kts`.
2. Si la versión cambia la base local (Room), agregá una `Migration` en `GlifoDatabase` y registrala en `DatabaseModule`. Sin migración, la app se cierra al abrir sobre datos viejos.
3. Compilá, con las variables de firma cargadas:

   ```powershell
   cd android
   Get-Content $env:USERPROFILE\.glifo-release\glifo-release.properties | ForEach-Object {
       $k, $v = $_ -split '=', 2; if ($k -and $v) { Set-Item "env:$k" $v }
   }
   .\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
   ```

4. Verificá la firma. El SHA-256 tiene que coincidir con el de las versiones anteriores (`803af401…b2cf`):

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\build-tools\<versión>\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
   ```

5. Instalalo encima de la versión anterior para comprobar que no se pierden datos:

   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

6. Publicá el release en GitHub:

   ```bash
   gh release create vX.Y app/build/outputs/apk/release/app-release.apk#glifo-X.Y.apk --title "Glifo X.Y" --notes "..."
   ```

### 5.3 Orden cuando cambian el backend y la app a la vez

1. Si hay migración de base, aplicala primero en Neon.
2. Publicá el backend con un merge a `main`. Render despliega solo. Esperá a que `/api/v1/health` responda.
3. Publicá el APK.

La app tolera campos nuevos que todavía no lleguen del backend, como `page_count`. Al revés no: un backend que deja de mandar un campo que la app necesita la puede romper.

---

## 6. Problemas comunes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| El login tarda casi un minuto | Render estaba dormido | Es normal en el plan gratis |
| La app en el emulador no llega al backend local | El backend no corre, o el build es release | `curl localhost:8080/api/v1/health` y usar el build debug |
| Render no arranca y el log dice `Schema-validation` | Falta una migración en Neon | Aplicar el `.psql` que corresponde (sección 3.3) |
| Render no arranca por `JWT_SECRET` | La variable no está definida | Cargarla en Environment |
| La app se cierra al abrir tras actualizar | Falta una migración de Room | Agregar la `Migration` y subir la versión de la base |
| Android dice "App no instalada" al actualizar | El APK está firmado con otra clave | Firmar con `glifo-release.jks` |
| Las funciones de IA dicen "no configurado" | No hay `OPENAI_API_KEY` | Definirla en `.env` (local) o en Render |
