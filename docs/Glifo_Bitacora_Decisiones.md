# Glifo — Decision Log

Historical record of the project. **This file does not describe the project**: it describes how we arrived at it.

Working documents (`Glifo_Alcance.md`, `Glifo_Arquitectura_Estandares.md`) are written in the present tense and describe only what is going to be built. Any reverted decision, discarded alternative, or older version lives here and **must not be reintroduced** there.

---

## Decision Index

| ID | Decision | Status |
|---|---|---|
| D-01 | Strategic direction: resilient ingestion engine | Closed |
| D-02 | Project name: Glifo | Closed |
| D-03 | Three user roles | Closed |
| D-04 | PostgreSQL main, Room as cache | Closed |
| D-05 | Branched processing ladder | Closed |
| D-06 | Math OCR engine: SimpleTex | Closed |
| D-07 | Self-hosted pix2tex | **Open** — pending professor feedback |
| D-08 | Coverage comparison: hybrid | Closed |
| D-09 | Selective JSONB, not generalized | Closed |
| D-10 | Offline and sync as a core pillar | Closed |
| D-11 | Custom telemetry + Firebase Analytics | Closed |
| D-12 | Palette: two modes, gold as accent, escalated isolated | Closed |
| D-13 | Backend Language: Kotlin + Spring Boot | Closed |
| D-14 | Documentation Language: English | Closed |
| D-15 | Student Corrections & Glossary: Suggestions model | Closed |
| D-16 | Note Ingestion Cardinality: DRAFT state | Closed |
| D-17 | Offline Context: LocalCourseContext | Closed |
| D-18 | Sync Push targeting: device_id | Closed |
| D-19 | Serialization library: Gson | Closed |
| D-20 | AI function numbering: IA-04 replaces IA-05 | Closed |
| D-21 | `ai_calls` view and targeted push are Must-have | Closed |
| D-22 | LaTeX validation runs on the backend | Closed |
| D-23 | Sync operation ordering: CREATE_NOTE before UPLOAD_PAGE | Closed |
| D-24 | Android package structure: Clean layer-first, screaming architecture discarded | Closed |
| D-25 | Mock API tool for Lab 3: Mockoon | Closed |
| D-26 | Lab 3 fully wired to Mockoon, gallery capture, language selector | Closed |
| D-27 | Database schema generated from JPA entities + `import.sql`; Flyway dropped | Closed |
| D-28 | Room activated for the offline pipeline: `LocalCourseContext` (#27) and pHash deduplication (#28) | Closed |
| D-29 | Room v2 with notes + outbox; results and sync screens on real data (#25, #26) | Closed |

---

## D-01 · Strategic direction

**Decision.** Glifo retains the course brief's domain —notes, coverage against syllabus, reinforcement— but shifts its product axis: from "a note app that saves tokens" to a **resilient ingestion engine for handwritten notes**.

**Alternatives considered and discarded**

| Option | Reason for discard |
|---|---|
| Keep the original proposal unchanged | The differentiator (preprocessing and token economy) was invisible to the user and did not address OCR fragility. |
| Total problem rethink (exam autopsy, collaborative note bank, camera-less study engine) | The brief is common to all nine groups; changing the problem steps out of the assigned framework and discards prior work. |

**Rationale.** The brief itself identifies OCR on poor-quality handwriting as its main risk, proposing manual student correction as mitigation. That mitigation does not scale for courses with mathematical notation. The territory of reliable ingestion was unoccupied by any other group.

---

## D-02 · Name

**Decision.** The project is named **Glifo**.

**Context.** It could not retain the name NotaViva: that is the course's guide brief (proposed by Group Charlie), and the professor requested distinct names per team.

**Alternatives evaluated**

| Candidate | Result |
|---|---|
| **Glifo** | **Chosen.** Minimum unit of writing; describes exactly what the app processes. |
| Grifo (Griffin) | Discarded as a name: in much of the Spanish-speaking world it means "water faucet/tap". Retained as the **base of the visual identity**. |
| Runa, Calco, Nitidez | Discarded for lower semantic precision or app-store saturation. |

---

## D-03 · Roles

**Decision.** Three roles: student, teacher, administrator.

**Reason for change.** Initial design had a single role. The professor marked a minimum of two roles as mandatory. The guide brief envisions three. 

**Differentiator.** The teacher role is not just a dashboard: it maintains the course's **canonical notation glossary**, which directly feeds the pipeline's confidence calculation.

---

## D-04 · Persistence

**Decision.** PostgreSQL as the primary database; Room as local cache and sync queue.

**Reason for change.** The initial design was local-first with Room as the main storage. PostgreSQL is mandatory for all nine groups, and AI service credentials cannot reside in the APK, demanding a backend regardless.

---

## D-05 · Processing ladder

**Decision.** Branched pipeline: N0 preprocessing → N1 local text OCR on all regions → classifier as router → N1.5 math OCR for formulas → N2 selective vision repair for unresolved regions and drawings → N3 full page on demand.

**Order decision.** The classifier runs **after** N1, not before. ML Kit is local and free; its behavior on a region (fragmented output vs coherent text) is a better classification signal than geometry alone.

---

## D-06 · Math OCR engine

**Decision.** SimpleTex as the primary implementation of `MathOcrEngine`, with automatic fallback to the vision model.

**Evaluation criteria.** The engine must deliver a confidence score **derived from the model**, allowing automatic acceptance or escalation routing. SimpleTex offers this and a permanent free tier. Mathpix was discarded (paid only), Tesseract (weak on handwriting), ML Kit Digital Ink (requires screen strokes, not photos).

---

## D-07 · Self-hosted pix2tex — **open**

**Status.** Unresolved. Pending consultation with the professor.

**Proposal.** Deploy pix2tex as our own REST microservice and consume it as another `MathOcrEngine` implementation.
**Questions for the professor:**
1. Is an independent Python microservice permissible, or does it contradict the monolithic architecture requirement?
2. Does self-hosting add evaluation value over consuming a free external service?
3. Is university infrastructure available?
4. Is cold-start latency acceptable for the final defense?

Regardless of the outcome, pix2tex runs locally on the calibration set for the scientific article comparison.

---

## D-08 · Coverage comparison

**Decision.** Deterministic local pre-filter resolves most topics; only ambiguous cases are sent to AI semantic adjudication.

**Reason for change.** Previous design was entirely local. The guide brief justifies the project's selection because it uses three distinct AI calls: vision, semantic comparison, and generation. The hybrid approach preserves the semantic call while applying the pipeline's escalation policy.

---

## D-09 · JSONB

**Decision.** Selective application based on documented criteria, not generalized.

**Adopted criteria:** JSONB where the structure is variable and no internal field is queried (e.g., `study_items.payload`, `notes.content`, `note_pages.regions`). Relational modeling where there is referential integrity or frequent aggregation (`topic_coverage`, `users`).

---

## D-10 · Offline and sync

**Decision.** Core product pillar, not secondary functionality.

**Rationale.** It corresponds to the applied research topic assigned to the team.
**Scope:** Offline consumption, offline capture queue, unidirectional sync with exponential backoff and idempotency. Bidirectional conflict resolution is future work.

---

## D-11 · Telemetry

**Decision.** Custom `ai_calls` ledger + Firebase Analytics and Crashlytics.

**Rationale.** The project's central argument is quantitative: the proportion of regions resolved at each pipeline level must be proven with data, not asserted. Crashlytics is included because OpenCV and ML Kit are native libraries whose crashes don't reproduce in standard emulators.

---

## D-12 · Palette and Visual Identity

**Decision.** Two complete modes (Day and Night) with the same token structure, and **gold as the color of action**. The palette materialized in the five-batch prototype is the canonical one.

**Reason for the change.** The original palette was born on paper and was never tested on real screens. Producing the 39 frames exposed the problem: the turquoise accent `#3FC5C0` and the heraldic gold `#C9A227` were competing — two saturated colors from different families fighting for attention on the same screen, with neither clearly dominant. Unifying on gold left a single interaction signal and freed the turquoise entirely.

**Consequences.**

1. **`heraldic` is removed.** Heraldic gold and the accent are now the same color, so gold can no longer be used as brand decoration: if it appears, the element can be touched or is active. Brand identity moves entirely onto the griffin logo.
2. **Tokens promoted to first class:** `surfaceHigh` and `border`; the `Soft` / `Faint` / `Line` variant set systematized across every semantic color; `accentText` and `onAccent` added to resolve gold's contrast in both modes.
3. **Colour collision resolved (closed before Lab 3).** `escalated` and `alert` shared `#E0693A` / `#B94117`. Escalating to a vision model is normal pipeline operation, not a failure, and presenting it in the error color contradicted the central argument of the product. `escalated` moved to its own amber: **`#F59E0B` (Night) / `#D97706` (Day)**. `alert` keeps the rust.
4. **Geometric trust coding retired from inline text.** The old symbol set —filled circle, square, triangle, dotted circle— did not survive contact with running text: there is nowhere to put a triangle inside a paragraph. Replaced by the dual-coding rule in `Glifo_Arquitectura_Estandares.md` §12.3: color + underline style (solid vs dotted) + background fill + explicit textual label. Geometric markers survive only as compact status chips in lists, never as the sole carrier.

**Why dual coding at all.** Roughly 8 % of men have some color vision deficiency, and green/amber is the worst-discriminated pair. If the confidence map depended on color alone, the application's central function would be inaccessible to those users.

### Previous values — do not reintroduce

| Token | Discarded version |
|---|---|
| `background` | `#0B1420` |
| `surface` | `#132234` |
| `accent` | `#3FC5C0` |
| `heraldic` | `#C9A227` |
| `textPrimary` | `#E8E4D9` |
| `textSecondary` | `#8FA3B8` |
| `alert` | `#D85A30` |
| Verified | `#5DCAA5` |
| Repaired | `#85B7EB` |
| Escalated | `#EF9F27` |
| Uncertain | `#F0997B` |
| `escalated` (interim, collided with `alert`) | `#E0693A` / `#B94117` |

**Propagation status.** Applied in `Glifo_Alcance.md`, `Glifo_Arquitectura_Estandares.md`, `Glifo_Diseno_Arquitectura.md`, `Glifo_UML_Modeling.md`, and across the prototype: `src/App.tsx`, the five wireframe batches, `Glifo_Paleta_Modos.html` and `Glifo_Biblioteca_Componentes.md`. The narrative that defended the shared token was rewritten in the last two rather than deleted, so the reversal is on record.

Also discarded: the geometric-figure encoding (filled circle, square, triangle, dotted circle) as the primary carrier, and `accent` and `accentText` treated as a single token — in Day mode `accent` is `#FFD372` (a fill) while `accentText` is `#8A6210` (gold as text). Collapsing them turns every primary button brown.

---

## D-13 · Backend Language

**Decision.** Kotlin + Spring Boot.

**Rationale.** Unifies the language across the repository, allowing code review in both directions. Eliminates Lombok and Mockito (replaced by MockK). The `kotlin-spring` and `kotlin-jpa` compiler plugins are mandatory to handle `final` classes and no-arg constructors cleanly.

---

## D-14 · Documentation Language

**Decision.** English for all code, identifiers, DB objects, API paths, payloads, commit messages, and technical documentation. Spanish is strictly reserved for user-facing UI text.

**Rationale.** Aligns with the scientific article requirement and eliminates "Spanglish" in the codebase.

---

## D-15 · Student Corrections and Glossary

**Decision.** Student corrections update their personal note (`notes.content`) and generate a `glossary_suggestion`. 

**Rationale.** The teacher role has the exclusive `GLOSSARY_WRITE` privilege. Allowing students to write directly to `course_glossary` violates the security model. The suggestion model maintains collaborative learning while keeping the canonical glossary safe and architecture clean.

---

## D-16 · Note Creation and Cardinality

**Decision.** A `Note` can have 0 pages temporarily during the `DRAFT` state.

**Rationale.** Eliminates the database contradiction where `POST /notes` followed by `POST /notes/{id}/pages` broke the `1..N` cardinality rule. The state machine (`DRAFT` → `PROCESSING` → `READY`) handles this properly.

---

## D-17 · Offline Context for Pipeline

**Decision.** Room stores a version-controlled `LocalCourseContext`.

**Rationale.** The `PipelineEngine` and `ConfidenceScorer` need glossary data to function offline. Copying the entire DB is unfeasible. `LocalCourseContext` holds just enough data (with `glossary_version`) to power the pipeline, enabling cache invalidation upon reconnection.

---

## D-18 · Push Notifications for Sync

**Decision.** `sync_queue` records must include `device_id` (FK to `devices`).

**Rationale.** If a user has a phone and a tablet, the FCM "Sync complete" push must only target the physical device that uploaded the pending batch.

---

## D-19 · Serialization Library

**Decision.** Gson.

**Rationale.** Resolves the conflict between the course framework and the architecture document. `kotlinx.serialization` is powerful for sealed classes, but Gson was already presented in earlier design iterations to the professor and aligns with standard Retrofit integrations in this context.

**Consequence.** `study_items.payload` carries three item kinds (`Glifo_UML_Modeling.md` §5.3). Without a sealed-class resolver, deserialization dispatches on the `kind` discriminator by hand. That is a small amount of explicit code in one place, which is the price paid for not reversing a decision the professor has already seen.

---

## D-20 · AI function numbering

**Decision.** Semantic coverage adjudication is **IA-04**. The identifier IA-05 is retired.

**Reason.** An earlier plan reserved IA-04 for *contradiction detection between notes*. That function was removed from scope (high false-positive risk; flagging a contradiction that does not exist destroys trust in the product), leaving a gap in the numbering: the inventory ran OCR-M · IA-00 · IA-01 · IA-02 · IA-03 · IA-05, with no IA-04. A gap with no written explanation is exactly the kind of detail that gets asked about in a defense. Adjudication was renumbered down to close it.

**Current inventory:** OCR-M (math OCR), IA-00 (selective vision repair), IA-01 (note reconstruction), IA-02 (flashcards and quizzes, batch), IA-03 (on-demand explanation), IA-04 (coverage semantic adjudication, batch).

---

## D-21 · Priority of the `ai_calls` view and the targeted push

**Decision.** Both are **Must-have**.

**Reason — `ai_calls` view (H2).** An earlier revision marked the consumption screen as Optional in `Glifo_Alcance.md` §6.6 while §12 listed it as minimum-product item 9 and §14 spends minute 4:15 of the defense on it. The *recording* of `ai_calls` was never in dispute; only the screen. The cost argument is the project's thesis, and a thesis with no screen behind it is an assertion. Promoted to Must-have; §6.6, §12, §14 and `Glifo_UML_Modeling.md` §12.1 now agree.

**Reason — targeted push (D-18).** Same shape of contradiction: §6.5 had it as Should-have while the MVP (§12, item 8) ends the offline flow with "notify via push", and the applied-research topic is offline and synchronization. Either it is Must-have or it comes out of the MVP and out of every claim that sync completes with a notification. Promoted. It does **not** enter the cut list in §11.

**Cost.** Neither changes the hour budget: both were already inside the Must + Should total of ~284 hours.

---

## D-22 · Where LaTeX validation runs

**Decision.** `LatexValidator` (JLaTeXMath) runs on the **backend**, immediately after N1.5. It is deterministic and uses no AI.

**Reason.** The validation gates were described as "deterministic, local", which read as *on the device*. Two of the three gates are: confidence scoring and the glossary check are pure functions over `LocalCourseContext`. The LaTeX gate is not — the LaTeX string is produced by N1.5, which is already a backend call, so validating it in the same request avoids a round trip and keeps the escalation decision where the data is.

**Clarification that this is not an AI call.** `LatexValidator` lives in the `ai/validation` package because it sits in the AI request path, not because it invokes a model. It appears in the "processes not using AI" list in `Glifo_Alcance.md` §8 and `Glifo_Diseno_Arquitectura.md` §9, and that stays true.

**Not to be confused with rendering.** `JLaTeXMath-Android` on the client *renders* already-validated LaTeX for display. It does not validate.

---

## D-23 · Ordering of sync operations

**Decision.** The outbox enqueues `CREATE_NOTE` before `UPLOAD_PAGE`, and the `SyncWorker` drains them in that order. Both the online and the offline sequence diagrams model the same order.

**Reason.** D-16 established that a `Note` may hold 0 pages while in `DRAFT`. What it did not state was the consequence for the queue: `POST /notes/{id}/pages` needs an `{id}` that exists on the server, so a page operation that reaches the server before its note operation fails with a 404 that no retry can fix — the note will never appear on its own. Ordering is therefore part of the contract, not an implementation detail.

**How the local id is reconciled.** The device creates the note with a local id. When `CREATE_NOTE` succeeds, the worker maps `localId → serverId` in Room before dispatching any page operation for that note. The idempotency key is per operation and is generated at enqueue time, so a retried `CREATE_NOTE` returns 409 and the mapping is read from the conflict response rather than creating a second note.

**Status transitions.** `DRAFT` on creation, `PROCESSING` when the first page lands, `READY` when reconstruction (IA-01) writes `notes.content`. A note in `READY` with zero pages is the invalid state, and it is prevented by the transition, not by a foreign key.

---

## D-24 · Android package structure: Clean layer-first, screaming architecture discarded

**Decision.** The Android package structure drops the feature-first ("screaming architecture") layout described in an earlier version of `Glifo_UML_Modeling.md` §11, specifically the `feature/<capability>/{data,domain,presentation}` duplication. `feature/` is removed; `data/`, `domain/`, and `presentation/` become root packages, each holding `model` / `dto` / `mapper` / `repository` / `viewmodel` etc. for every screen, with no per-capability subdivision. `pipeline/`, `engine/`, `core/`, and `di/` are unaffected — they are cross-cutting engines and infrastructure with no UI screens attached, outside the CRUD-screen structure the professor's rubric addresses. `Glifo_Arquitectura_Estandares.md` §2 reflects the flat version going forward.

**Reason.** Lab 2 scored 80/100, and the professor's stated deduction was structural non-compliance: he grades the Android package layout against his own TaskApp reference material, which is Clean Architecture applied flat — three root packages, no feature grouping. The feature-first layout in §11 is a legitimate pattern (it cites Robert Martin's own screaming-architecture essay and was adopted deliberately, including a recent `feature/teacher/` → `feature/course/` + `feature/admin/` refactor), but the professor never taught the hybrid of screaming-at-the-top with Clean-inside-each-feature, so his rubric treats the two patterns as mutually exclusive. The team chose to match the taught pattern rather than defend the hybrid, given the grading stakes and the imminent Lab 3 defense.

**Scope of the reversal.** This closes over the Android front end only. The backend (`cr.ac.una.glifo`, organized by aggregate per `Glifo_Arquitectura_Estandares.md` §3) is untouched — the professor's objection was specific to the mobile client structure shown in his course material.

**DAO / Room stays out of scope for now, separately from this decision.** Local persistence (Room) is calendared for Module 5 / Applied Research in `Contexto_Curso.md`, not Lab 2 or Lab 3, and "Local storage implementation and data synchronization" is X-Ray's own assigned Applied Research topic. No `data/local/` package or DAO exists in the Android code until that milestone; this is a scheduling fact, not a consequence of the layer-first flattening.

**Future re-evaluation.** Once the app grows past the handful of screens graded in Labs 2–3, the flat structure may become unwieldy (all repositories, all view models, and all domain models sharing one folder regardless of capability). If that becomes a real maintenance cost, the feature-first hybrid documented in the pre-D-24 version of §11 remains the candidate to revisit — but only after the course's structural grading window has closed.

---

## D-25 · Mock API tool for Lab 3

**Decision.** Mockoon serves the Lab 3 mock API. The environment is versioned at
`mockoon/Glifo.json` in the frontend repo, port 3000, prefix `api/v1`.

**Reason.** The professor's own "API de Prueba (Mock API)" lesson lists Mockoon, Beeceptor,
Mocky, JSON Server, and Postman Mock Server as acceptable options. Mockoon was picked
because it runs locally — matching Glifo's local-first, offline-capable principle (D-01,
D-10) — and its response rules can simulate arbitrary status codes and latency
deterministically, which the Lab 3 rubric's error-handling requirement needs without a live
network dependency during the defense.

**Scope.** Only `AuthRepositoryImpl` (`login`/`register`) is wired to the mock — that is
what Lab 3 grades (`Contexto_Curso.md`, "Error handling, Hilt, Retrofit, mock API"). `GlifoApi`
also declares `courses`, `courses/{id}`, `enrollments`, and `notes` routes with matching mock
responses (happy path plus deterministic 400/404/409 triggers), kept ready for whichever
repository is the next one moved off `data/datasource/dummy/` — but none of them are consumed
yet. Every other repository stays dummy-backed under the standing "temporary until there's a
real backend" decision; wiring them to Mockoon would front-run Lab 4/5 backend work for no
grading benefit.

---

## D-26 · Conexión completa de Lab 3 a Mockoon, Captura desde Galería y Selector de Idioma

**Decisión.** Se completa la integración técnica de Laboratorio 3 en el frontend Android de Glifo:
1. **Consumo de Mockoon extendido**: `CourseRepositoryImpl` y `NoteRepositoryImpl` sustituyen a los providers en memoria para los flujos principales de consulta, detalle, creación y matrícula (`POST /enrollments`). Se integró el manejador uniforme de errores `ApiErrorHandler` mapeando excepciones HTTP y de red a `AppError` (`NoConnection`, `Unauthorized`, `Validation`, `Server`), extrayendo el mensaje del JSON del servidor.
2. **Flujo de Matrícula (Enrollments)**: `JoinCourseViewModel` y `JoinCourseScreen` se conectaron de extremo a extremo a `POST /enrollments`. Si el código es inválido o el usuario ya está matriculado, Mockoon responde con 404 o 409 y la UI muestra el banner de error correspondiente sin saltar a éxito incondicional.
3. **Captura desde Galería**: Se implementó el contrato `ActivityResultContracts.GetContent()` en `CaptureScreen`, permitiendo seleccionar imágenes desde el almacenamiento del dispositivo, copiándolas de forma segura a un archivo temporal en `context.cacheDir` y redirigiéndolas al mismo flujo de revisión y procesamiento (`CaptureReviewScreen`).
4. **Selector de Idioma Dinámico**: Se creó `LanguageManager` persistido en `SharedPreferences` con soporte para Sistema, Español e Inglés, aprovechando los catálogos existentes `values/strings.xml` y `values-es/strings.xml`, inyectando `LocalContext` y `LocalConfiguration` en Compose para recomposición inmediata sin reiniciar la aplicación.
5. **Estado de Room / DAO**: Se verificó rigurosamente que ninguna entidad, DAO o base de datos Room está activa ni compila en el frontend. Los archivos de persistencia local permanecen en cuarentena (`.kt.txt` en `_deferred_module5/`) conforme al calendario del Módulo 5 / Lab 4.

---

## D-27 · Database schema: generated from the JPA entities, seeded by `import.sql`

**Decision.** The PostgreSQL schema is created by Hibernate from the JPA entities
(`spring.jpa.hibernate.ddl-auto=create` while the entities change) and seeded by
`src/main/resources/import.sql`. Flyway and `db/migration/V1__baseline.sql` are removed from
the backend. Every rule the baseline script carried is now declared on the entities, so the
generated schema is the complete model:

| Rule | Declared as |
|---|---|
| Enum `CHECK`s (`Glifo_UML_Modeling.md` §5.7) and `payload ->> 'kind' = kind` | `@Table(check = [CheckConstraint(...)])` |
| Composite `UNIQUE`s (§4.3) | `@Table(uniqueConstraints = ...)` |
| Indexes | `@Table(indexes = ...)` |
| `ON DELETE CASCADE / SET NULL` | `@OnDelete` on the child foreign key |
| JSONB columns (7) | `String` + `@JdbcTypeCode(SqlTypes.JSON)` |

The generated DDL is kept at `docs/db/schema.sql` in the backend as a readable reference; it
is never executed. `DatabaseSchemaTest` asserts the 21 tables, the seven JSONB columns, and
that the constraints reject invalid rows. `docker-compose.yml` offers a local PostgreSQL
equivalent to the pgAdmin setup.

**Reason.** The professor's DataLayer lessons (Loom 20–23) build the data layer exactly this
way: role and database created in pgAdmin, entities as the source of the schema, `ddl-auto`
explained as `create` during development and `update` once data must survive, and
`import.sql` for seed rows. Flyway was only ever a proposal (`Glifo_UML_Modeling.md` §3.2,
open item 9) and no course material uses it. As with D-24, the team matches the taught
pattern because it is what the lab is graded against.

**Corrections applied with this decision.**
1. **JSONB columns could not be written.** They were mapped as `String` with
   `columnDefinition = "jsonb"` but without `@JdbcTypeCode`, so PostgreSQL rejected every
   INSERT (`varchar` into `jsonb`). Fixed on all seven columns.
2. **`course_glossary` had no entity**, so with a generated schema the table did not exist.
   Added `CourseGlossary` + `CourseGlossaryRepository`.
3. **Enum values drifted.** The removed baseline script disagreed with §5.7 in five sets
   (`topic_coverage.state`, `glossary_suggestions.status` `ACCEPTED`→`APPROVED`,
   `note_pages.level_reached` without `UNRESOLVED`, `sync_queue.status` `PROCESSING`→`IN_PROGRESS`,
   `notifications.kind`). §5.7 is the source of truth; the entities follow it.
   `ai_calls.level` uses `ProcessingLevel` as §5.7 states (the script had invented
   `BATCH`/`ON_DEMAND`).
4. **Course owners were enrolled with status `OWNER`**, a value outside `EnrollmentStatus`.
   Ownership is `courses.owner_user_id`; `CourseService.createCourse` no longer creates that
   enrollment.
5. `coverage_snapshots.summary` (JSONB) was listed in `Glifo_Arquitectura_Estandares.md` §6.2
   and `Glifo_Diseno_Arquitectura.md` §11 but never existed in the ERD, which has
   `coverage_pct`. Removed from both documents; the JSONB count is **seven**, not eight.

**Cost accepted.** `ddl-auto=create` wipes the data on every start. Before the cloud
deployment (Lab 6) it must move to `update`, which never drops or alters existing
constraints; if the schema still changes after that point, a migration tool comes back as a
new decision.

---

## D-28 · Room activated for the offline pipeline (#27, #28)

**Decision.** Room leaves the `_deferred_module5/` quarantine (D-24, D-26) in Lab 4, but only
with the two tables the offline pipeline needs, in `data/datasource/local/`:

| Table | Issue | Content |
|---|---|---|
| `local_course_context` | #27 | One row per course: glossary (JSON) + `syllabus_version` + `glossary_version` (D-17) |
| `processed_pages` | #28 | Perceptual hash + serialized `PageAnalysis` of every page that went through the pipeline |

- **#27.** Opening the camera calls `GET /courses/{id}/glossary`. If both versions match the
  local row, nothing is rewritten; if either changed, the row is replaced. Without a network
  the local copy is used. The pipeline reads **only Room** when it processes a capture, so it
  behaves the same online and offline. The endpoint exists in the Mockoon mock; the backend
  implements it with the glossary module (Lab 5).
- **#28.** DCT pHash (64 bits, 16 hex characters, same format as `note_pages.perceptual_hash`)
  compared by Hamming distance within the same course. A match reuses the stored analysis and
  skips N0 + N1. The image is downsampled by successive halvings before hashing; a single
  resize to 32×32 aliased the lines of text.
- **Threshold `DUPLICATE_THRESHOLD = 16`**, measured on drawn note pages: re-photographing
  the same page gave 0–14 differing bits, different pages 24–32. The costly error is merging
  two different pages, so the threshold sits closer to the retakes. pHash is not invariant to
  a very different framing: it catches double shots, similar retakes and the same image
  imported twice. Hashing after N0's perspective correction is the candidate improvement.

**Verified on the emulator against Mockoon.** The glossary reached Room when the camera
opened. The same gallery image processed after restarting the app was reused
(0/64 bits). With the server unreachable, the pipeline still used glossary v3 from Room.

**Still deferred (at the time).** Notes, outbox (`SyncOperation`) and the rest of the Room modeling in
`_deferred_module5/`. Done later under D-29; the folder was deleted.

---

## D-29 · Room v2: notes and outbox; screens on real data (#25, #26)

**Decision.** Room grows to v2 with `notes`, `note_pages`, `fragment_corrections` and the `sync_operations` outbox. Room is what the app shows. The server's notes are cached there, and a capture is saved there first, then queued. A real `SyncWorker` (WorkManager, network constraint, exponential backoff) drains the queue in order, `CREATE_NOTE` before `UPLOAD_PAGE` (D-23). The backend's `POST /notes/{id}/pages` stores the result of the pipeline, not the image.

**Why.** #25 and #26 had been closed while Room was still quarantined and the worker was an empty scaffold. Without them, "View results" always opened a fixed example note, and the sync screen showed invented items.

**Rules adopted.**
- The note-reading screens show only real content: the pipeline analysis stored in Room, or the IA-01 reconstruction from the server. When neither exists they say so instead of showing an example.
- Retries are idempotent on pages: a 409 counts as uploaded. `POST /notes` still lacks an idempotency key, which is a known gap.
- The v1 → v2 schema change was destructive because v1 held only caches. From v2 on there is user data, so the next change needs a real `Migration`.
- The active course is the last one opened, kept in SharedPreferences. It is no longer a fixed id.

**Still dummy after this decision** (their backend features are Lab 5/6): coverage, study, teacher syllabus, glossary and gaps, and admin. The frontend PR for #25/#26 lists each one with its issue.

---

## D-30 · Monorepo with full history (2026-11-20)

**Decision.** The backend and Android repositories from the course organization are merged into this repository as `backend/` and `android/` with a subtree merge. All 243 commits are kept. The early prototype modules of this repository were removed; `prototype/` stays because GitHub Pages builds the demo from it.

**Why.** Glifo continues as a personal project, and the course repositories could be deleted at the end of the term. A single repository keeps the history, the docs and the release APKs in one place.

**Rules adopted.**
- Render deploys from `main` with `backend` as its root directory.
- A change that touches both sides ships in order: database migration, backend, APK (`docs/Guia_Despliegue.md` §5.3).

---

## D-31 · Bottom navigation bar instead of the drawer (2026-11-20)

**Decision.** The navigation drawer is replaced by a Material 3 `NavigationBar` with five tabs per role, rendered once by the `GlifoNavHost` scaffold. Student: Home, Notes, Coverage, Study, Profile. Teacher: Home, Courses, Gaps, Glossary, Profile. Admin: Home, Users, Courses, Roles, Profile. Profile takes over Settings, Sync and the logout warning for unsynced captures.

**Why.** The hamburger menu was flagged as an outdated Android pattern in the final review. A bottom bar keeps the main destinations one tap away, and rendering it once removed a drawer copy from fifteen screens.

**Rules adopted.**
- A screen shows the bar only when its route is one of the role's tabs. Any other screen gets a back arrow.
- Tabs scoped to a course use the active course. If there is none, they send the user to Courses with a hint.

---

## Features evaluated and excluded from scope

Record of what is **not** built, with the reason each was rejected, so it does not reappear in later planning.

| Feature | Reason |
|---|---|
| Contradiction detection between notes | High false-positive risk; flagging a contradiction that does not exist destroys trust in the product |
| Semantic comparison via embeddings | Requires an additional model and vector storage; the lexical pre-filter with the glossary plus AI adjudication gives comparable results |
| Automatic subject detection | The natural flow is selecting the course before capturing |
| Bidirectional sync conflict resolution | A project in itself; declared future work |
| Per-course consumption budget with degraded mode | The call ledger is enough to sustain the cost argument |
| Full assessment-driven study planning | Reduced to a manual exam date with priority ordering |
| Streaks, achievements, and social comparison between students | Occupied by other teams; adds multi-user and privacy considerations without differentiation |
| Note sharing via messaging or email | Additional surface unrelated to the project's axis |
| Open-ended questions graded by AI | Requires one call per answer; contradicts the batching policy |
| Full grading rubric with weightings | Another team's territory; does not touch the engine |
| QR enrollment with teacher approval | Adds states and workflow; a course code performs the same function |
| Admin panel with consumption-based suspension | Requires a quota policy and real-time telemetry |
| Weekly email summary | An additional channel to maintain |
| Biometric authentication | Marginal for the scope |
| A second NoSQL database | JSONB covers the document-shaped data without adding a deployment |
| Advanced semantic search, text-to-speech, calendar integration | Out of scope from the start |

---

## Corrections to the course framework

Record of facts that were corrected after verification, so they are not reintroduced. Every row here was wrong in an earlier version of the project documents.

| Fact | Incorrect version | Verified version |
|---|---|---|
| Team size | 2 members; "maximum 2 people" per group | **3 members.** The limit of 2 applies to individual labs and the scientific article, not to project teams |
| Delivery mode | Submission through the virtual classroom | **Live defense in class.** Nothing is uploaded; the virtual classroom only publishes the grade |
| Effective dates | The calendar's closing dates | **The last Wednesday class before the closing date** |
| Lab 2 duration | 3 weeks | **2 effective weeks**, one of them shared with the Lab 1 correction |
| Lab 2 scope | Full implementation | Activities only: menu, authentication, navigation, and lists. AI does not appear until Lab 6 |
| Lab 4 defense | Wed Sep 23 | **Wed Sep 30** |
| Schema tooling | Flyway migrations (`Glifo_UML_Modeling.md` §11.5, earlier revision) | **Hibernate `ddl-auto` + `import.sql`**, as taught in the DataLayer lessons (D-27) |

---

## Lab 1 Result

Grade obtained: **85**. Single deduction on the database UML diagram (1.5 out of 3), with two observations: incorporate roles/privileges tables, and explore the use of JSON. Both resolved via D-03, D-04, and D-09.

The observation-by-observation closure, with the one-line defense for each, is in `Glifo_UML_Modeling.md` §12.2.