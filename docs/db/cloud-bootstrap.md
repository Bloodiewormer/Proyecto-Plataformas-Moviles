# One-time cloud database initialization

Status: the exact approved script was applied once on 2026-10-28 to project patient-boat-45032812, production branch br-plain-leaf-b5041ubn, database neondb. It exited 0 with COMMIT; read-only counts and runtime privileges were verified. See docs/cloud-deployment.md for the execution record and evidence hash. Do not rerun it against this initialized database. Any other destination or schema operation needs separate explicit approval.

## Scope and provenance

cloud-bootstrap.psql is a psql 18 script for an empty neondb database owned by neondb_owner. It creates 21 application tables, their constraints/identity sequences, three application roles, 16 privileges, 30 role/privilege links, and one PostgreSQL login role glifo_runtime. It inserts no users, courses, documents, password hashes or demo data. Application roles are reference rows, not PostgreSQL login roles.

DDL was exported without data/ownership/grants/comments from a new tmpfs PostgreSQL 18 fixture initialized by the reviewed application image sha256:ec08768cd1fffcf052b87593706a0f3c6015981ae22d7b803a73a64026532cb1 (base 6e8d5a0 plus the cloud packaging patch). The existing docs/db/schema.sql contains demonstration rows and must NOT be executed as production initialization. Only its five explicit roles/privileges/role_privileges reference INSERT statements were reused. No demonstration identity/password hash was copied.

This is a baseline for that exact entity set, not a migration framework or an automatic upgrade. Future schema changes require reviewed incremental migrations. Do not regenerate/overwrite an approved script silently.

## Preconditions and secrets

- Confirm the target using authenticated Neon read-only API checks, including project and branch IDs, region and PostgreSQL version. A matching database name alone cannot identify the correct project/branch. Independently inspect the destination schema immediately before execution.
- Confirm that the public schema has no application tables, views, sequences, foreign tables, functions or types and that glifo_runtime does not exist. The script checks those object classes again inside a transaction and aborts instead of adopting/replacing existing objects or a role. The transaction uses search_path=pg_catalog and explicitly qualifies all DDL/catalog targets and references with public.; inherited search_path cannot redirect the catalog inserts.
- Supply the initialization connection privately as libpq environment variables: PGHOST (verified direct endpoint, not the pooler), PGPORT, PGDATABASE=neondb, PGUSER=neondb_owner, PGPASSWORD, PGSSLMODE=verify-full and PGSSLROOTCERT=system. Use a PostgreSQL 18 client with a working system CA trust store. Never put credentials in command arguments, repository files, issue/PR text or terminal transcripts.
- Supply GLIFO_RUNTIME_PASSWORD privately, using a unique strong password of at least 32 characters. The operator must validate that it is nonempty and sufficiently strong before invocation. psql reads it from the process environment; no password is embedded in the script. Do not enable statement/parameter logging, shell tracing, psql echo modes, or copy an environment dump; a literal password is sent to PostgreSQL's CREATE ROLE command.
- Use an isolated temporary credential location if needed, outside the repository, with access restricted to the user and delete it after use. The runtime application must receive glifo_runtime credentials, never neondb_owner credentials.

## Exact operator command, only after approval

From the backend repository, with the privately injected environment above:

~~~text
psql -X --file docs/db/cloud-bootstrap.psql
~~~

Do not use --echo-all, --echo-queries, --single-step, interactive recovery or ON_ERROR_ROLLBACK overrides. -X skips personal/system psql startup customizations. The script enables ON_ERROR_STOP, acquires a transaction-scoped advisory lock, checks the target/preconditions, creates the schema/reference data/role, applies grants and commits once. A failure exits rather than continuing; an open transaction rolls back when psql disconnects. Do not retry by deleting tables or roles. Investigate the failure and request a new approval if the content/destination changes.

The script deliberately fails on reapplication; it does not use IF NOT EXISTS to disguise a partial/different installation. It has no DROP, TRUNCATE, migration of existing data or destructive rollback command. Schema CREATE for PUBLIC is revoked in this database. Any reverse migration or deletion requires a separately reviewed operation and approval.

## Runtime permissions

The login has NOSUPERUSER, NOCREATEDB, NOCREATEROLE, NOREPLICATION and NOBYPASSRLS. It owns neither application tables nor the schema. Grants cover CONNECT, public schema USAGE, DML on 18 application tables, SELECT only on roles/privileges/role_privileges, and USAGE/SELECT on the 17 noncatalog identity sequences. It receives no blanket grants for future tables, schema CREATE, table ownership, TRUNCATE or role membership. A future entity or catalog update needs an explicit migration/grant decision.

After initialization, verify the counts (21 tables, 3 roles, 16 privileges, 30 role_privileges and zero users) using read-only queries. Verify actual authentication as glifo_runtime, expected table/schema permissions and rejection of application-schema DDL/catalog writes in an isolated test, not by damaging production objects.

Start only the sanitized image with the unchanged cloud profile, Hibernate validate and SQL initialization disabled. A live HTTP registration smoke would insert user data and needs its own approved fixture/account scope. Read-only health alone does not prove database readiness. Do not enable Hibernate create/update/create-drop on Neon.

## Local verification record

The final candidate passed fresh empty-database initialization by neondb_owner without SUPERUSER, second-run refusal, wrong-database/wrong-user refusal, missing-password refusal, standalone-type/existing-role refusal, forced-error rollback, hostile inherited search_path isolation, restricted-role application startup, read-only catalog permissions and authentication HTTP checks in disposable local PostgreSQL. Evidence, script SHA256 and review findings are recorded in docs/cloud-deployment.md and the local issue record. The final independent static acceptance audit found no new SQL blocker; its two documentation observations were corrected locally. The auditor did not reproduce the tests or independently corroborate every reported guard/permission outcome from complete traces. This paragraph records the local checkpoint. The later explicitly authorized Neon execution and read-only postchecks are recorded in docs/cloud-deployment.md; human review and application deployment remain pending.

Official client references: https://www.postgresql.org/docs/18/app-psql.html and https://www.postgresql.org/docs/18/libpq-connect.html.
