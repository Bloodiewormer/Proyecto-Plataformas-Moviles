# Incremental migration: study items owner user

Status: prepared, NOT applied to Neon. Apply it before deploying the backend that includes `StudyItem.user`, because the cloud profile runs Hibernate `validate` and refuses to start without the column and index.

## Scope

Adds `user_id` column to `public.study_items` (foreign key to `users` with `ON DELETE CASCADE`, nullable for backward compatibility with existing rows) and creates index `idx_study_items_user_course` on `(user_id, course_id)`. Existing foreign keys from `attempts` and `review_schedule` already specify `ON DELETE CASCADE` referencing `public.study_items(id)` in `cloud-bootstrap.psql` (constraints `fkfc34j718tarxisqp0wfdyedgc` and `fkoprg1fo3psxpus02cfj5d6sfw`), so no modifications to dependent tables are needed.

## Guards

Same transaction pattern as `user-avatars-incremental.psql`: `ON_ERROR_STOP`, one transaction, 5 s lock timeout, 60 s statement timeout, `search_path=pg_catalog`, advisory lock `pg_advisory_xact_lock(197701, 61)`. It refuses a wrong database or role, a missing `glifo_runtime`, `study_items` or `users` tables not owned by `neondb_owner`, and a repeated run (`user_id` column already exists on `study_items`). Do not add `IF NOT EXISTS` or rerun it automatically.

## Operator command (after explicit approval)

From the backend root, with the owner connection supplied privately through libpq variables (`verify-full` TLS):

~~~text
psql -X --file docs/db/study-items-owner-incremental.psql
~~~

Read-only postcheck: `public.study_items` has `user_id` column nullable referencing `public.users(id)` with ON DELETE CASCADE, index `idx_study_items_user_course` exists, and `glifo_runtime` retains SELECT/INSERT/UPDATE/DELETE. Then deploy the backend on Render.

## Rollback

~~~sql
DROP INDEX IF EXISTS public.idx_study_items_user_course;
ALTER TABLE public.study_items DROP COLUMN IF EXISTS user_id;
~~~

Do it only after rolling back the backend, or the cloud profile will fail Hibernate validation.
