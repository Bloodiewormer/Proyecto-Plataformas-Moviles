# Glifo — Backend API

The server-side component for **Glifo**, providing AI orchestration and persistent storage.

##  Architecture

Built with **Kotlin** and Spring Boot, following a domain-driven package structure:

- `cr.ac.una.glifo.ai`: AI Orchestration layer (IA-00 to IA-05).
- `cr.ac.una.glifo.user`, `course`, `note`, `study`: Domain aggregates.
- `cr.ac.una.glifo.security`: JWT and Spring Security configuration.
- `cr.ac.una.glifo.common`: Centralized exception handling and shared utilities.

##  Tech Stack

- **Language:** Kotlin (JVM 21)
- **Framework:** Spring Boot 3.3
- **Persistence:** Spring Data JPA + Hibernate + PostgreSQL
- **Security:** Spring Security + JWT
- **Migrations:** Flyway
- **AI Integration:** SimpleTex API + Vision LLM
- **Math Validation:** JLaTeXMath

## Getting Started

1. Configure environment variables for DB and AI services.
2. Run `./gradlew bootRun` (requires Gradle with Kotlin DSL).

## Documentation

Detailed API specs and diagrams are located in `docs/claude_Glifo_UML_Modeling.md`.
