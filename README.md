# Glifo 

Aplicación Android que reconstruye notas manuscritas de estudiantes en conocimiento estructurado por materia, detecta vacíos frente al syllabus del curso, y guía el estudio adaptativo (flashcards, quizzes, repetición espaciada, plan de examen).

**Curso:** EIF411 - Plataformas Móviles
**Profesor:** Maikol Guzmán
**Equipo:** Grupo X-Ray — Brandon Brenes, David González, Felipe Ugalde

---

## Demo en vivo

**[Ver prototipo interactivo en GitHub Pages](https://bloodiewormer.github.io/Proyecto-Plataformas-Moviles/)**

---

## Principio de diseño

> **"La IA no debe hacer todo."**

El máximo procesamiento posible ocurre local y de forma determinística (OpenCV, ML Kit, reglas, repetición espaciada SM-2) antes de invocar un LLM. Toda llamada a IA produce un resultado persistente y reutilizable — nunca se recalcula con IA algo que ya fue guardado.

Ver `docs/Glifo_Alcance.md` para el detalle completo del flujo de procesamiento y las 4 funciones de IA (IA-01 a IA-04).

---

## Estructura del repositorio

```
glifo/
├── docs/           # Documentación de arquitectura, alcance, UML, decisiones
├── prototype/       # Prototipo interactivo (Figma Make / React+Vite) — 39 pantallas, tandas T1-T5
├── android/         # Cliente Android (Kotlin, Android Studio)
├── backend/         # Backend (Spring Boot, Java, PostgreSQL)
└── .github/         # Plantillas de issues para asignación de tareas
```

## Documentación (`docs/`)

| Documento | Contenido |
|---|---|
| `Glifo_Alcance.md` | Alcance funcional, flujo de procesamiento, funciones de IA |
| `Glifo_Diseno_Arquitectura.md` | Diseño de arquitectura general |
| `Glifo_Arquitectura_Estandares.md` | Estándares de arquitectura, tokens de diseño, UML (§4/§4.1/§4.3) |
| `Glifo_Bitacora_Decisiones.md` | Bitácora de decisiones tomadas y valores deprecados |
| `Glifo_Contexto_Competitivo.md` | Análisis de contexto competitivo |
| `Contexto_Curso.md` | Contexto y requisitos del curso EIF411 |
| `Glifo_UML_Modeling.md` | Modelado UML (diagramas de clase, etc.) |

## Stack técnico

- **Android:** Kotlin, Android Studio, CameraX, Room, OpenCV, Google ML Kit Text Recognition, Retrofit, Hilt, WorkManager
- **Backend:** Spring Boot (Kotlin), PostgreSQL, Spring Security, JWT, JPA
- **OCR matemático:** SimpleTex API (primario) con fallback a modelo de visión LLM
- **IA generativa externa:** Claude / GPT-4o-mini

## Frentes de trabajo

1. **Cliente Android** — UI, cámara, Room, pipeline local
2. **Backend Spring Boot** — API, autenticación, persistencia
3. **Visión por computadora / pipeline** — OpenCV, OCR, detección de gaps vs. syllabus

Ver issues del repositorio para la asignación específica por persona.

## Cómo contribuir

Ver `CONTRIBUTING.md`.
