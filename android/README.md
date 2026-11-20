# Glifo · App Android

App de Glifo en Kotlin y Jetpack Compose: captura apuntes, los procesa en el teléfono, sincroniza con el backend y arma el estudio.

## Arquitectura

Capas al estilo Clean Architecture, con MVVM en la presentación:

| Paquete | Contenido |
|---|---|
| `presentation` | Pantallas Compose (`ui/screen`), ViewModels y navegación (`GlifoNavHost`, barra inferior por rol en `GlifoBottomBar`) |
| `domain` | Modelos, interfaces de repositorio y errores (`AppError`, `Result`) |
| `data` | Repositorios, Retrofit (`GlifoApi`), DTOs, mappers y Room (`GlifoDatabase`) |
| `core/pipeline` | Pipeline de captura: calidad (N0), OCR local (N1), clasificación de regiones y hash perceptual |
| `core/opencv`, `core/mlkit`, `core/confidence` | Segmentación con OpenCV, ML Kit y puntaje de confianza por fragmento |
| `core/sync` | Cola sin conexión (`sync_operations`) procesada por WorkManager con reintentos |
| `core/push`, `core/reminder`, `core/telemetry` | FCM, recordatorios locales de estudio y Crashlytics |
| `core/ui` | Tema, tokens de color y componentes compartidos (`GlifoComponents`) |

## Stack

Jetpack Compose (Material 3), Hilt, Retrofit con OkHttp y Gson, Room, WorkManager, CameraX, ML Kit Text Recognition, OpenCV, KaTeX (sin conexión, en `assets`), Firebase Crashlytics y Cloud Messaging.

## Correr

1. Levantá el backend local (ver la [guía de despliegue](../docs/Guia_Despliegue.md#2-backend-en-tu-máquina)).
2. Abrí esta carpeta (`android/`) en Android Studio y corré el build **debug** en un emulador. Apunta a `http://10.0.2.2:8080/api/v1/`, que es tu PC vista desde el emulador.
3. Pruebas unitarias: `./gradlew :app:testDebugUnitTest`.

El build **release** apunta a Render y se firma con el keystore del proyecto. La guía explica cómo cambiar la URL, conectar un teléfono físico y publicar una versión nueva.

El APK de release incluye solo `arm64-v8a` y `armeabi-v7a`. Funciona en teléfonos y en emuladores x86_64 recientes que traducen ARM.

## Sincronización sin conexión

Cada captura se guarda primero en Room y crea operaciones en `sync_operations` (`CREATE_NOTE`, `UPLOAD_PAGE`) con una clave de idempotencia. WorkManager procesa la cola en orden cuando hay red, con reintentos exponenciales. Una página espera a que su apunte tenga id del servidor, y un `409` al subirla se toma como éxito, así que un corte de red nunca duplica páginas.

## Notificaciones push por dispositivo (FCM)

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

## Documentación

- [Guía de despliegue](../docs/Guia_Despliegue.md): backend, URL del APK, firma y releases.
- [Estándares de arquitectura](../docs/Glifo_Arquitectura_Estandares.md): capas, tokens de diseño y componentes.
- [Modelado UML](../docs/Glifo_UML_Modeling.md).
