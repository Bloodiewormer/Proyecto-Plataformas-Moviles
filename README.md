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

## Documentation

See `docs/Glifo_Arquitectura_Estandares.md` and `docs/claude_Glifo_UML_Modeling.md` for detailed technical specifications.
