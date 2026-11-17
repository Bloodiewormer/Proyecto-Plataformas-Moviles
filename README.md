# Glifo — Android Client

The mobile application for **Glifo**, a resilient ingestion engine for handwritten notes, built with Kotlin and Jetpack Compose.

## Architecture

Following the **Screaming Architecture** principle, the project is organized by domain and capability:

- `cr.ac.una.glifo.pipeline`: The core ingestion engine (N0–N3 escalation).
- `cr.ac.una.glifo.feature`: Functional capabilities (auth, capture, note, coverage, study, course, admin).
- `cr.ac.una.glifo.core`: Cross-cutting concerns (network, database, sync, push notifications, UI theme).
- `cr.ac.una.glifo.di`: Hilt dependency injection modules.

## Tech Stack

- **UI:** Jetpack Compose (MVVM)
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp + Gson
- **Persistence:** Room (Local Cache + Offline Queue)
- **Background Work:** WorkManager
- **Image Processing:** OpenCV Android SDK
- **OCR:** ML Kit Text Recognition
- **Camera:** CameraX
- **Push Notifications:** Firebase Cloud Messaging (`firebase-messaging` via Firebase BoM `33.10.0`)

## Mock API (Lab 3)

`NetworkModule.kt` points Retrofit at `http://127.0.0.1:3000/api/v1/`, served locally by
[Mockoon](https://mockoon.com/). Import `mockoon/Glifo.json` into Mockoon and start the
environment before running the app, or its `AuthRepositoryImpl` calls (and the network
security config) will get `AppError.NoConnection`.

`GlifoApi` also declares `courses`, `courses/{id}`, `enrollments`, and `notes` routes, and
the mock has matching responses for all of them — but no repository consumes them yet.
Every repository except `AuthRepositoryImpl` is still backed by `data/datasource/dummy/`
on purpose (see the KDoc on `AuthRepositoryImpl` and `GlifoApi`): Lab 3 only grades the
auth path, so the rest stays dummy-backed until there's a real backend to point at instead
of a hand-maintained mock.

Deterministic error triggers for exercising `AppError` mapping:

| Call | Trigger | Response |
|---|---|---|
| `POST auth/login` | `password: "wrong-password"` | `401` |
| `POST courses` | blank/missing `name` or `code` | `400` |
| `GET courses/{id}` | `id = 999` | `404` |
| `POST enrollments` | `join_code: "INVALID"` | `404` |
| `POST enrollments` | `join_code: "ALREADY"` | `409` |
| `POST notes` | blank/missing `title` | `400` |
| `GET notes/{id}` | `id = 999` | `404` |
| `DELETE notes/{id}` | `id = 999` | `404` |

Anything else on those routes falls through to the happy-path response.

## Notificaciones Push FCM Dirigidas por `device_id` (Lab 6 — Issue #48)

### Identidad de instalación, `device_id` y token FCM
- `TokenManager` genera y conserva un `installation_id` estable (UUID de instalación de la app en `DataStore`, sin leer identificadores físicos sensibles del dispositivo).
- Cuando `GlifoFirebaseMessagingService.onNewToken` recibe un token antes de iniciar sesión, se guarda localmente en `TokenManager`.
- Al iniciar sesión (`AuthRepositoryImpl`) o al arrancar la app con sesión activa (`GlifoApplication`), `DevicePushManager.reconcileDeviceOnSessionStart()` invoca `POST /api/v1/devices` enviando `installation_id`, `platform = "ANDROID"` y el `fcm_token` actual. El servidor devuelve el `device_id` perteneciente a `(user_id, installation_id)`.
- Si el token rota durante una sesión activa, `onNewToken` invoca `PUT /api/v1/devices/{id}/token`.
- Al cerrar sesión (`SessionViewModel.logout()`), `DevicePushManager.unlinkDeviceOnLogoutAsync()` invoca `DELETE /api/v1/devices/{id}` y limpia las claves de sesión en `TokenManager` (preservando `installation_id` y `fcm_token` para el próximo login).

### Detección del fin de la cola offline
- En `SyncRepositoryImpl.syncNow()`, tras procesar las operaciones ejecutables en `sync_operations`, si `pendingCount() == 0` y `failed == 0`, se delega en `DevicePushManager.reportQueueCompletionIfNeeded(uploaded)`.
- Solo se informa al servidor (`POST /api/v1/sync/complete`) cuando el mayor `id` con `status = 'DONE'` supera el último `id` ya reportado para ese usuario (`last_reported_sync_op_<userId>`).
- La solicitud envía una clave de idempotencia determinista calculada sobre `(userId, installationId, fromOpId, toOpId)`. Si la llamada a `completeSync` falla por red o `5xx`, el worker devuelve `RetryLater` y reintenta con la misma `idempotency_key` sin volver a subir las páginas que ya están en `DONE`.

### Recepción, canal y limitaciones del sistema operativo
- El backend envía un mensaje FCM **data-only** (`kind = "SYNC_COMPLETE"`, `event_id`, `device_id`, `user_id`, `uploaded_count`), lo que garantiza que `GlifoFirebaseMessagingService.onMessageReceived` se ejecute de forma consistente tanto en **foreground** como en **background** sin que el sistema operativo duplique la notificación.
- `SyncNotificationPresenter` valida que la preferencia `"Avisar cuando termine la cola"` esté activa, que `user_id` y `device_id` coincidan con la sesión activa, y deduplica por `event_id` en `TokenManager`.
- El texto mostrado indica exclusivamente que terminó la subida de la cola sin conexión desde ese dispositivo, sin afirmar que haya finalizado la reconstrucción o generación por IA.
- **Limitaciones del sistema operativo Android:**
  - En Android 13+ (API 33+) el usuario debe conceder `android.permission.POST_NOTIFICATIONS` (solicitable desde Ajustes o desde la pantalla de Sincronización). Si el permiso es denegado o el canal `glifo_sync_channel` es silenciado, la app omite mostrar la notificación sin fallar.
  - Se requiere un dispositivo o emulador con **Google Play Services** configurado y conectado a internet para recibir mensajes de FCM.
  - Si el usuario fuerza la detención de la aplicación (*Force Stop*) desde los ajustes del sistema o si el fabricante aplica restricciones agresivas de segundo plano (*Doze* / *App Standby*), el sistema operativo retendrá o descartará la entrega hasta que el usuario vuelva a abrir la aplicación.

## Distribución del APK con Firebase App Distribution (Proyecto final — Issue #55)

### URL del backend por tipo de build
- `debug`: `http://10.0.2.2:8080/api/v1/` (backend local o Mockoon visto desde el emulador).
- `release`: `https://glifo-api.onrender.com/api/v1/` (backend desplegado en Render).
- Se define con `BuildConfig.API_BASE_URL` en `app/build.gradle.kts`; `NetworkModule` lo usa.

### Firma de release
La firma nunca se versiona. `app/build.gradle.kts` lee estas cuatro claves de `~/.gradle/gradle.properties` o de variables de entorno; si falta alguna, `assembleRelease` genera un APK sin firmar:

```properties
GLIFO_RELEASE_STORE_FILE=C:/ruta/privada/glifo-release.jks
GLIFO_RELEASE_STORE_PASSWORD=...
GLIFO_RELEASE_KEY_ALIAS=glifo-release
GLIFO_RELEASE_KEY_PASSWORD=...
```

Guardá una copia de respaldo del keystore y su contraseña fuera del repositorio: sin ellos no se pueden publicar actualizaciones que se instalen encima de la versión distribuida.

### Generar, verificar y distribuir
1. Subí `versionCode` (y `versionName`) en `app/build.gradle.kts` antes de cada entrega nueva.
2. Desde la raíz del workspace: `gradlew.bat -p <ruta-del-frontend> :app:assembleRelease`.
3. Verificá la firma: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`.
4. En la consola de Firebase (proyecto `glifoapp`) → App Distribution → app `cr.ac.una.glifo`: subí `app-release.apk`, agregá testers o un grupo y notas de versión, y distribuí.

### Limitaciones
- El APK incluye las bibliotecas nativas de las cuatro arquitecturas (también x86/x86_64 para emuladores), por eso pesa unos 200 MB.
- Render en plan gratuito tarda hasta ~90 s en despertar: la primera petición tras un periodo inactivo puede agotar el tiempo de espera de la app (15 s de conexión); reintentar funciona.

## Documentation

See `docs/Glifo_Arquitectura_Estandares.md` and `docs/claude_Glifo_UML_Modeling.md` for detailed technical specifications.
