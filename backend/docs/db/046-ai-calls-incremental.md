# ai_calls incremental migration for the initialized cloud database (#46)

Status: NOT applied. Applying it to Neon requires explicit user authorization, like 049. Human review is pending.

## Why it exists

The cloud profile runs Hibernate with `ddl-auto=validate`. Issue #46 adds columns to `ai_calls` (`model`, `success`, `output_accepted`, `error_code`, `error_message`) and a new table `ai_avoided_calls`, so an image built from this code does not start against the current Neon schema until this script is applied. Local development is unaffected: `ddl-auto=create` rebuilds the schema on every start and `docs/db/schema.sql` is the regenerated reference.

## Exact scope

Apply `046-ai-calls-incremental.psql` after the original `cloud-bootstrap.psql` baseline and after `049-fcm-incremental.psql`. It changes `public.ai_calls` and creates `public.ai_avoided_calls`:

- `level`, `input_tokens`, `output_tokens` and `estimated_cost` become nullable: NULL means "unknown" (a level that does not apply to IA-01/IA-02/IA-04, tokens the provider did not report, a cost without a configured rate), never zero.
- `estimated_cost` widens from `numeric(10,6)` to `numeric(12,8)` (USD, 8 decimals). Same integer part, so no value can lose precision.
- New columns `model varchar(100)`, `success boolean NOT NULL`, `output_accepted boolean`, `error_code varchar(64)` and `error_message varchar(255)`, plus the check `ck_ai_calls_outcome`.
- New table `ai_avoided_calls` (`id` identity with sequence `ai_avoided_calls_id_seq`, `user_id`, `course_id`, `call_type`, `level`, `created_at`): one row per request answered with an already stored result, without calling the provider. It has the same `call_type`/`level` checks as `ai_calls`, the foreign keys Hibernate names (`course_id` with `ON DELETE SET NULL`) and the index `idx_ai_avoided_calls_user_created`. `glifo_runtime` gets the same grants as on every other application table: DML on the table and USAGE/SELECT on its sequence.

On `ai_calls`, existing grants, the identity sequence, the foreign keys and `idx_ai_calls_user_created` are kept. There are no new roles, backfills, deletes or table recreation.

It is restricted to an empty `ai_calls`. Before #46 nothing in the application wrote to that table, so the initialized database is expected to be empty. A database with historical rows needs a separate reviewed policy: the script refuses to invent a `success` value for calls whose outcome was never recorded.

## Preconditions and transaction

Same operating rules as 049 (see `049-fcm-incremental.md` and `docs/cloud-deployment.md`): confirm the Neon project, branch, database and direct endpoint read-only right before running it; use `neondb_owner` through private libpq variables with verify-full TLS; compute the SHA256 of the actual file. The script uses ON_ERROR_STOP, one transaction, a 5-second lock timeout, a 60-second statement timeout, `search_path=pg_catalog` and the transaction advisory lock `(197701, 46)`. It checks the database and role, the table owner, the runtime role, the exact baseline columns, that the table is empty and that `ai_avoided_calls` does not exist yet, after taking an ACCESS EXCLUSIVE lock on `ai_calls`. A partial or repeated run is refused; do not add IF NOT EXISTS or enable Hibernate create/update to bypass a refusal.

~~~text
psql -X --file docs/db/046-ai-calls-incremental.psql
~~~

After COMMIT, read-only checks should show 15 `ai_calls` columns, nullable `level`/`input_tokens`/`output_tokens`/`estimated_cost`, `numeric(12,8)`, non-null `success`, the new check constraint and zero rows; and an empty `ai_avoided_calls` with 6 columns, owned by `neondb_owner`, on which `glifo_runtime` has SELECT/INSERT/UPDATE/DELETE and USAGE/SELECT on `ai_avoided_calls_id_seq`. Then validate the image with Hibernate `validate`.

## Local test gate

`AiCallPersistenceTest` applies the block between `BEGIN MIGRATION BODY` and `END MIGRATION BODY` to a throwaway schema in `glifo_test` that holds the baseline `ai_calls` copied from `cloud-bootstrap.psql`. It then checks that every column of both tables matches the one Hibernate generates from the entities (type and nullability), that `ai_avoided_calls` has the same primary key, checks and foreign keys, and drops the schema. The GRANT statements sit after the body because `glifo_test` has no `glifo_runtime` role. The identity, empty-table and replay guards and the grants need the Neon role layout and are not covered locally. The Docker-based gate used for 049 was not extended because Docker was not available when this was written.
