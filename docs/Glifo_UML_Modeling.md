# Glifo — UML & Architecture Modeling

**Team X-Ray** — Brandon Brenes · David González · Felipe Ugalde
EIF411 · Mobile Platform Design and Programming · II Term 2026
Technical reference document · version 1.0 · 16 August 2026

> **What this document is.** The complete modeling package for Glifo: entity–relationship
> diagram, UML class diagrams, development stack, API surface, UI/UX map and file layout.
> Every diagram is source code, not a picture, so it can be regenerated and diffed.
>
> **What this document is not.** It does not restate scope (`Glifo_Alcance.md`), coding
> conventions (`Glifo_Arquitectura_Estandares.md`) or decision history
> (`Glifo_Bitacora_Decisiones.md`). It models what those documents describe.
>
> **Language.** This document is written entirely in English — diagrams, notes, prose and
> identifiers. This is a deliberate departure from `Glifo_Arquitectura_Estandares.md` §7.3,
> which currently reads "code in English; comments, documentation and UI text in Spanish".
> See §12.3 for the amendment that has to be applied to that document.

---

## Index

1. [How to read and render this document](#1-how-to-read-and-render-this-document)
2. [Tier separation — the one rule that governs every diagram](#2-tier-separation--the-one-rule-that-governs-every-diagram)
3. [Development stack](#3-development-stack)
4. [Database — entity relationship diagram](#4-database--entity-relationship-diagram)
5. [JSON contracts](#5-json-contracts)
6. [UML class diagrams — frontend](#6-uml-class-diagrams--frontend)
7. [UML class diagrams — backend](#7-uml-class-diagrams--backend)
8. [The API seam — how API calls are notated](#8-the-api-seam--how-api-calls-are-notated)
9. [Sequence diagrams](#9-sequence-diagrams)
10. [UI/UX — screen map and navigation](#10-uiux--screen-map-and-navigation)
11. [Screaming architecture — file layout](#11-screaming-architecture--file-layout)
12. [Traceability and open items](#12-traceability-and-open-items)

---

## 1. How to read and render this document

### 1.1 Rendering

Every diagram is provided as a PNG image in the `docs/images/` directory. If you need to view
or edit the source, each diagram has a link to its corresponding `.puml` file in `docs/uml/`.

To render the source files:
- **IDE:** Use the PlantUML Integration plugin (IntelliJ / Android Studio) or the PlantUML
  extension (VS Code).
- **Web:** Paste the code into `plantuml.com/plantuml`.
- **Batch:** Run `java -jar plantuml.jar docs/uml/*.puml` to export all diagrams to PNG.

### 1.2 Files on disk

Diagram sources are extracted to `docs/uml/`, one file per diagram, named after the
`@startuml` identifier:

```
docs/uml/
├── 01_stack_deployment.puml
├── 02_erd_core.puml
├── 03_erd_operations.puml
├── 04_erd_access_control.puml
├── 05_front_pipeline.puml
├── 06_front_pipeline_contracts.puml
├── 07_front_mvvm_slice.puml
├── 08_front_data_offline.puml
├── 09_back_domain.puml
├── 10_back_web_service_slice.puml
├── 11_back_ai_orchestration.puml
├── 12_back_security.puml
├── 13_component_api_seam.puml
├── 14_seq_capture_to_note.puml
├── 15_seq_offline_sync.puml
├── 16_nav_toplevel.puml
├── 17_nav_capture_flow.puml
└── 18_nav_study_flow.puml
```

### 1.3 Colour legend

Colour carries meaning in every diagram in this document. It is the same four-value scale
throughout, derived from the Glifo day-mode palette so the diagrams and the product look
like the same project.

| Swatch | Hex fill | Hex line | Means |
|---|---|---|---|
| Blue | `#E8EEF6` | `#3E6E9E` | **FRONT** — runs on the Android device |
| Gold | `#FFF3D8` | `#8A6210` | **BACK** — runs on the Spring Boot server |
| Green | `#E4F0EA` | `#2F7D62` | **DB** — persistent storage |
| Terracotta | `#F5EDE9` | `#B94117` | **EXTERNAL** — third-party service, outside our control |

Colour is never the only carrier: every grouped element also sits inside a labelled
`package`/`node` that names its tier. Same accessibility rule as the product itself
(`Glifo_Arquitectura_Estandares.md` §12.3).

---

## 2. Tier separation — the one rule that governs every diagram

The request was to keep front, back and database cleanly separated. That is not a styling
preference; it is a modeling rule with a testable consequence.

> **The rule.** No single diagram may contain classes from two tiers connected by an
> association. If two tiers must appear together, they appear as *components* with an
> interface between them — never as classes with an arrow between them.

**Why.** A class diagram models compile-time structure: what can hold a reference to what.
An Android `ViewModel` can never hold a reference to a `NoteService` running on Render.
Drawing that arrow states something false about the system. It is the single most common
error in student architecture diagrams and it is exactly what a reviewer looks for.

**Consequence — the four-diagram split.** Any interaction that crosses the network is
modeled four times, each time answering a different question:

| Question | Diagram type | Where |
|---|---|---|
| What structures exist on the client? | Class diagram, FRONT only | §6 |
| What structures exist on the server? | Class diagram, BACK only | §7 |
| Where is the seam and who provides what? | Component diagram | §8.2 |
| In what order does it happen? | Sequence diagram | §9 |

The class diagram on the client side **stops at the Retrofit interface**. The class diagram
on the server side **starts at the controller**. Nothing spans the gap. The gap itself is
§8.

---

## 3. Development stack

### 3.1 Deployment view

![Deployment Stack](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/01_stack_deployment.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/01_stack_deployment.puml)

### 3.2 Technology inventory

**Read the Source column first.** Not every line below is a settled decision. Some are fixed
by the course or by the team's existing documents; the rest are proposals made in *this*
document and nothing else. Mixing the two is how a stack acquires dependencies nobody chose.

| Mark | Means |
|:--:|---|
| **●** | **Fixed.** Stated in `Contexto_Curso.md`, `Glifo_Alcance.md`, `Glifo_Arquitectura_Estandares.md` or `Glifo_Diseno_Arquitectura.md`. The reference is given |
| **○** | **Proposed here.** Not in any project document. Needs team ratification before it is treated as decided |

Version numbers are **all** `○` — no project document pins a version. They are current-stable
suggestions, not requirements.

**FRONT — Android**

| Concern | Technology | Source | Where it is fixed | Lab |
|---|---|:--:|---|:--:|
| Language | Kotlin | ● | `Contexto_Curso` §6 | all |
| IDE | Android Studio | ● | `Contexto_Curso` §6 | all |
| UI toolkit | Jetpack Compose | ● | `Contexto_Curso` §6, Lab 2 | 2 |
| Navigation | Navigation Compose | ○ | Navigation is required; the library is not named | 2 |
| Architecture | MVVM + Clean layering | ● | `Estandares` §1.1 | 2 |
| Dependency injection | Hilt | ● | `Contexto_Curso` §6, Lab 3 | 3 |
| HTTP client | Retrofit | ● | `Contexto_Curso` §6, `Diseno` §10 | 3 |
| — OkHttp interceptor | OkHttp | ○ | Implied by `AuthInterceptor` in `Estandares` §2 | 3 |
| Serialization | **Gson or kotlinx.serialization** | ⚠ | **Conflict — see note below** | 3 |
| Local persistence | Room | ● | `Estandares` §2, `Alcance` §6.5 | 4 |
| Background work | WorkManager | ● | `Estandares` §2 (`SyncWorker`) | 4 |
| Camera | CameraX | ● | `Alcance` §6.2 | 2 |
| Image preprocessing | OpenCV Android SDK | ● | `Alcance` §6.2, `Diseno` §6 | 2 |
| Text OCR | ML Kit Text Recognition | ● | `Diseno` §7 | 3 |
| Math rendering | JLaTeXMath-Android | ○ | LaTeX rendering is scoped; the library is not named | 6 |
| Reactive state | Coroutines + StateFlow | ● | `Estandares` §7.1 | 2 |
| Testing | JUnit 5 · MockK · Turbine | ● | `Estandares` §10.2 | 5 |
| Crash + analytics | Crashlytics · Firebase Analytics | ● | `Alcance` §6.6 | 3 / 6 |
| Distribution | Firebase App Distribution | ● | `Contexto_Curso` §6 | final |

> **⚠ Serialization conflict to resolve.** `Contexto_Curso.md` §6 allows either
> ("Retrofit + Gson / kotlinx.serialization"), but `Glifo_Diseno_Arquitectura.md` §10 commits
> to **Gson** — and that is the document shown to the professor. `Glifo_Arquitectura_Estandares.md`
> does not say. Earlier revisions of this document silently wrote *kotlinx.serialization*;
> that was an unrecorded change of an existing decision. **Pick one and record it.** The
> argument for kotlinx.serialization is sealed-class support, which §5.3's three item kinds
> would use directly. The argument for Gson is that `Diseno` already says so and the professor
> has seen it.

**BACK — Spring Boot on Kotlin**

| Concern | Technology | Source | Where it is fixed / why proposed | Lab |
|---|---|:--:|---|:--:|
| Language | **Kotlin** (JVM target 21) | ○ | **Proposed D-13.** No document names a JVM language; `Estandares` §7.2 currently assumes Java | 4 |
| Framework | Spring Boot, monolithic | ● | `Diseno` §10, `Estandares` §1.1 | 4 |
| Compiler plugins | `kotlin-spring` · `kotlin-jpa` | ○ | Mandatory *consequence* of D-13, not an independent choice | 4 |
| Web | Spring Web MVC | ● | `Estandares` §1.1 (Controllers · DTOs) | 5 |
| Persistence | Spring Data JPA + Hibernate | ● | `Estandares` §1.1 (Repositories · Entities · JPA) | 4 |
| JSON | Jackson + `jackson-module-kotlin` | ○ | Spring Boot's default; no document specifies server-side JSON | 4 |
| Security | Spring Security + JWT | ● | `Contexto_Curso` §6, Lab 6 | 6 |
| — JWT library | JJWT | ○ | Library not named anywhere | 6 |
| Validation | Jakarta Bean Validation | ● | `Estandares` §1.1 ("Validación"), §7.2 (`@Valid`) | 5 |
| Mapping | Extension functions | ○ | `Estandares` §5 requires a Mapper *pattern*, not a library. See §7.2 | 5 |
| Boilerplate | none needed | ● | `Estandares` §7.2 uses `@RequiredArgsConstructor` (Lombok). Kotlin removes the need | 4 |
| Migrations | Flyway | ○ | **Not in any document.** Proposed in §11.5 — see the note below | 4 |
| API docs | springdoc-openapi | ○ | `Estandares` §3 lists `OpenApiConfig`, so OpenAPI is intended; the library is not named | 5 |
| LaTeX validation | JLaTeXMath | ● | `Estandares` §3 (`ai/validation/LatexValidator`) | 6 |
| Testing | JUnit 5 · MockMvc | ● | `Estandares` §10.2 | 5 |
| — Mocking | MockK, replacing Mockito | ○ | `Estandares` §10.2 specifies Mockito. Changing it is a consequence of D-13 | 5 |
| API testing | Postman collection | ● | `Contexto_Curso` §6, Lab 5 | 5 |
| Build | Gradle + Kotlin DSL | ○ | **Not in any document.** Kotlin's default and matches the Android module | 4 |

**DB — PostgreSQL**

| Concern | Choice | Source | Rationale |
|---|---|:--:|---|
| Engine | PostgreSQL | ● | Mandatory for all nine teams — `Contexto_Curso` §8.1 |
| Primary keys | `BIGSERIAL` | ● | `Estandares` §6.1 |
| Semi-structured columns | `JSONB`, selectively | ● | `Estandares` §6.2, `Diseno` §11 |
| Enumerations | `VARCHAR` + `CHECK` | ● | `Estandares` §6.1 |
| Indexing on JSONB | GIN, only where a payload field is filtered | ○ | None required in the core scope |
| Schema evolution | Flyway `V<n>__<description>.sql` | ○ | Proposed here |

> **On Flyway — a proposal, with its cost.** No project document mentions migrations, so this
> is an addition, not a clarification. The argument for it: the Lab 1 deduction was about the
> data model, and from Lab 4 onward the model becomes code. A numbered migration file is
> reviewable in a pull request, and the ERD in §4 and `db/migration/` become the same artifact
> expressed twice, the second one executable. The argument against: it is one more tool to
> learn against the zero-slack budget in `Glifo_Alcance.md` §11, and Hibernate's
> `ddl-auto: update` costs nothing to start with. **If the team declines Flyway, §11.5 collapses
> to a single `schema.sql` and nothing else in this document changes.**

---
## 4. Database — entity relationship diagram

### 4.1 Why two levels

The professor's instruction was to reduce the size of the data model — *"very large databases
become hyper-complex to manage"*. The schema in `Glifo_Arquitectura_Estandares.md` §6.3 has
23 tables. Presenting 23 boxes on a projector fails for a reason that has nothing to do with
the design: nobody can read it, so nobody can evaluate it.

The answer is not to delete functionality. It is to separate **the model that carries the
domain** from **the tables that carry operations**.

| Level | Tables | Contains | Defended |
|---|---|---|---|
| **Level 1 — Core** | 15 | Access control, academic domain, ingestion, study | Yes. This is the diagram on screen |
| **Level 2 — Operations** | 5 | Telemetry, sync outbox, devices, notifications, snapshots | Annex. Shown only if asked |

Twenty-three became twenty, and only fifteen are on the main diagram.

### 4.2 What was merged, and why

Four tables disappeared. Every one of them was a **strict 1:1** with its parent — a table
that can only ever have exactly one row per parent row is not an entity, it is a group of
columns that was given its own box.

| Removed | Absorbed into | Cardinality that justifies it |
|---|---|---|
| `syllabi` | `courses` → `syllabus_source_uri`, `syllabus_parsed_at` | A course has exactly one syllabus. §4.3 of the old model already drew it `1 --> 1` |
| `page_processing` | `note_pages` → `level_reached`, `overall_confidence`, `quality_metrics`, `regions`, `processed_at` | Every page has exactly one processing record, created with the page |
| `note_contents` | `notes` → `content`, `content_generated_at` | One reconstruction per note. Regeneration overwrites; there is no history requirement |
| `coverage_snapshots` | → Level 2 annex | Reporting only. Never read by the core flow |

And four tables were **never created**, because the JSON decision in §5 removed the need for
them: `quizzes`, `questions`, `question_options`, `answer_keys`. That is covered in §5.2.

**The honest counter-argument.** Merging a 1:1 into its parent makes the parent row wider and
means a `SELECT * FROM notes` drags a JSONB document it may not need. That is real. It is
acceptable here because `notes` is never scanned in bulk — it is always filtered by
`user_id` and `course_id`, returning tens of rows, not thousands. If note listing ever
becomes a hot path, the fix is a projection query (`SELECT id, title, class_date …`), not
re-splitting the table.

### 4.3 Level 1 — core schema

![Core ERD](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/02_erd_core.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/02_erd_core.puml)

**Reading the cardinality.** `||--o{` is one-to-zero-or-many. `||--|{` is one-to-one-or-many
— a note always has at least one page, which is enforced by the fact that a note is created
*by* capturing a page. `|o--o{` is optional-to-many: `notes.syllabus_topic_id` is nullable
because topic detection can fail and the note still exists.

### 4.4 Level 2 — operations annex

![Operations ERD](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/03_erd_operations.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/03_erd_operations.puml)

**Note on `sync_queue`.** The authoritative outbox lives in **Room, on the device** — that is
the whole point of the applied-research topic. The server-side `sync_queue` table is the
landing record used to enforce idempotency: the client generates `idempotency_key` as a UUID
before the first attempt and resends it on every retry, so a duplicate delivery is detected
by a unique-constraint violation rather than by guesswork. Both sides are modeled: this
table in §4.4, the Room side in §6.4.

**Correction — `coverage_snapshots` must not be load-bearing.** An earlier revision of this
document had the F3 Delta screen read from `coverage_snapshots`. That is a design error against
`Glifo_Alcance.md`: §6.3 marks *"Delta de cobertura entre sesiones"* as **Deseable** while
*"Instantáneas históricas"* is **Opcional**, and §11 lists the snapshots as the **second** item
to drop when slack is needed. Coupling a feature that stays to a table that leaves means the
first schedule cut silently breaks the delta.

The delta is therefore computed from `topic_coverage` alone:

```sql
-- coverage delta since a timestamp, with no snapshot table involved
SELECT state, COUNT(*) FROM topic_coverage
WHERE user_id = :userId AND syllabus_topic_id IN (:topicIds)
  AND updated_at > :since
GROUP BY state;
```

`topic_coverage.updated_at` already exists and is already maintained. `coverage_snapshots`
remains in the annex for the **weekly progress chart** (`Glifo_Alcance.md` §6.3, Opcional),
which genuinely needs a point-in-time series and genuinely disappears if it is cut. Nothing
above Opcional reads that table.

### 4.5 Access control — detail

![Access Control ERD](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/04_erd_access_control.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/04_erd_access_control.puml)

#### What was adapted from the reference structure, and why

| Reference image | Glifo | Reason |
|---|---|---|
| `role`, `privilege`, `user_role`, `role_privilege` (singular) | `roles`, `privileges`, `user_roles`, `role_privileges` (plural) | The professor dictated the plural bridge names, and §6.1 of our standards requires plural throughout. Consistency beats matching the picture |
| `enabled : boolean` | `is_active : BOOLEAN` | §6.1: booleans carry an `is_` / `has_` prefix |
| `create_date : timestamp` | `created_at` + `updated_at : TIMESTAMPTZ` | §6.1 audit convention. `TIMESTAMPTZ` because the app will be demonstrated across devices with different clocks |
| `token_expired : boolean` | **dropped** | JWT is stateless and carries its own `exp` claim. A boolean column that has to be flipped by the server is a session table wearing a disguise, and it would silently make the API stateful |
| `id : uuid` (first image) | `id : BIGSERIAL` | §6.1. The second reference image also uses `bigint`. UUID buys distributed-generation safety we do not need and costs index locality |
| `password : string` | `password_hash : VARCHAR(255)` | The column name should state what is actually in it. BCrypt output is 60 characters; 255 leaves room for an algorithm change |
| `materia (nombre, carta_url)` | `courses (name, syllabus_source_uri)` | Spanish identifiers are out per the all-English rule; `carta_url` becomes the syllabus source URI, which is what it holds |
| `course (user_id, code, name, period)` | `courses (owner_user_id, code, name, term, join_code)` | `owner_user_id` states the relationship instead of implying it; `period` → `term`; `join_code` added because students enroll by code |
| `phone : string` (first image) | **dropped** | Nothing in the scope uses a phone number. A column nobody writes to is a column that will be `NULL` in the demo |

#### Seed data — roles and privileges

The bridge tables are worthless in a defense if they are empty. This is the seed migration
content, and it is what makes the two-role requirement demonstrable.

```sql
-- V2__seed_access_control.sql
INSERT INTO roles (name, description) VALUES
  ('ROLE_STUDENT', 'Captures notes, studies, sees own coverage'),
  ('ROLE_TEACHER', 'Owns courses, publishes syllabus and glossary'),
  ('ROLE_ADMIN',   'Manages users, roles and system state');
```

| Privilege | STUDENT | TEACHER | ADMIN |
|---|:--:|:--:|:--:|
| `NOTE_READ_OWN` | ✓ | ✓ | ✓ |
| `NOTE_WRITE_OWN` | ✓ | ✓ | |
| `NOTE_DELETE_OWN` | ✓ | ✓ | |
| `COURSE_READ` | ✓ | ✓ | ✓ |
| `COURSE_WRITE` | | ✓ | ✓ |
| `SYLLABUS_PUBLISH` | | ✓ | |
| `GLOSSARY_WRITE` | | ✓ | |
| `COVERAGE_READ_OWN` | ✓ | ✓ | |
| `COVERAGE_READ_COURSE` | | ✓ | ✓ |
| `STUDY_ITEM_GENERATE` | ✓ | ✓ | |
| `STUDY_ATTEMPT_WRITE` | ✓ | ✓ | |
| `AI_VISION_INVOKE` | ✓ | ✓ | |
| `USAGE_READ_OWN` | ✓ | ✓ | |
| `USAGE_READ_COURSE` | | ✓ | ✓ |
| `USER_MANAGE` | | | ✓ |
| `ROLE_MANAGE` | | | ✓ |

**Why privileges and not just roles.** Roles are what a user *is*; privileges are what an
endpoint *requires*. Spring Security is annotated against privileges
(`@PreAuthorize("hasAuthority('SYLLABUS_PUBLISH')")`), never against roles. That means
adding a fourth role later — a teaching assistant who can read course coverage but not
publish a syllabus — is a row in `role_privileges`, not a code change. This is the entire
justification for the bridge tables, and it is the answer to give if asked why two tables
were not enough.

---
## 5. JSON contracts

### 5.1 The rule for when a column is JSONB

The professor's instruction was to *evaluate* JSON columns for quizzes, questions, options
and flashcards — not to convert the whole schema. So the decision needs a stated criterion,
applied consistently, and a list of places where it was deliberately **not** applied. That
list is what proves the criterion is real.

> **A column is JSONB when its structure varies between rows of the same table and no query
> filters, joins or aggregates on a field inside it. Otherwise it is relational.**

| Column | JSONB? | Which half of the rule decided it |
|---|:--:|---|
| `study_items.payload` | ✓ | A flashcard and a multiple-choice question have genuinely different shapes |
| `attempts.response` | ✓ | The answer shape follows the item kind |
| `notes.content` | ✓ | A reconstructed note is a document tree of variable depth |
| `note_pages.regions` | ✓ | Region count and fields vary per photograph |
| `note_pages.quality_metrics` | ✓ | Diagnostic bag; new metrics get added during calibration |
| `notifications.payload` | ✓ | Payload varies by notification kind |
| `sync_queue.payload` | ✓ | Holds a serialized operation of any entity type |
| `topic_coverage` | ✗ | `state` and `score` are filtered and aggregated on every coverage screen |
| `syllabus_topics` | ✗ | Self-referencing hierarchy with real cardinality |
| `users` / `roles` / `privileges` | ✗ | Referential integrity is the whole point |
| `ai_calls` | ✗ | Aggregated by type, level and period — that is the argument of the project |
| `enrollments` | ✗ | Join target with a status that gets filtered |

### 5.2 What JSON removed from the schema

This is the "does it simplify?" answer, and it is concrete. A fully normalized quiz model is
the textbook version:

```
quizzes(id, course_id, topic_id, title)
questions(id, quiz_id, kind, stem, order_index)
question_options(id, question_id, label, text, order_index)
answer_keys(id, question_id, correct_option_id, explanation)
flashcards(id, topic_id, front, back)
attempt_answers(id, attempt_id, question_id, selected_option_id)
```

Six tables, five joins to render one quiz, and a polymorphism problem the moment true/false
arrives (a true/false question has no `question_options` rows, so the model has to encode
"absence means boolean" — which is a rule living in application code, not in the schema).

Glifo's version is **two tables**, `study_items` and `attempts`, with a `kind` discriminator
column and a JSONB `payload`. Four tables removed, no joins to render a quiz, and adding a
fourth item kind is a new payload shape plus a new `CHECK` value — no migration of existing
rows.

**The trade accepted.** You cannot write `SELECT * FROM questions WHERE stem ILIKE '%limit%'`
against a normalized column. If full-text search over question text is ever needed, it needs
a GIN index on the payload. Nothing in the scope requires it, and it is one line of DDL if
it ever does.

**Why `kind` is duplicated** — once as a column, once inside the payload. The column exists
so PostgreSQL can filter without opening the document. The payload field exists so
`kotlinx.serialization` can pick the right sealed subclass without the client having to
trust an out-of-band value. They are asserted equal by a `CHECK` constraint:

```sql
ALTER TABLE study_items ADD CONSTRAINT study_items_kind_matches_payload
  CHECK (payload ->> 'kind' = kind);
```

### 5.3 `study_items.payload` — the three item kinds

Every payload carries `schemaVersion`. A JSONB column with no version field is a migration
you cannot write later.

**FLASHCARD**

```json
{
  "schemaVersion": 1,
  "kind": "FLASHCARD",
  "front": "State the Bolzano–Weierstrass theorem.",
  "back": "Every bounded sequence in R^n has a convergent subsequence.",
  "latex": "\\forall (x_n)\\ \\text{bounded}\\ \\exists (x_{n_k})\\ \\text{convergent}",
  "hint": "Think about compactness.",
  "sourceNoteIds": [412, 415],
  "difficulty": "MEDIUM"
}
```

**MULTIPLE_CHOICE**

```json
{
  "schemaVersion": 1,
  "kind": "MULTIPLE_CHOICE",
  "stem": "Which condition guarantees that a continuous function attains a maximum on a set S?",
  "options": [
    { "id": "a", "text": "S is open and bounded",   "isCorrect": false },
    { "id": "b", "text": "S is closed and bounded", "isCorrect": true  },
    { "id": "c", "text": "S is connected",          "isCorrect": false },
    { "id": "d", "text": "S is countable",          "isCorrect": false }
  ],
  "explanation": "Extreme value theorem: continuity on a compact set. In R^n, compact = closed and bounded.",
  "shuffleOptions": true,
  "sourceNoteIds": [412],
  "difficulty": "MEDIUM"
}
```

**TRUE_FALSE**

```json
{
  "schemaVersion": 1,
  "kind": "TRUE_FALSE",
  "statement": "Every convergent sequence in R is bounded.",
  "answer": true,
  "explanation": "Convergence implies all but finitely many terms lie in a neighbourhood of the limit; the finite remainder is bounded.",
  "sourceNoteIds": [415],
  "difficulty": "EASY"
}
```

**The answer key ships to the device — deliberately.** `isCorrect` is inside the payload the
client downloads. That is required by the scope: grading is local, deterministic and works
offline (`Glifo_Alcance.md` §6.4). It is acceptable because study items are practice
material, not graded assessments — there is no mark at stake and nothing to cheat. If Glifo
ever issued a real grade, the key would have to move behind a server-side grading endpoint,
and offline grading would have to be dropped. Stating that trade explicitly is the point;
discovering it during questions is not.

### 5.4 `attempts.response`

```json
{ "schemaVersion": 1, "kind": "MULTIPLE_CHOICE", "selectedOptionId": "b", "elapsedMs": 8420 }
```
```json
{ "schemaVersion": 1, "kind": "TRUE_FALSE", "selectedAnswer": true, "elapsedMs": 3110 }
```
```json
{ "schemaVersion": 1, "kind": "FLASHCARD", "selfRating": "GOOD", "elapsedMs": 12050 }
```

`selfRating` is one of `AGAIN` · `HARD` · `GOOD` · `EASY` and feeds the spaced-repetition
interval directly. For flashcards `attempts.is_correct` is derived as
`selfRating IN ('GOOD','EASY')`, so a single boolean column keeps working across all three
kinds and coverage math never has to branch on item type.

### 5.5 `notes.content` — the IA-01 reconstruction

This is the document that the confidence map renders. Its structure is the reason the
confidence map is possible at all: **confidence is attached per span, not per note**.

```json
{
  "schemaVersion": 1,
  "blocks": [
    { "id": "b1", "type": "HEADING", "level": 2, "text": "Compactness in R^n" },
    {
      "id": "b2",
      "type": "PARAGRAPH",
      "spans": [
        { "text": "A set is compact when it is ", "confidence": 0.96, "resolvedAt": "N1", "regionId": "r3" },
        { "text": "closed and bounded",            "confidence": 0.71, "resolvedAt": "N2", "regionId": "r4" }
      ]
    },
    {
      "id": "b3",
      "type": "FORMULA",
      "latex": "\\lim_{n\\to\\infty} x_{n_k} = x \\in S",
      "confidence": 0.83,
      "resolvedAt": "N1_5",
      "regionId": "r7",
      "cropUri": "s3://glifo/pages/8891/r7.png",
      "latexCompiles": true
    },
    {
      "id": "b4",
      "type": "FIGURE",
      "caption": "Open cover of the interval",
      "resolvedAt": "N2",
      "regionId": "r9",
      "cropUri": "s3://glifo/pages/8891/r9.png"
    },
    {
      "id": "b5",
      "type": "PARAGRAPH",
      "spans": [
        { "text": "[unreadable]", "confidence": 0.22, "resolvedAt": "UNRESOLVED", "regionId": "r11", "isUncertain": true }
      ]
    }
  ],
  "summary": { "verified": 14, "repaired": 2, "escalated": 1, "uncertain": 1 }
}
```

`summary` is denormalized on purpose — it is the header line of every note card
(`14 verified · 2 repaired · 1 escalated · 1 uncertain`) and recomputing it by walking the
block tree on every list render would be wasteful. It is written once, by the same service
that writes `blocks`.

### 5.6 `note_pages.regions` and `note_pages.quality_metrics`

```json
[
  { "id": "r3",  "bbox": [0.08, 0.12, 0.92, 0.19], "kind": "TEXT",    "resolvedAt": "N1",    "confidence": 0.96, "isUncertain": false },
  { "id": "r7",  "bbox": [0.14, 0.34, 0.63, 0.43], "kind": "MATH",    "resolvedAt": "N1_5",  "confidence": 0.83, "isUncertain": false, "engine": "SimpleTex" },
  { "id": "r9",  "bbox": [0.10, 0.51, 0.70, 0.78], "kind": "DRAWING", "resolvedAt": "N2",    "confidence": 0.65, "isUncertain": false },
  { "id": "r11", "bbox": [0.12, 0.82, 0.88, 0.90], "kind": "TEXT",    "resolvedAt": "UNRESOLVED", "confidence": 0.22, "isUncertain": true, "reason": "GLARE" }
]
```

`bbox` is `[x0, y0, x1, y1]` normalized to `0.0–1.0` so the overlay survives any rendering
resolution and any later change of stored image size.

```json
{
  "blurVariance": 142.7,
  "brightness": 0.61,
  "glareRatio": 0.03,
  "skewAngleDeg": 1.8,
  "isUsable": true,
  "rejectionReason": null
}
```

`rejectionReason` is one of `TOO_BLURRY` · `TOO_DARK` · `GLARE` · `SKEW_EXCESSIVE` ·
`NO_TEXT_DETECTED`. It exists because the scope commits to rejecting a capture *with a
concrete reason* rather than a generic failure message (`Glifo_Alcance.md` §6.2). A
`NULL`-able string is not enough — it is a closed set, and the UI maps each value to a
specific instruction to the student.

### 5.7 Enumerations — the closed sets

Every one of these is a `VARCHAR` with a `CHECK` constraint in PostgreSQL, a Kotlin `enum
class` on the client, and a Java `enum` persisted with `@Enumerated(EnumType.STRING)` on the
server. Three declarations of the same set is a duplication risk, so the values live in one
place in the repository — `db/migration/V1__baseline.sql` — and everything else must match
it.

| Enumeration | Values | Used in |
|---|---|---|
| `ProcessingLevel` | `N0` · `N1` · `N1_5` · `N2` · `N3` · `UNRESOLVED` | `note_pages.level_reached`, `ai_calls.level`, region payloads |
| `RegionKind` | `TEXT` · `MATH` · `DRAWING` | region payloads |
| `CoverageState` | `SOLID` · `PARTIAL` · `ABSENT` · `UNCERTAIN` | `topic_coverage.state` |
| `ItemKind` | `FLASHCARD` · `MULTIPLE_CHOICE` · `TRUE_FALSE` | `study_items.kind` |
| `SyncStatus` | `PENDING` · `IN_PROGRESS` · `FAILED` · `DONE` | `sync_queue.status` |
| `EnrollmentStatus` | `ACTIVE` · `PENDING` · `REMOVED` | `enrollments.status` |
| `GlossaryKind` | `TERM` · `SYMBOL` · `NOTATION` | `course_glossary.kind` |
| `AiCallType` | `OCR_M` · `IA_00` · `IA_01` · `IA_02` · `IA_03` · `IA_05` | `ai_calls.call_type` |
| `NotificationKind` | `SYNC_COMPLETE` · `REVIEW_DUE` · `COVERAGE_DROP` | `notifications.kind` |
| `RejectionReason` | `TOO_BLURRY` · `TOO_DARK` · `GLARE` · `SKEW_EXCESSIVE` · `NO_TEXT_DETECTED` | `quality_metrics.rejectionReason` |

---
## 6. UML class diagrams — frontend

All four diagrams in this section contain **only** classes that compile into the APK. No
server class appears. The outermost class on the network side is a Retrofit interface, and
it is where the diagram stops.

### 6.1 Pipeline engine — the differentiator

![Pipeline Engine](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/05_front_pipeline.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/05_front_pipeline.puml)

**The design point in that note.** `MathOcrRequestStep` and `VisionRepairRequestStep` are the
two steps that need the server, and they were the temptation to draw an arrow from the
pipeline to the backend. They do not call it. They return a `RemoteWorkRequest` describing
what they need, and the repository decides whether to dispatch it now, queue it, or drop it
because the device is offline. That is what makes `EscalationPolicy` a pure unit test with
no emulator and no network — which the standards document lists as a high-priority test
(§10.1).

### 6.2 Pipeline data contracts

![Pipeline Contracts](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/06_front_pipeline_contracts.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/06_front_pipeline_contracts.puml)

**`RegionResult.finalText` and `latex` are both nullable, and never both null.** A drawing
region resolved at N2 has a caption but no LaTeX; a formula has LaTeX but no plain text.
This is the one place where the model tolerates a nullable pair instead of a sealed
hierarchy, because the JSONB serialization on the other side is flat. It is a conscious
concession and it is worth naming before someone else names it.

### 6.3 MVVM slice — the canonical screen

![MVVM Slice](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/07_front_mvvm_slice.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/07_front_mvvm_slice.puml)

### 6.4 Data layer and the offline outbox

![Offline Data Outbox](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/08_front_data_offline.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/08_front_data_offline.puml)

**Room mirrors Postgres, it does not duplicate it.** The Room schema holds only what the
device needs to work offline: `notes`, `note_pages`, `study_items`, `topic_coverage`,
`review_schedule`, plus `sync_operations`. It does **not** hold `users`, `roles`,
`privileges`, `ai_calls` or `enrollments` — an offline device has no reason to know the
privilege table, and caching it would be a security surface with no benefit. The authority
for identity is the JWT in `EncryptedSharedPreferences`, and it expires.

---
## 7. UML class diagrams — backend

Same rule in the other direction: nothing in this section runs on the phone. The outermost
class on the network side is a `@RestController`, and that is where the diagram starts.

**The backend is Kotlin**, so these diagrams use Kotlin type syntax — `Int` not `Integer`,
`Unit` not `void`, `Note?` not `Optional<Note>`, and `?` marking every nullable property.
§7.5 accounts for that decision in full.

### 7.1 Domain entities — the JPA mirror of the core ERD

![Backend Domain Entities](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/09_back_domain.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/09_back_domain.puml)

**Nullability is new information in this diagram.** Under Java every field was implicitly
nullable and the diagram could not say which ones actually were. Under Kotlin, `String?`
versus `String` is a compiler-enforced statement that maps one-to-one onto
`@Column(nullable = true/false)` and onto the `NOT NULL` markers in §4.3. The three documents
— ERD, entity diagram, migration file — can now be checked against each other mechanically.

**Composition vs association is not decoration here.** `Course *-- SyllabusTopic` is a filled
diamond: deleting a course deletes its topics, mapped as `cascade = ALL, orphanRemoval =
true`. `User --> Note` is a plain association: deleting a user must **not** silently delete
their notes; that path is blocked by a foreign key with `ON DELETE RESTRICT` and requires an
explicit administrative action. The diamonds in this diagram are the cascade policy, drawn.

### 7.2 Web / service / persistence slice — the canonical resource

![Web Service Slice](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/10_back_web_service_slice.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/10_back_web_service_slice.puml)

**Why the mapper is a file of functions and not an interface.** `Glifo_Arquitectura_Estandares.md`
§5 requires the **Adapter/Mapper pattern** and §7.2 requires the **name** `<Recurso>Mapper`. It
never requires a mapping library, and no project document names one. So the only question is
what shape the pattern takes in Kotlin, and the answer is
`fun Note.toResponse() = NoteResponse(id = id!!, title = title, …)` — ordinary code the IDE
navigates into, the debugger steps through, and a reviewer reads in a pull request.

*(Correction to an earlier revision of this document, which claimed MapStruct was being
removed. MapStruct was never part of Glifo's stack — it was introduced by this document and
then argued away. It is mentioned here only because it is the reflexive choice in a Java
Spring project, and the point worth keeping is that Kotlin's named and default arguments make
the reflex unnecessary.)*

### 7.3 AI orchestration

![AI Orchestration](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/11_back_ai_orchestration.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/11_back_ai_orchestration.puml)

**Why the external boundary is a separate coloured package.** `SimpleTexClient` and
`LlmClient` are interfaces we declare but do not implement — Spring generates the proxy. Put
differently: everything inside the gold package is testable with a mock, and everything in
the terracotta package is a place where the demo can fail for reasons outside the team's
control. Drawing that line is what makes the fallback strategy legible: `MathOcrEngine` has
two implementations precisely because the terracotta box on the left has a daily quota.

### 7.4 Security

![Backend Security](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/12_back_security.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/12_back_security.puml)

### 7.5 Kotlin on the backend — what changes

The decision is that the backend is Kotlin, not Java. Nothing in the course requires Java —
`Contexto_Curso.md` §6 lists "Spring Boot (implicit from the use of Spring Security)" and
never names a JVM language. This section accounts for what that costs and what it buys, so
the answer exists if it is asked during a defense.

#### What it removes

Only two, and both are traceable to a specific line in the team's own documents. Anything
else would be a dependency this document invented in order to remove.

| Dependency | Where it exists today | Why it disappears |
|---|---|---|
| **Lombok** | `Glifo_Arquitectura_Estandares.md` §7.2 uses `@RequiredArgsConstructor` in its canonical controller | `@Getter`, `@Setter`, `@RequiredArgsConstructor` and `@Builder` are all language features in Kotlin. Lombok also mixes badly with it — both are annotation processors and the ordering between them is a known source of build failures |
| **Mockito** | `Glifo_Arquitectura_Estandares.md` §10.2 specifies it for the backend | MockK replaces it, so the team maintains **one** mocking library across both tiers instead of two. Mockito cannot stub a `final` class, and every Kotlin class is final unless opened — keeping it would mean fighting the language on every test |

The `Optional<T>` wrapper also disappears from repository signatures, replaced by `Note?`,
which the compiler enforces rather than merely documenting.

**Not counted as a removal:** MapStruct and `kapt`. Neither appears in any project document —
the Mapper *pattern* is required (§5) but no library is. An earlier revision of this document
listed MapStruct as removed; that was double-counting a dependency this document had itself
introduced.

#### What it adds

| Addition | Why it is mandatory, not optional |
|---|---|
| `kotlin-spring` compiler plugin | Kotlin classes are `final` by default; Spring needs to subclass `@Configuration`, `@Service`, `@Transactional` beans to proxy them. The plugin opens exactly those |
| `kotlin-jpa` compiler plugin | Hibernate requires a no-argument constructor on every `@Entity`. The plugin synthesizes one |
| `jackson-module-kotlin` | Without it Jackson cannot construct a `data class` that has no default constructor, and every request DTO fails to deserialize |

**This is the single thing most likely to cost the team a day in Lab 4.** Omit any of the
three and the application compiles cleanly and then fails at runtime with a message that
does not mention Kotlin. Add them to `build.gradle.kts` on day one:

```kotlin
plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.spring") version "2.0.20"   // all-open
    kotlin("plugin.jpa") version "2.0.20"      // no-arg
    id("org.springframework.boot") version "3.3.4"
}
```

#### The entity trap

An `@Entity` must **not** be a `data class`. This is the most common Kotlin/JPA mistake and
it is worth stating in the standards document, because it fails silently rather than loudly:

- `data class` generates `equals`/`hashCode` over **all** properties. Hibernate's lazy
  proxies do not have those properties loaded, so an entity's identity changes depending on
  whether it was fetched lazily — which breaks `Set<Note>` membership and second-level
  caching in ways that only appear under load.
- `data class` generates `toString()` over all properties, so logging an entity triggers
  lazy loading of every association, sometimes outside a transaction.
- `copy()` on a persisted entity produces a second object with the same `id`, which is a bug
  with no compiler warning.

The rule: **`data class` for DTOs, plain `class` with `var` for entities**, and `equals` /
`hashCode` written by hand over `id` only. DTOs are exactly where `data class` shines — the
`<<data class>>` stereotypes in §7.2 mark them.

#### The nullability trap

Kotlin's non-null types give false confidence against a lazy association. `val course: Course`
on an entity is non-null to the compiler, but if it is a `LAZY` proxy accessed outside a
transaction, Hibernate throws `LazyInitializationException` — the compiler cannot see it.
Null-safety is a compile-time property; lazy loading is a runtime one. They do not compose,
and the fix is unchanged from Java: fetch what you need inside the service, in the
transaction.

#### Shared DTOs between Android and the backend — declined

Kotlin on both tiers makes it technically possible to define `NoteResponse` once and use it
in the APK and the JAR. It is declined for this project:

- It requires a shared Gradle module and either a composite build or a published artifact,
  which is a build-system project on top of the actual project.
- It couples the client's release cycle to the server's. The whole point of `ApiEnvelope` and
  `ErrorMapper` (§8.3) is that the client can tolerate a server that changed.
- It would put a compile-time dependency across the seam that §2 exists to prevent — the
  seam would stop being a contract and become a shared type.

Stated here so that "why didn't you share the DTOs, they're both Kotlin?" has an answer
rather than a pause.

#### Conventions delta

`Glifo_Arquitectura_Estandares.md` §7.2 is written for Java and has to be replaced. The
naming table (`<Recurso>Controller`, `<Acción>Request`, `<Recurso>Response`) survives
unchanged; the code rules do not:

| §7.2 rule (Java) | Kotlin replacement |
|---|---|
| Constructor injection, never `@Autowired` on a field | Unchanged — but now it is the *only* option, since `val` properties must be initialized |
| `@RequiredArgsConstructor` from Lombok | Primary constructor. Lombok is gone |
| A JPA entity never crosses the HTTP boundary | Unchanged, and now enforced by type: DTOs are `data class`, entities are not |
| Getters and setters | `val` / `var` properties |
| `Optional<T>` from repositories | `T?` |
| Checked exceptions | Kotlin has none; the `GlobalExceptionHandler` contract is unaffected |

The canonical controller, replacing the Java sample in §7.2 of that document:

```kotlin
@RestController
@RequestMapping("/api/v1/notes")
class NoteController(
    private val noteService: NoteService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateNoteRequest): ResponseEntity<NoteResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(noteService.create(request))
}
```

#### What does not change

The architecture. Every diagram in §7 has the same classes, the same packages, the same
dependency directions and the same patterns as it did under Java — only the type syntax
moved. That is the useful observation: the language was never load-bearing. The tier
separation in §2, the aggregate-per-package layout in §11.4 and the API seam in §8 are
unaffected, which is what a clean architecture is supposed to give you.

---

## 8. The API seam — how API calls are notated

This section answers the question directly: *where do the API calls go in these maps?*

### 8.1 The four rules

**Rule 1 — A class diagram never draws the network.**
No arrow crosses from a client class to a server class. §6 and §7 obey this without
exception. If you find yourself drawing that arrow, what you actually want is a component
diagram or a sequence diagram.

**Rule 2 — The HTTP contract lives on the Retrofit interface, as annotations.**
The verb and path are properties of the method, so they belong in the method signature, not
on an arrow. Written out in full, a Retrofit interface is a complete, readable API
specification:

```kotlin
interface NoteApi {
    @GET("api/v1/notes")
    suspend fun list(
        @Query("courseId") courseId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): ApiEnvelope<List<NoteResponse>>

    @GET("api/v1/notes/{id}")
    suspend fun getById(@Path("id") id: Long): ApiEnvelope<NoteResponse>

    @POST("api/v1/notes")
    suspend fun create(@Body body: CreateNoteRequest): ApiEnvelope<NoteResponse>

    @Multipart
    @POST("api/v1/notes/{id}/pages")
    suspend fun uploadPage(
        @Path("id") noteId: Long,
        @Part image: MultipartBody.Part,
        @Part("clientResult") clientResult: RequestBody,
        @Header("Idempotency-Key") key: String,
    ): ApiEnvelope<PageResponse>

    @PATCH("api/v1/notes/{id}/fragments/{spanId}")
    suspend fun correctFragment(
        @Path("id") noteId: Long,
        @Path("spanId") spanId: String,
        @Body body: CorrectionRequest,
    ): ApiEnvelope<NoteResponse>
}
```

In the UML diagram this same interface appears with the stereotype `<<Retrofit>>` and, where
space allows, the verb and path as a note. In a compact diagram the stereotype alone is
enough — the reader knows the file to open.

**Rule 3 — The seam itself is a component diagram with ball-and-socket.**
One diagram, §8.2, for the whole system. Not one per endpoint.

**Rule 4 — Ordering is a sequence diagram; inventory is a table.**
Never try to make one diagram do both. §9 has the sequences, §8.3 has the inventory.

### 8.2 Component diagram — the seam

![API Seam](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/13_component_api_seam.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/13_component_api_seam.puml)

### 8.3 Endpoint inventory

`Privilege` is what `@PreAuthorize` enforces. `Idem` marks endpoints that accept an
`Idempotency-Key` header — every endpoint the offline outbox can replay.

**`Prio` is not decoration.** `Glifo_Alcance.md` §11 budgets ~284 hours of work against ~285
hours of team capacity and states plainly *"Queda sin margen."* A flat list of thirty endpoints
hides that. Every row below carries the priority its feature has in `Glifo_Alcance.md` §6, and
the five features §11 names as the first to drop are numbered in cut order.

| Mark | Tier | Means |
|:--:|---|---|
| **I** | must-have | *Imprescindible* — in the minimum product, `Glifo_Alcance.md` §12 |
| **D** | should-have | *Deseable* |
| **O** | could-have | *Opcional* |
| **①–⑤** | — | Position on the cut list in `Glifo_Alcance.md` §11, applied in order to recover slack |

*(The letters are the initials of the Spanish tier names used in `Glifo_Alcance.md` §6. They
are kept rather than translated so a row here can be matched to a row there without a lookup
table — the same reason the document keeps `Glifo_*` filenames in Spanish.)*

**Auth** — `/api/v1/auth`

| Verb | Path | Privilege | Prio | Idem | Lab |
|---|---|---|:--:|:--:|:--:|
| POST | `/auth/register` | public | **I** | | 6 |
| POST | `/auth/login` | public | **I** | | 6 |
| POST | `/auth/refresh` | public (valid refresh token) | **I** | | 6 |
| GET | `/users/me` | authenticated | **I** | | 6 |

**Courses** — `/api/v1/courses`

| Verb | Path | Privilege | Prio | Idem | Lab |
|---|---|---|:--:|:--:|:--:|
| GET | `/courses` | `COURSE_READ` | **I** | | 5 |
| POST | `/courses` | `COURSE_WRITE` | **I** | ✓ | 5 |
| GET | `/courses/{id}` | `COURSE_READ` | **I** | | 5 |
| POST | `/courses/{id}/syllabus` | `SYLLABUS_PUBLISH` | **I** | | 5 |
| GET | `/courses/{id}/topics` | `COURSE_READ` | **I** | | 5 |
| POST | `/courses/{id}/topics` *(manual syllabus fallback)* | `SYLLABUS_PUBLISH` | D ⑤ | ✓ | 5 |
| GET | `/courses/{id}/glossary` | `COURSE_READ` | D | | 5 |
| POST | `/courses/{id}/glossary` | `GLOSSARY_WRITE` | D | ✓ | 5 |
| GET | `/courses/{id}/gaps` | `COVERAGE_READ_COURSE` | D | | 6 |
| POST | `/enrollments` | `COURSE_READ` | **I** | ✓ | 5 |

**Notes and ingestion** — `/api/v1/notes`

| Verb | Path | Privilege | Prio | Idem | Lab |
|---|---|---|:--:|:--:|:--:|
| GET | `/notes` | `NOTE_READ_OWN` | **I** | | 4 |
| POST | `/notes` | `NOTE_WRITE_OWN` | **I** | ✓ | 4 |
| GET | `/notes/{id}` | `NOTE_READ_OWN` | **I** | | 4 |
| DELETE | `/notes/{id}` | `NOTE_DELETE_OWN` | **I** | | 4 |
| POST | `/notes/{id}/pages` | `NOTE_WRITE_OWN` | **I** | ✓ | 4 |
| POST | `/notes/{id}/pages/{pageId}/repair` *(N2)* | `AI_VISION_INVOKE` | **I** | ✓ | 6 |
| POST | `/notes/{id}/pages/{pageId}/full-vision` *(N3)* | `AI_VISION_INVOKE` | D ① | ✓ | 6 |
| POST | `/notes/{id}/reconstruct` *(IA-01)* | `NOTE_WRITE_OWN` | **I** | ✓ | 6 |
| PATCH | `/notes/{id}/fragments/{spanId}` | `NOTE_WRITE_OWN` | **I** | ✓ | 6 |

**Coverage and study**

| Verb | Path | Privilege | Prio | Idem | Lab |
|---|---|---|:--:|:--:|:--:|
| GET | `/courses/{id}/coverage` | `COVERAGE_READ_OWN` | **I** | | 5 |
| POST | `/courses/{id}/coverage/evaluate` *(IA-05)* | `COVERAGE_READ_OWN` | **I** | ✓ | 6 |
| GET | `/courses/{id}/coverage/delta?since=` | `COVERAGE_READ_OWN` | D | | 5 |
| POST | `/courses/{id}/study-items/generate` *(IA-02)* | `STUDY_ITEM_GENERATE` | **I** | ✓ | 6 |
| GET | `/courses/{id}/study-items?topicId=` | `COURSE_READ` | **I** | | 6 |
| GET | `/study-items/{id}/explanation` *(IA-03)* | `COURSE_READ` | O | | 6 |
| POST | `/attempts` | `STUDY_ATTEMPT_WRITE` | **I** | ✓ | 6 |
| GET | `/review-schedule?dueBefore=` | `STUDY_ATTEMPT_WRITE` | **I** | | 6 |

**Operations and administration**

| Verb | Path | Privilege | Prio | Idem | Lab |
|---|---|---|:--:|:--:|:--:|
| GET | `/courses/{id}/ai-usage` | `USAGE_READ_OWN` | ⚠ | | 6 |
| POST | `/devices` | authenticated | **I** | ✓ | 6 |
| GET | `/admin/users` | `USER_MANAGE` | D | | 6 |
| PATCH | `/admin/users/{id}/roles` | `ROLE_MANAGE` | D | | 6 |

> **⚠ Contradiction in the source documents, for the team to resolve.**
> `Glifo_Alcance.md` §6.6 marks *"Vista de consumo para estudiante y docente"* as **Opcional**,
> but §12 lists as minimum-product item 9: *"`ai_calls` visible: llamadas, nivel y ahorro."*
> The **recording** of `ai_calls` is Imprescindible in §6.6 and is not in dispute; the
> **screen that displays it** is Opcional in one section and mandatory in another.
>
> This one matters more than its size suggests. The defense script in `Glifo_Alcance.md` §14
> spends minute 4:15 on *"cinco páginas, tres motores gratuitos, dos llamadas al modelo de
> pago"* — which is this screen. If H2 is genuinely optional it should come out of the
> defense script; if it stays in the script it is not optional. **Recommendation: promote it
> to Imprescindible in §6.6**, since the cost argument is the project's thesis and an argument
> with no screen behind it is an assertion.

**Two endpoints that only exist at Deseable or below.** `full-vision` (N3) is first on the cut
list, and `explanation` (IA-03) is Opcional. Both are in this inventory so the API surface is
complete, but neither belongs in a Lab 6 commitment. If the schedule tightens, `full-vision`
is the first thing to go and the pipeline still runs N0→N1→N1.5→N2 unchanged — which is
exactly why §11 lists it first: it is the only escalation level whose removal costs nothing
architecturally.

**Envelope.** Every response uses the shape already fixed in
`Glifo_Arquitectura_Estandares.md` §8.3 — `{ "data": …, "meta": … }` on success,
`{ "error": { code, message, details, timestamp, path } }` on failure. `ApiEnvelope<T>` in
the Retrofit signatures above is the client-side type for it, so the envelope is unwrapped
in exactly one place (`ErrorMapper`) rather than in every repository.

---
## 9. Sequence diagrams

Two flows are worth the space: the one the whole project rests on, and the one that is being
defended as applied research.

### 9.1 Capture → reconstructed note

![Capture Flow Sequence](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/14_seq_capture_to_note.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/14_seq_capture_to_note.puml)

**What this diagram is designed to prove during the defense.** Read the `EXTERNAL` box: for a
good photograph it is never touched. For a photograph with one handwritten formula, only
SimpleTex is touched, once per formula. The paid vision model is reached only inside the
`opt` block. That is the cost argument of the project, and it is visible in the diagram
rather than asserted in prose.

### 9.2 Offline capture and deferred sync

![Offline Sync Sequence](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/15_seq_offline_sync.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/15_seq_offline_sync.puml)

---

## 10. UI/UX — screen map and navigation

### 10.1 Inventory

41 screen identifiers across 39 distinct frames — `B1a` / `B1b` / `B1c` are the same
navigation drawer rendered with a different section list per role, which is why they share a
composable. Every screen already exists in the Figma prototype.

| Zone | IDs | Screens |
|---|---|---|
| **A · Access** | A1–A4 | Splash · Login · Register · Recover password |
| **B · Shell** | B1a/b/c, B2–B4 | Navigation drawer (×3 roles) · Student home · Teacher home · Admin home |
| **C · Courses** | C1–C3 | My courses · Join course · Course/role switcher · Course hub |
| **D · Capture** | D1–D3 | Camera · Rejection & diagnostics · Processing |
| **E · Notes** | E1–E5 | Note list · Confidence map · Correct fragment · Formula viewer · Provenance |
| **F · Coverage** | F1–F3 | Coverage · Topic detail · Delta |
| **G · Study** | G1–G4 | Study hub · Flashcards · Quiz · Result |
| **H · Operations** | H1–H2 | Sync queue · AI usage |
| **I · Teacher** | I1–I5 | Teacher courses · New course · Publish syllabus · Glossary · Group gaps |
| **J · Admin** | J1–J3 | Users · User detail · Roles |
| **K · Account** | K1–K2 | Settings · Profile |

### 10.2 Top-level navigation

![Top Level Navigation](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/16_nav_toplevel.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/16_nav_toplevel.puml)

### 10.3 Capture flow

![Capture Flow](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/17_nav_capture_flow.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/17_nav_capture_flow.puml)

### 10.4 Study flow

![Study Flow](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/images/18_nav_study_flow.png)
[Source Code (PlantUML)](file:///C:/Users/David/StudioProjects/Proyecto-Plataformas-Moviles/docs/uml/18_nav_study_flow.puml)

### 10.5 UI rules that the diagrams encode

These are already product commitments in `Glifo_Arquitectura_Estandares.md` §12.5; listed
here because each one is visible as a transition above.

| Rule | Where it shows up |
|---|---|
| A note is never displayed without its confidence indicator | `D3 → E2` goes to the confidence map, not to a plain reader |
| The original crop is always available beside a transcribed formula | `E2 → E4` exists as a first-class screen |
| A rejected capture states the concrete reason | `D2` is a screen, not a toast |
| The pipeline level that resolved a region is inspectable | `E5` exists |

---

## 11. Screaming architecture — file layout

### 11.1 What the test is

Robert Martin's point is that the top level of a repository should announce **what the
software does**, not what framework it was written with. The test is mechanical: show
someone the top-level folder listing with no other context and ask what the application is
about.

- `controllers/ services/ models/ utils/` screams *"a web framework was used here."*
- `note/ course/ study/ pipeline/ user/` screams *"this thing reads and studies notes."*

Both structures compile. Only one of them tells a new team member where to put a file.

### 11.2 Repository root

```
glifo/
├── android/                     FRONT   Android client
├── backend/                     BACK    Spring Boot service
├── database/                    DB      migrations + seeds, single source of truth
├── docs/
│   ├── uml/                     .puml sources (see §1.2)
│   ├── postman/                 versioned collection + environments
│   └── decisions/               ADR copies of Glifo_Bitacora_Decisiones
├── prototype/                   Figma export, 39 frames
└── README.md
```

Three top-level folders named after the three tiers. The tier separation from §2 is not just
a diagram convention — it is the directory structure, so it cannot silently rot.

### 11.3 FRONT — Android

Root package `cr.ac.una.glifo`.

```
android/app/src/main/java/cr/ac/una/glifo/
│
├── GlifoApplication.kt              @HiltAndroidApp
├── MainActivity.kt
│
├── pipeline/                        ◄── THE DIFFERENTIATOR, at the top level
│   ├── PipelineEngine.kt
│   ├── model/                       PageRegion · RegionResult · ConfidenceScore
│   ├── preprocess/                  ImagePreprocessor · OpenCvPreprocessor · QualityAnalyzer
│   ├── segment/                     RegionSegmenter · RegionClassifier
│   ├── ocr/                         TextOcrEngine · MlKitTextOcrEngine
│   ├── confidence/                  ConfidenceScorer · ScoringWeights
│   ├── escalation/                  EscalationPolicy · EscalationStep · N1/N1_5/N2 steps
│   └── hash/                        PerceptualHasher
│
├── feature/                         One package per capability
│   ├── auth/
│   ├── capture/
│   ├── note/
│   ├── coverage/
│   ├── study/
│   ├── course/                      Teacher-side course, syllabus, glossary
│   └── admin/                       Users, roles
│
├── core/                            Cross-cutting, no business logic
│   ├── common/                      Result · AppError · Constants
│   ├── network/                     ApiClient · AuthInterceptor · ErrorMapper
│   ├── database/                    GlifoDatabase · Converters · dao/
│   ├── sync/                        SyncQueue · SyncWorker · RetryPolicy
│   └── ui/                          theme/ · component/
│
└── di/                              NetworkModule · DatabaseModule · PipelineModule · RepositoryModule
```

Every package under `feature/` repeats the same three-layer shape:

```
feature/<name>/
├── data/
│   ├── remote/        <X>Api.kt (Retrofit) + dto/ + <X>Mapper.kt
│   ├── local/         <X>Dao.kt + <X>Entity.kt
│   └── <X>RepositoryImpl.kt
├── domain/
│   ├── model/         Pure Kotlin data classes
│   ├── <X>Repository.kt          ◄── the interface
│   └── usecase/       One file per use case
└── presentation/
    ├── <X>Screen.kt
    ├── <X>ViewModel.kt
    └── <X>UiState.kt
```

**Two changes from the current structure in `Glifo_Arquitectura_Estandares.md` §2**, both in
service of the screaming test:

1. `feature/teacher/` → **`feature/course/`** and **`feature/admin/`**. `teacher` names a
   *role*, not a capability. It answers "who uses this" instead of "what is this", and it
   forces a decision every time a screen is used by two roles. Course management and user
   administration are two different capabilities that happened to share an actor.
2. `pipeline/` is confirmed at the **top level, as a sibling of `feature/`**, not inside
   `feature/capture/`. It is the thing that distinguishes this project from the other eight
   teams building the same brief. Burying it two levels down inside a feature folder would
   be the structure disagreeing with the product.

### 11.4 BACK — Spring Boot on Kotlin

Root package `cr.ac.una.glifo`. Organized **by aggregate**, not by technical layer. Source
root is `src/main/kotlin`, which is what `start.spring.io` generates for a Kotlin project.

**On the asymmetry with §11.3.** The Android module keeps `src/main/java` even though its
contents are `.kt` — that is the Android Gradle Plugin default and what Android Studio
creates, so the whole ecosystem shares the misnomer. The Kotlin Gradle plugin compiles `.kt`
from either directory, so this is a naming convention, not a build constraint. Follow each
platform's default rather than forcing one on both.

```
backend/
├── build.gradle.kts                 kotlin("jvm") · plugin.spring · plugin.jpa
├── settings.gradle.kts
└── src/main/kotlin/cr/ac/una/glifo/
│
├── GlifoApplication.kt
│
├── user/                            One package per aggregate
│   ├── controller/UserController.kt
│   ├── service/UserService.kt
│   ├── repository/UserRepository.kt
│   ├── entity/User.kt · Role.kt · Privilege.kt
│   ├── dto/UserRequest.kt · UserResponse.kt
│   └── mapper/UserMappers.kt        top-level extension functions
│
├── course/                          courses · enrollments · topics · glossary
├── note/                            notes · pages · corrections
├── study/                           coverage · items · attempts · schedule
│
├── ai/                              ◄── ORCHESTRATION, at the top level
│   ├── AiOrchestrator.kt
│   ├── engine/                      MathOcrEngine · SimpleTexEngine · VisionMathEngine · Factory
│   ├── service/                     VisionRepair · Reconstruction · Generation · Explanation · SemanticJudge
│   ├── prompt/                      PromptBuilder.kt
│   ├── validation/                  LatexValidator.kt
│   ├── client/                      SimpleTexClient · LlmClient  (declarative HTTP interfaces)
│   └── ledger/                      CostLedgerService.kt · AiCall.kt
│
├── notification/                    PushService (FCM) · DeviceController
│
├── security/                        JwtTokenProvider · JwtAuthenticationFilter · UserDetailsServiceImpl
├── config/                          SecurityConfig · CorsConfig · OpenApiConfig · JacksonConfig
└── common/
    ├── exception/                   GlobalExceptionHandler · ApiError · domain exceptions
    ├── response/                    ApiResponse · PageResponse
    └── audit/                       Auditable.kt
```

**One Kotlin-specific note on file granularity.** Java forced one public class per file;
Kotlin does not. `UserMappers.kt` holding six extension functions is idiomatic, and so is
`AuthDtos.kt` holding `LoginRequest`, `RegisterRequest` and `AuthResponse` together — they
are three declarations of one concept. The rule to apply: **one file per concept, not one
file per declaration**, and the file is named after the concept. Do not carry the Java habit
of `LoginRequest.kt` containing four lines.

**The screaming test applied.** The top level reads `user · course · note · study · ai ·
notification`. Five of those six are the domain. `security`, `config` and `common` are the
framework, and they are the *last* three entries on purpose. Compare with the default Spring
tutorial layout — `controller/ service/ repository/ model/` — which would put every one of
Glifo's fifteen entities into four undifferentiated folders.

**Where a new file goes** is answerable without asking anyone: an endpoint that returns a
course glossary is `course/controller/`; a new AI call type is `ai/service/`; a new
`@ExceptionHandler` is `common/exception/`.

### 11.5 DB — migrations

```
database/
├── migration/                       Flyway, applied in order, never edited after merge
│   ├── V1__baseline_schema.sql              15 core tables + CHECK constraints
│   ├── V2__seed_access_control.sql          roles, privileges, role_privileges
│   ├── V3__operations_tables.sql            ai_calls, sync_queue, devices, notifications, snapshots
│   ├── V4__indexes.sql                      FK indexes, (user_id, course_id), unique join_code
│   └── V5__jsonb_constraints.sql            kind-matches-payload checks, schemaVersion presence
├── seed/
│   ├── demo_course_eif411.sql               the course used in the live defense
│   └── demo_calibration_notes.sql           the three prepared photographs
└── schema/
    └── glifo_erd.dbml                       optional dbdiagram.io export, generated from V1
```

**Why the calibration data is a versioned seed file.** The scope commits to three prepared
photographs for the live demo — good (N1), fair (N1.5), poor (N2). If they live on one
laptop, the demo has a single point of failure. As a seed script in the repository, any team
member can reproduce the demo state in one command.

---
## 12. Traceability and open items

### 12.1 Screen → use case → endpoint → tables

The point of this table is that it is checkable in both directions. A screen with no endpoint
is a screen with no data. A table nothing reaches is a table that should not exist. Both
conditions are visible here and nowhere else.

| Screen | Prio | Use case | Endpoint | Core tables |
|---|:--:|---|---|---|
| A2 Login | **I** | `LoginUseCase` | `POST /auth/login` | `users`, `user_roles`, `role_privileges` |
| A3 Register | **I** | `RegisterUseCase` | `POST /auth/register` | `users`, `user_roles` |
| C1 My courses | **I** | `GetMyCoursesUseCase` | `GET /courses` | `courses`, `enrollments` |
| C2 Join course | **I** | `JoinCourseUseCase` | `POST /enrollments` | `enrollments`, `courses` |
| C3 Course hub | **I** | `GetCourseSummaryUseCase` | `GET /courses/{id}` | `courses`, `syllabus_topics`, `topic_coverage` |
| D1 Camera | **I** | `ProcessCaptureUseCase` | — *(local, N0–N1)* | — *(Room only)* |
| D2 Rejection | **I** | — *(local)* | — | — |
| D3 Processing | **I** | `UploadPageUseCase` | `POST /notes/{id}/pages` | `note_pages`, `ai_calls` |
| E1 Note list | **I** | `GetNotesUseCase` | `GET /notes` | `notes` |
| E2 Confidence map | **I** | `GetNoteUseCase` | `GET /notes/{id}` | `notes.content`, `note_pages.regions` |
| E3 Correct fragment | **I** | `CorrectFragmentUseCase` | `PATCH /notes/{id}/fragments/{spanId}` | `notes.content`, `course_glossary` |
| E4 Formula viewer | D | — *(reads cached content)* | — | `notes.content`, `note_pages.storage_uri` |
| E5 Provenance | **I** | — *(reads cached content)* | — | `note_pages.regions` |
| F1 Coverage | **I** | `GetCoverageUseCase` | `GET /courses/{id}/coverage` | `topic_coverage`, `syllabus_topics` |
| F2 Topic detail | **I** | `GetTopicDetailUseCase` | `GET /courses/{id}/topics` | `syllabus_topics`, `topic_coverage`, `notes` |
| F3 Delta | D | `GetCoverageDeltaUseCase` | `GET /courses/{id}/coverage/delta` | `topic_coverage` **only** — see §4.4 |
| G1 Study hub | **I** | `GetDueItemsUseCase` | `GET /review-schedule?dueBefore=` | `review_schedule`, `study_items` |
| G2 Flashcards | **I** | `SubmitAttemptUseCase` | `POST /attempts` | `attempts`, `review_schedule` |
| G3 Quiz | **I** | `SubmitAttemptUseCase` | `POST /attempts` | `attempts`, `study_items` |
| G4 Result | **I** | `RecalculateCoverageUseCase` | `POST /courses/{id}/coverage/evaluate` | `topic_coverage`, `ai_calls` |
| H1 Sync queue | D ③ | `GetSyncStateUseCase` | — *(local)* | `sync_operations` *(Room)*, `sync_queue` *(L2)* |
| H2 AI usage | ⚠ | `GetUsageUseCase` | `GET /courses/{id}/ai-usage` | `ai_calls` *(L2)* |
| I2 New course | **I** | `CreateCourseUseCase` | `POST /courses` | `courses` |
| I3 Publish syllabus | **I** | `PublishSyllabusUseCase` | `POST /courses/{id}/syllabus` | `courses`, `syllabus_topics` |
| I4 Glossary | D | `UpsertGlossaryUseCase` | `POST /courses/{id}/glossary` | `course_glossary` |
| I5 Group gaps | D | `GetCourseGapsUseCase` | `GET /courses/{id}/gaps` | `topic_coverage`, `enrollments` |
| J1 Users | D | `GetUsersUseCase` | `GET /admin/users` | `users`, `user_roles` |
| J3 Roles | D | `AssignRolesUseCase` | `PATCH /admin/users/{id}/roles` | `user_roles`, `roles`, `role_privileges` |
| K1 Settings | **I** | — | — | *`SharedPreferences`, never a table* |

**Three things this table proves on inspection.**

1. Every one of the fifteen core tables appears at least once, so nothing was modeled
   speculatively.
2. D1, D2, E4, E5 and H1 have no endpoint at all — those five screens work with the network
   off, which is the offline claim stated as a property of the design rather than as a promise.
3. **The Imprescindible column is a closed, self-consistent product.** Strike every `D`, `O`
   and `⚠` row and what remains still runs end to end: log in, join a course, capture,
   confidence map, correct, coverage, study, sync. No Imprescindible screen depends on a
   Deseable one. That is the property that makes the §11 cut list safe to execute under
   deadline pressure, and it is worth stating out loud because it is not automatic.

**The administrator role is Deseable, not core.** `Glifo_Alcance.md` §6.1 and
`Glifo_Diseno_Arquitectura.md` §13 both place it under Deseable. Earlier revisions of this
document treated the J-zone screens as part of the core product. They are not — but the
`users` · `roles` · `privileges` **tables** and the `ROLE_ADMIN` seed row are Imprescindible,
because they answer the Lab 1 deduction and they are what `@PreAuthorize` reads. The schema is
core; the administration UI on top of it is not. Do not let the second get cut and take the
first with it.

### 12.2 Answering the Lab 1 deduction

The single deduction was 1.5/3 on the database UML diagram, with two observations. This is
where each is closed.

| Observation | Closed by | One-line defense |
|---|---|---|
| *"You need the `privilege` and `roles` tables"* | §4.5 | Five tables, composite keys on the bridges, sixteen seeded privileges, and Spring Security annotated against privileges rather than roles — so a fourth role is a data change, not a code change |
| *"Explore the use of JSON"* | §5 | A stated criterion with a list of where it was **not** applied; seven JSONB columns; four normalized tables eliminated from the quiz model; a `CHECK` constraint keeping the discriminator column and the payload honest |
| *(carried from the closing remark)* second user role | §4.5, §10.2 | Three roles seeded, three distinct home screens, and a role router keyed on privileges |
| *(implicit)* "reduce the model size" | §4.1–4.2 | 23 tables → 15 on the defended diagram, with the merge justification being strict 1:1 cardinality in every case |

### 12.3 Amendments required to `Glifo_Arquitectura_Estandares.md`

Two changes in this document contradict the standards document as it stands. Both need to be
applied there, and both need a decision record.

#### (a) Backend language — Java → Kotlin · proposed **D-13**

`Glifo_Arquitectura_Estandares.md` describes a Java backend in three places, all of which
have to be rewritten:

| Section | Currently says | Becomes |
|---|---|---|
| §1.1 topology | "BACKEND — Spring Boot monolítico" with Java packages | Unchanged in shape; language line updated |
| §3 backend packages | `GlifoApplication.java`, `.java` throughout | `src/main/kotlin`, `.kt` throughout — see §11.4 here |
| §7.2 Java conventions | Lombok, `@RequiredArgsConstructor`, getters/setters, Java sample | Kotlin conventions and sample — see §7.5 here |

Add to §5 (design patterns): the **Builder** row currently justified by "prompts composed of
optional parts" should note that Kotlin's named and default arguments cover most of what a
Builder was for; `PromptBuilder` survives because it accumulates state across conditional
branches, which named arguments do not do.

**The honest trade, measured against the right number.** The constraint that decides this is
not the course regulations — it is `Glifo_Alcance.md` §11: **~284 hours of work against ~285
hours of team capacity**, a budget its own author annotates *"Queda sin margen."* Any language
change spends learning time out of a budget with one hour of slack, and `Glifo_Alcance.md`
§11.4 assigns the backend to a **single person** (Frente B), so the cost is not spread.

Against that: the team already writes Kotlin daily on the Android side, one language across
the repository means code review works in both directions, two dependencies disappear
(§7.5), and Spring's own documentation covers Kotlin completely. Java's advantage is more
Spanish-language Spring tutorials and more Stack Overflow answers — real, but it applies to
a language the team uses less often.

**The deciding question is not "which language is better" but "how fluent is Frente B in
Kotlin outside Android?"** If the answer is *fluent*, the cost rounds to zero and the
benefits stand. If the answer is *has only written Kotlin against the Android SDK*, then
Spring + JPA + Hibernate in an unfamiliar dialect is being learned on the critical path, and
the one-hour margin is where that lands. That is a question only the team can answer, and it
should be answered before Lab 4 opens on 9 September rather than during it.

**The one technical risk** is §7.5's compiler-plugin trap. Mitigate it by generating the
project from `start.spring.io` with Kotlin + Gradle selected, which wires all three plugins
correctly, rather than converting a Java skeleton by hand.

#### (b) Document and code language — Spanish → English · proposed **D-14**

Adopting all-English for this document contradicts §7.3 of the standards document, which
currently reads:

> **Idioma:** el código en inglés; los comentarios, la documentación y los textos de la
> interfaz en español.

That line has to be replaced. The proposed replacement, which keeps the one distinction that
actually matters:

> **Language.** Code, identifiers, database objects, API paths, JSON payload fields, commit
> messages and technical documentation are written in English. **User-facing interface text
> is written in Spanish**, because the users are Spanish-speaking students. UI strings live
> in `res/values-es/strings.xml`, with `res/values/strings.xml` as the English default —
> which also satisfies the internationalization requirement in Module 3 of the course
> programme.

Two consequences to note before this is applied:

1. **Test names.** §7.1 currently prescribes Spanish test names in backticks
   (``fun `debería marcar incierto…`()``). Under the amendment they become
   ``fun `should mark uncertain when latex does not compile`()``. This is a rename across
   however many tests exist at the time — cheap now, expensive in November.
2. **This is a real trade, not an obvious improvement.** Spanish comments are easier for the
   team to write quickly and the professor reads Spanish. The argument for English is that
   the scientific article is required to be in English, the identifiers already are, and a
   codebase that mixes languages inside one file is harder to read than one that does not.
   The decision belongs in `Glifo_Bitacora_Decisiones.md` as **D-14**, with this paragraph
   as its rationale.

### 12.4 Open items

| # | Item | Owner | Deadline |
|---|---|---|---|
| 1 | **D-12 colour collision.** `escalated` and `alert` share `#E0693A` / `#B94117`. Escalation to vision is normal operation, not a failure, and the confidence map is defended at Lab 3 | Front | 9 Sep, before Lab 3 |
| 2 | **D-07 pix2tex.** Still open pending the four questions to the professor. Affects whether `MathOcrEngine` gets a third implementation | Engine | Ask on 19 Aug |
| 3 | **Storage for page crops.** `note_pages.storage_uri` is modeled but the backing store is undecided — object storage, filesystem on the host, or `BYTEA`. Free hosting tiers have ephemeral filesystems, which rules out the middle option in practice | Back | Lab 4, 23 Sep |
| 4 | **Refresh-token strategy.** `POST /auth/refresh` is in the inventory but no table stores refresh tokens, since `users.token_expired` was deliberately dropped (§4.5). Either accept short-lived access tokens with re-login, or add a `refresh_tokens` table to Level 2 | Back | Lab 6, 28 Oct |
| 5 | **`RegionResult` nullable pair** (§6.2). Tolerated for flat JSONB serialization. Revisit only if a third content type appears | Engine | — |
| 6 | **Full-text search on `study_items.payload`** — not in scope; one GIN index if it ever is | Back | — |
| 7 | **Serialization conflict.** `Glifo_Diseno_Arquitectura.md` §10 commits to **Gson**; `Contexto_Curso.md` §6 allows either; `Glifo_Arquitectura_Estandares.md` is silent. §5.3's three sealed item kinds argue for kotlinx.serialization. Pick one and record it | Front | Lab 3, 9 Sep |
| 8 | **`ai_calls` view priority.** `Glifo_Alcance.md` §6.6 says Opcional, §12 puts it in the minimum product, §14 spends 30 seconds of the defense on it. Resolve in favour of one | All | 19 Aug |
| 9 | **Ratify or reject the `○` rows in §3.2** — Flyway, Gradle, springdoc, JJWT, MockK, Jackson, Navigation Compose, JLaTeXMath-Android. None appear in any project document; all were proposed here | All | Lab 4, 23 Sep |
| 10 | **Frente B's Kotlin fluency outside Android** (§12.3a). Determines whether D-13 costs zero or costs the one-hour margin in `Glifo_Alcance.md` §11 | Back | Before 9 Sep |

### 12.5 Checklist before the 19 August correction

- [ ] Export `02_erd_core`, `04_erd_access_control` and `09_back_domain` to PNG at
      presentation resolution
- [ ] Have `V1__baseline_schema.sql` and `V2__seed_access_control.sql` written — a diagram
      backed by runnable DDL is a stronger answer than a diagram
- [ ] Be able to state, in one sentence each: why the bridge tables exist, why seven columns
      are JSONB, and why the model went from 23 tables to 15
- [ ] Have §5.2 ready — the four tables JSON eliminated is the concrete answer to *"explore
      the use of JSON"*
- [ ] Bring the four D-07 questions to ask in person
- [ ] Decide **D-13** (Kotlin backend) and **D-14** (all-English) as a team, then apply both
      §12.3 amendments to `Glifo_Arquitectura_Estandares.md`
- [ ] Generate the backend skeleton from `start.spring.io` with **Kotlin + Gradle** selected,
      and confirm `plugin.spring` and `plugin.jpa` are both in `build.gradle.kts` — this is
      cheap now and is a lost afternoon in Lab 4

---

## Appendix — diagram index

| # | Diagram | Tier | Section |
|---|---|---|---|
| 01 | `stack_deployment` | ALL | §3.1 |
| 02 | `erd_core` | DB | §4.3 |
| 03 | `erd_operations` | DB | §4.4 |
| 04 | `erd_access_control` | DB | §4.5 |
| 05 | `front_pipeline` | FRONT | §6.1 |
| 06 | `front_pipeline_contracts` | FRONT | §6.2 |
| 07 | `front_mvvm_slice` | FRONT | §6.3 |
| 08 | `front_data_offline` | FRONT | §6.4 |
| 09 | `back_domain` | BACK | §7.1 |
| 10 | `back_web_service_slice` | BACK | §7.2 |
| 11 | `back_ai_orchestration` | BACK | §7.3 |
| 12 | `back_security` | BACK | §7.4 |
| 13 | `component_api_seam` | FRONT ↔ BACK | §8.2 |
| 14 | `seq_capture_to_note` | ALL | §9.1 |
| 15 | `seq_offline_sync` | FRONT ↔ BACK | §9.2 |
| 16 | `nav_toplevel` | FRONT | §10.2 |
| 17 | `nav_capture_flow` | FRONT | §10.3 |
| 18 | `nav_study_flow` | FRONT | §10.4 |

Eighteen diagrams. Every one of them fits on a projected slide and answers one question.
That was the constraint: not fewer diagrams, but no overloaded ones.
