# Glifo — Android Client

The mobile application for **Glifo**, a resilient ingestion engine for handwritten notes, built with Kotlin and Jetpack Compose.

##  Architecture

Following the **Screaming Architecture** principle, the project is organized by domain and capability:

- `cr.ac.una.glifo.pipeline`: The core ingestion engine (N0–N3 escalation).
- `cr.ac.una.glifo.feature`: Functional capabilities (auth, capture, note, coverage, study, course, admin).
- `cr.ac.una.glifo.core`: Cross-cutting concerns (network, database, sync, UI theme).
- `cr.ac.una.glifo.di`: Hilt dependency injection modules.

##  Tech Stack

- **UI:** Jetpack Compose (MVVM)
- **Dependency Injection:** Hilt
- **Networking:** Retrofit + OkHttp + Gson
- **Persistence:** Room (Local Cache + Offline Queue)
- **Background Work:** WorkManager
- **Image Processing:** OpenCV Android SDK
- **OCR:** ML Kit Text Recognition
- **Camera:** CameraX

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

## Documentation

See `docs/Glifo_Arquitectura_Estandares.md` and `docs/claude_Glifo_UML_Modeling.md` for detailed technical specifications.
