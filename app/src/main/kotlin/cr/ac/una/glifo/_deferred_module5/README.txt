UPDATE (D-28, Lab 4): Room is active again in `data/datasource/local/` with the two tables
the offline pipeline needs: `local_course_context` (#27) and `processed_pages` (#28), wired
by `data/di/DatabaseModule.kt`. What is left in this folder (notes, outbox, users) is still
deferred; move it into `data/datasource/local/` when that work is resumed.

Per D-24 (Glifo_Bitacora_Decisiones.md) and Glifo_Arquitectura_Estandares.md §2:

  "No `data/local/` package or DAO exists in the Android code until [Module 5 /
  Investigación Aplicada]; this is a scheduling fact."
  "Note on `core/database/`: removed from this listing per D-24 — Room,
  `GlifoDatabase`, `Converters`, and any `dao/`/`entity/` packages are deferred to
  Module 5 / Applied Research and must not exist in the codebase before then."

This folder holds the team's existing Room modeling work (GlifoDatabase, DAOs, entities,
converters, and the Hilt DatabaseModule that wired them), preserved as `.kt.txt` so it is
NOT picked up by the Gradle Kotlin source set (which only compiles `**/*.kt` under
app/src/main/kotlin). Nothing was thrown away — when Module 5 starts, rename these back to
`.kt`, move them under `cr.ac.una.glifo.data.datasource.local` (per §2: local persistence
belongs in `data/datasource/`, not a top-level `core/database/`), and re-add the Room
dependency usage in `data/di/`.

`core/sync/SyncWorker.kt` used to read `SyncQueueDao` from here; it's now a scaffold with a
TODO instead, since the sync queue it will eventually drain doesn't have anywhere to persist
until this comes back.
