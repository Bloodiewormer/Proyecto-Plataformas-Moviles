# Incremental migration: user profile photos

Status: prepared, NOT applied to Neon. Apply it before deploying the backend that includes `UserAvatar`, because the cloud profile runs Hibernate `validate` and refuses to start without the table.

## Scope

Creates only `public.user_avatars` (one row per user: `user_id` primary key and foreign key to `users` with `ON DELETE CASCADE`, `content_type` limited to JPEG/PNG, `data bytea` up to 1 MiB, `updated_at`) and grants DML to `glifo_runtime`. It does not modify any existing table, row, catalog or grant. There is no sequence because the key is the user id.

## Guards

Same transaction pattern as `049-fcm-incremental.psql`: `ON_ERROR_STOP`, one transaction, 5 s lock timeout, 60 s statement timeout, `search_path=pg_catalog`, advisory lock. It refuses a wrong database or role, a missing `glifo_runtime`, a `users` table not owned by `neondb_owner`, and a repeated run (table already exists). Do not add `IF NOT EXISTS` or rerun it automatically.

## Operator command (after explicit approval)

From the backend root, with the owner connection supplied privately through libpq variables (`verify-full` TLS):

~~~text
psql -X --file docs/db/user-avatars-incremental.psql
~~~

Read-only postcheck: `public.user_avatars` exists with 4 columns, `pk_user_avatars`, `fk_user_avatars_user`, the two checks, and `glifo_runtime` has SELECT/INSERT/UPDATE/DELETE on it. Then deploy the backend on Render.

## Rollback

`DROP TABLE public.user_avatars;` removes only the photos. Do it only after rolling back the backend, or the cloud profile will fail Hibernate validation.
