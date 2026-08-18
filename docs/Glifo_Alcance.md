# Glifo — Scope

**Team X-Ray** — Brandon Brenes · David González · Felipe Ugalde
Mobile Application Development · II Term 2026
Scope document · version 2.1

> **Related Documents**
> · `Glifo_Arquitectura_Estandares.md` — architecture, packages, classes, conventions
> · `Glifo_Diseno_Arquitectura.md` — version for consultation with the professor
> · `Glifo_UML_Modeling.md` — visual modeling and API seam
> · `Glifo_Bitacora_Decisiones.md` — historical record of decisions
> · `Contexto_Curso.md` — course framework

---

## 1. Identity

**Glifo.** A glyph is the minimum unit of writing: the individual hand-drawn sign. It is exactly what the application reads, evaluates, and decides whether it understood.

The visual identity is built upon the **mythological griffin** (grifo) —eagle's head, lion's body— bringing the two core ideas of the product: sharp sight to read what is hard to read, and guardianship of the student's content.

> The glyph is the unit of writing. The griffin, its guardian.

**Legal constraint.** The aesthetic inspiration cannot include third-party intellectual property. The mythological griffin is public domain.

### Base Palette

Glifo defines **two complete modes** —Night and Day— rather than a dark theme with a light variant bolted on. Both share the exact same token structure; only the values change.

| Token | Night | Day | Usage |
|---|---|---|---|
| `background` | `#161E27` | `#EDEAE0` | App background |
| `surface` | `#2E3B4B` | `#F7F4EC` | Cards and elevated surfaces |
| `surfaceHigh` | `#3B4A5C` | `#D7D1B9` | Inner fill over `surface`: progress tracks, fields, crops |
| `border` | `#4A5A6E` | `#C4BCA3` | Card outlines and separators |
| `accent` | `#FFD372` | `#FFD372` | Primary action fill and active state |
| `accentText` | `#FFD372` | `#8A6210` | The accent applied to text or icon over a background |
| `textPrimary` | `#D7D1B9` | `#2E3B4B` | Main text |
| `textSecondary`| `#959595` | `#63666A` | Secondary text |
| `alert` | `#E0693A` | `#B94117` | Destructive actions and errors |

The full set of derived tokens —`onAccent`, `scrim`, the `Soft` / `Faint` / `Line` variants of every semantic color, `btnSecBorder`, `btnSecText` and `neutralSoft`— is in `Glifo_Arquitectura_Estandares.md` §12.3.

**Gold is the color of action, not brand decoration.** If it appears, the element can be touched or is active. Brand identity rests on the griffin logo, not on a reserved color.

> `accent` and `accentText` are **not** the same token in Day mode. `accent` is the fill of a primary button (`#FFD372` in both modes); `accentText` is gold used as text or icon over a background, darkened to `#8A6210` in Day mode to reach legible contrast. Never `#FFD372` as text over a light background.

### Confidence States

Functional scale of the confidence map. Every state carries **non-chromatic coding in addition to color**: roughly 8 % of men have some color vision deficiency, and the green/amber pair is the worst discriminated. If the confidence map depended on color alone, the central function of the application would be inaccessible to those users.

| State | Night | Day | Non-Chromatic Coding | Level |
|---|---|---|---|---|
| **Verified** | `#5FA88C` | `#2F7D62` | Solid underline 2px, no fill | N1 |
| **Repaired** | `#8FB7DC` | `#3E6E9E` | Solid underline 2px, `repairedSoft` fill | N1.5 |
| **Escalated**| `#F59E0B` | `#D97706` | Solid underline 2px, `escalatedFaint` fill | N2 |
| **Uncertain**| `#959595` | `#63666A` | **Dotted** underline 2px, `uncertainSoft` fill | UNR |

Additionally, any fragment not resolved at N1 carries a **textual label** detailing its state and level (e.g., `REPAIRED · N1.5`, `ESCALATED · N2`, `UNCERTAIN`), and the note header summarizes the distribution (`14 verified · 2 repaired · 1 escalated · 1 uncertain`). Color is never the sole information carrier.

**Escalation is not failure.** `escalated` uses its own amber, distinct from `alert`. Escalating to a vision model is normal pipeline operation; presenting it in the error color would contradict the core argument of the product. See `Glifo_Bitacora_Decisiones.md` D-12.

---

## 2. Problem and Proposal

The cycle required by the brief —notes, coverage against syllabus, directed reinforcement— depends entirely on a first step: that a photograph of a handwritten note is faithfully converted into structured content.

In courses with mathematical notation, this step fails. Standard text OCR does not recognize fractions, exponents, matrices, or limits. The result is a reconstructed note that *looks* correct to the system, but generates study material based on an error.

> **Glifo digitizes handwritten notes —formulas and diagrams included— scaling processing only when necessary, and shows the student what it understood securely, what it had to repair, and what requires human review.**

| Commitment | Implementation |
|---|---|
| Nothing is invented in silence | Every fragment carries a calculated, visible confidence level |
| No photo is left unprocessed | Automatic escalation to the next engine when the threshold is missed |
| Cost is a design decision | Every level has a known cost; the system records which level resolved each region |

**Framing:** Glifo is not "local instead of cloud." It is **local-first, cloud when necessary, and only for the specific region that needs it.**

---

## 3. Principles

1. **AI does not do everything.** Local, deterministic processing is maximized before invoking any model.
2. **Every AI call produces a persistent result.** Already computed content is never sent to AI again.
3. **Never one call per item.** Generation and repair always travel in batches.
4. **Minimal context.** The syllabus is processed once and never sent entirely again.
5. **Uncertainty is displayed.** If the system couldn't read something, it marks it; it does not hallucinate an answer.

---

## 4. Roles

| Role | Responsibilities |
|---|---|
| **Student** | Capture notes, review and correct the reconstruction, study, check coverage and AI usage. Student corrections generate *glossary suggestions*. |
| **Teacher** | Create courses, publish syllabus, maintain the **canonical notation glossary** (approving student suggestions), check group coverage gaps. |
| **Admin** | Manage users, roles, and privileges; monitor system status. |

The glossary maintained by the teacher directly feeds the pipeline's confidence calculation. It is a functional input, not just a reference dashboard.

**MVP commitment:** two operational roles (Student and Teacher) over the full `users` · `roles` · `privileges` · `user_roles` · `role_privileges` structure. The Admin role is Should-have and is seeded as data, not as new code — Spring Security is annotated against privileges, not roles.

---

## 5. Main Flow

```text
TEACHER
  creates course → publishes syllabus (PDF) → defines canonical glossary
  → processed once → persisted

STUDENT
  joins via code
        │
        ▼
  CAPTURE (CameraX) → perceptual hash
        │            └─ page already processed → reused, zero calls
        ▼
  ╔═══════ BRANCHED LADDER ═══════════════════════════╗
  ║ N0  Local OpenCV pre-processing                    ║
  ║     deskew · perspective · contrast                 ║
  ║     quality metrics · region segmentation           ║
  ║     └─ unrecoverable → prompt retry WITH REASON     ║
  ║                        │                            ║
  ║ N1  Local ML Kit OCR on ALL regions                 ║
  ║                        │                            ║
  ║             REGION CLASSIFIER (Router)              ║
  ║          │             │              │             ║
  ║        TEXT          MATH          DRAWING          ║
  ║      resolved          ▼              │             ║
  ║       at N1     N1.5 MATH OCR         │             ║
  ║          │        → LaTeX + conf      │             ║
  ║          └─────────────┬──────────────┘             ║
  ║                        ▼                            ║
  ║     VALIDATION GATES (deterministic, NO AI):        ║
  ║       confidence below threshold      [device]      ║
  ║       LaTeX does not compile          [backend]     ║
  ║                        ▼                            ║
  ║ N2  SELECTIVE VISION REPAIR                         ║
  ║     crops · ONE CALL PER PAGE                       ║
  ║                        ▼                            ║
  ║ N3  Full page to vision · on student demand         ║
  ╚═════════════════════════════════════════════════════╝
        │
        ▼
  IA-01 RECONSTRUCTION → JSON + confidence per fragment
        │
        ▼
  REVIEW WITH CONFIDENCE MAP
     └─ manual corrections generate glossary suggestions
        │
        ▼
  SYLLABUS COVERAGE
     local pre-filter resolves the majority
     └─ ambiguous zone → IA-04 batch semantic adjudication
        │
        ▼
  STATES PER TOPIC + PROGRESS DELTA
        │
        ▼
  IA-02 flashcards + quizzes of the gaps · 1 call per batch
        │
        ▼
  LOCAL EVALUATION → deterministic spaced repetition
```

---

## 6. Features

*(Priorities: Must-have, Should-have, Could-have)*

### 6.1 Course Baseline

| Feature | Lab | Priority |
|---|---|---|
| Auth, register, navigation, lists | 2 | Must-have |
| Navigation drawer/menu | 2 | Must-have |
| `users` · `roles` · `privileges` + bridge tables | 4 | Must-have |
| Student and Teacher roles | 2 | Must-have |
| Administrator role | 6 | Should-have |
| Hilt DI | 3 | Must-have |
| Retrofit, Gson, error handling | 3 | Must-have |
| Mock API consumption | 3 | Must-have |
| Spring Boot: Repository, Services, DTOs | 4–5 | Must-have |
| PostgreSQL with JSONB | 4 | Must-have |
| Postman collection | 5 | Must-have |
| JWT and Spring Security | 6 | Must-have |
| Cloud deployment | 6 | Must-have |
| Firebase App Distribution | Final | Must-have |
| Real Push notifications | 6 | Must-have |
| Survey to 5 external users | Final | Must-have |

### 6.2 Capture and Ingestion — Core of the Project

| Feature | Lab | Priority |
|---|---|---|
| CameraX and capture screen | 2 | Must-have |
| **N0** OpenCV pre-processing | 2 | Must-have |
| Quality diagnostic with concrete rejection reason | 2 | Must-have |
| Region segmentation | 3 | Must-have |
| **Region classifier as router** | 3 | Must-have |
| **N1** Local text OCR | 3 | Must-have |
| **ConfidenceScorer** and calibration | 3 | Must-have |
| **Confidence Map overlay in UI** | 3 | Must-have |
| Manual fragment correction | 3 | Must-have |
| **N1.5** Math OCR to LaTeX | 6 | Must-have |
| **N2** Selective vision repair | 6 | Must-have |
| LaTeX validation via JLaTeXMath (backend, deterministic) | 6 | Should-have |
| LaTeX rendering alongside original crop | 6 | Should-have |
| **N3** Full page request | 6 | Should-have |
| Deduplication via perceptual hash | 4 | Should-have |
| Canonical notation glossary | 5 | Should-have |
| Import from gallery | 2 | Could-have |
| Multi-page continuous scanning | — | Could-have |

### 6.3 Knowledge and Coverage

| Feature | Lab | Priority |
|---|---|---|
| Courses and join via code | 5 | Must-have |
| Syllabus processed once and persisted | 5 | Must-have |
| **IA-01** Reconstruction to JSON | 6 | Must-have |
| Coverage local pre-filter | 5 | Must-have |
| **IA-04** Semantic adjudication | 6 | Must-have |
| States with operative definitions | 5 | Must-have |
| Automatic topic detection | 5 | Should-have |
| Manual syllabus fallback | 5 | Should-have |
| **Coverage delta** between sessions | 5 | Should-have |
| Historical snapshots | 5 | Could-have |
| Weekly progress chart | 6 | Could-have |

### 6.4 Study

| Feature | Lab | Priority |
|---|---|---|
| **IA-02** Flashcards and quizzes in batch | 6 | Must-have |
| Deterministic local grading | 6 | Must-have |
| Deterministic spaced repetition | 6 | Must-have |
| Rescheduling of failed items | 6 | Should-have |
| Flashcard self-evaluation | 6 | Should-have |
| Mastery metrics | 6 | Should-have |
| Quiz history | 6 | Could-have |
| **IA-03** On-demand explanation | 6 | Could-have |
| Exam dates and prioritization | — | Could-have |

### 6.5 Offline and Synchronization

*(Corresponds to the applied research topic assigned to the team).*

| Feature | Lab | Priority |
|---|---|---|
| Room as read cache | 4 | Must-have |
| Offline consumption of all generated content | 4 | Must-have |
| **LocalCourseContext cache for offline pipeline** | 4 | Must-have |
| Offline capture queue (Outbox) | 4 | Must-have |
| Unidirectional synchronization | 5 | Must-have |
| **Retries with backoff and idempotency** | 5 | Must-have |
| Sync status indicator | 5 | Should-have |
| Push notification to specific `device_id` when queue drains | 6 | **Must-have** |

### 6.6 Telemetry and Testing

| Feature | Lab | Priority |
|---|---|---|
| Record `ai_calls` with reached level | 6 | Must-have |
| **AI usage view for Student and Teacher** | 6 | **Must-have** |
| Firebase Analytics with custom events | 6 | Should-have |
| Crashlytics | 3 | Should-have |
| Unit tests for ConfidenceScorer | 5 | Should-have |
| Sync policy tests | 5 | Could-have |

---

## 7. Out of Scope

Declared explicitly as future work:
- Bidirectional sync conflict resolution
- Semantic comparison via vector embeddings
- Contradiction detection between notes
- Complete study planning tied to calendar assessments
- Hard usage quotas with degraded modes
- Streaks, achievements, and social comparison
- Sharing notes via external channels
- Biometric authentication

The full record of evaluated alternatives, with the reason each was rejected, is in `Glifo_Bitacora_Decisiones.md`.

---

## 8. AI Calls

| ID | Function | Trigger | Batching |
|---|---|---|---|
| **OCR-M** | Formula → LaTeX (N1.5) | Region classified as math | Per region |
| **IA-00** | Selective vision repair | Regions failing validation gates | **One call per page** |
| **IA-01** | Reconstruction to JSON structure | Once per note | Batch |
| **IA-02** | Flashcards and quizzes | Once per topic set | **Batch** |
| **IA-03** | On-demand concept explanation | User request | Cached result |
| **IA-04** | Coverage semantic adjudication | Only the gray zone of the pre-filter | Batch |

**No AI is used for:** pre-processing, text OCR, segmentation, classification, deduplication, confidence calculation, LaTeX validation, coverage pre-filtering, progress delta, quiz grading, spaced repetition scheduling.

---

## 9. Architecture and Data Model

Detailed in `Glifo_UML_Modeling.md` and `Glifo_Arquitectura_Estandares.md`. Summary:

- **Android Client** in Kotlin, Compose, MVVM with Hilt.
- **Backend Monolith in Kotlin/Spring Boot** with Spring Security and JWT, organized by domain, using `AiOrchestrator` as the facade for LLM calls.
- **PostgreSQL** as main DB with selective JSONB; **Room** as cache and outbox.
- **16 core tables** on the defended diagram, plus a 5-table operations annex. English nomenclature.
- All external service credentials reside strictly in the backend.

---

## 10. Schedule

All labs are defended live during Wednesday classes. **The operative date is the last class before the official deadline.**

| Defense | Lab | Course Requirement | Glifo Parallel Work |
|---|---|---|---|
| **19 Aug** | Lab 1 Corr. | Access tables · JSON rationale · bounded model | Teacher role in prototype · ladder screens |
| **26 Aug** | Lab 2 | Menu, auth, navigation, lists · two role flows | OpenCV compiling · CameraX · **N0** · rejection reasons |
| **9 Sep** | Lab 3 | Hilt · Retrofit · errors · Mock API | **N1** · segmentation · **ConfidenceScorer** · **confidence map** |
| **23 Sep** | Lab 4 | Backend · PostgreSQL · Repository | Full schema · **Room and offline queue** · deduplication |
| **14 Oct** | Lab 5 | Services · REST · DTOs · Postman | Syllabus · coverage pre-filter · **sync with retries** · glossary |
| **28 Oct** | Lab 6 | JWT · Spring Security · **AI API** · cloud | **N1.5 and N2** · IA-01/02/04 · LaTeX · `ai_calls` · push |
| 1–10 Nov | — | No open deliverables | Integration · survey · article · defense rehearsal |
| **11 Nov** | Final | APK · JWT · survey | Defense and applied research with Q&A round |

**Critical week: September 23** — Lab 4 defense while Labs 5 and 6 are already open.

---

## 11. Capacity and Distribution

Three members · 7 hours/week of independent study each · 13.5 weeks ≈ **285 team hours**, including the scientific article and weekly evaluations.

| Block | Hours |
|---|---|
| Course baseline | ~165 |
| Capture and ingestion | ~106 |
| Knowledge and coverage | ~52 |
| Study | ~42 |
| Offline and sync | ~33 |
| Telemetry and testing | ~24 |
| **Complete catalog** | **~422** |

The complete catalog exceeds capacity. Applying priority filters:

| Level | Hours |
|---|---|
| Must-have | ~223 |
| Should-have | ~61 |
| **Must + Should** | **~284** |
| Could-have | ~38 |

There is zero slack. **To recover buffer, drop items in this order:**
1. N3 (Full page request)
2. Historical snapshots
3. Sync status indicator
4. Flashcard self-evaluation
5. Manual syllabus fallback

The targeted push (§6.5) was promoted from Should-have to Must-have without changing these
totals: it was already inside the Must + Should budget, and it is item 8 of the MVP. It is
therefore **not** on the cut list — dropping it would break the offline story the applied
research is built on.

### Work Fronts

| Front | Scope |
|---|---|
| **A — Client** | Compose, navigation, roles in UI, Hilt, Retrofit, confidence map |
| **B — Backend** | Spring Boot, PostgreSQL, Repository, DTOs, JWT, deployment |
| **C — Engine** | OpenCV, ML Kit, ladder, Room, sync queue, push, **applied research** |

Fronts define primary responsibility, not exclusivity. All members review all code.

---

## 12. Minimum Product (MVP)

Must function end-to-end on November 11:

1. Authentication with two real roles over the full access structure and JWT.
2. Teacher: create course, publish syllabus. Student: join.
3. Capture with **N0 → N1 → N1.5 → N2 ladder** and visible reached level.
4. **Confidence Map** with manual correction (generating glossary suggestions).
5. **At least one handwritten formula** recognized and rendered.
6. Coverage with four states and **delta** between sessions.
7. Flashcards and quizzes generated in batch, graded locally.
8. **Offline Queue**: capture offline with `LocalCourseContext`, sync upon reconnect, notify via push.
9. `ai_calls` screen visible: calls, level, and savings proven.
10. Backend deployed and APK distributed.

---

## 13. Risks

| Risk | Mitigation |
|---|---|
| OpenCV integration in Android | First task of Lab 2. Include only necessary modules to save size. |
| Segmentation fails to discriminate | Degrade to full-page processing (higher cost, but system keeps operating). |
| Threshold calibration | Fixed set of calibration photographs defined week 1; adjust weights against it. |
| Incorrect formula transcription | Original crop is preserved and shown next to the LaTeX. |
| Math OCR engine latency/downtime | Test in Lab 5. `MathOcrEngine` is swappable, falls back to vision model. |
| Region classification error | If N1.5 gets text, confidence will be low and it will escalate anyway. Costs latency, not correctness. |
| Partial synchronization | Retries with exponential backoff and idempotent UUIDs. |
| Cloud deployment | Tackle it during Lab 5, not waiting for Lab 6. |

---

## 14. Defense Script

| Min | Content |
|---|---|
| 0:00 | All nine projects assume note photos read well. The brief highlights poor OCR as the main risk. That risk *is* our project. |
| 0:30 | Good photo → processed locally, **zero network calls**. |
| 1:15 | Photo with an integral → router sends only that region to math engine while the rest is local → LaTeX renders with original crop next to it. |
| 2:15 | Bad photo → escalating to vision, or rejection **with concrete reason**. |
| 3:00 | Confidence Map. *We never silently hallucinate what we couldn't read.* |
| 3:45 | Coverage, delta, and quiz — brief, since everyone has this. |
| 4:15 | Ledger: *five pages, three free engines, only two paid model calls.* |
| 4:45 | The two roles and the teacher's glossary. |

---

## 15. Synthesis

Glifo's functional cycle is the one from the brief, shared by all nine teams.

The proprietary contribution lies in **guaranteeing the first step of that cycle**: verifiably converting a photograph of handwritten notes, including mathematical notation, into structured content; scaling processing cost solely on the regions that require it, and making the confidence level of every fragment explicit to the student rather than presenting unreadable text as fact.
