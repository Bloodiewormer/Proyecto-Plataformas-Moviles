# FCM incremental migration for the initialized cloud database

Status: applied once to Neon on 2026-10-28 after explicit user authorization; see docs/cloud-deployment.md for the execution record. Do not rerun it against this database: the script refuses replay. Human review is pending.

Candidate SHA256: 769c237a84b9f936eb191106f88e9ee428d6e6db51ac3788bb2fed4eb7a7dd96. Local image/evidence hashes and limitations are recorded in docs/cloud-deployment.md.

## Exact scope

The published cloud preparation is based on 6e8d5a0. Upstream main 3da8b04 adds FCM mappings for devices and notifications. The original cloud-bootstrap.psql remains unchanged and must not be rerun against the initialized database. Apply 049-fcm-incremental.psql only after the original baseline, not instead of it.

The candidate adds installation_id, is_active and updated_at to devices; widens its fcm_token to varchar(512) and permits null tokens. It adds device_id, idempotency_key, status, attempts, fcm_message_id, last_error and created_at to notifications, with the mapped unique constraints, status check, device foreign key and two lookup indexes. Existing table grants and identity sequences are retained. There are no new login roles, catalog rows, account rows, backfills, deletes or table recreation.

This is intentionally restricted to empty devices and notifications. A deployment with historical rows needs a separate reviewed data-migration policy; this script refuses to invent installation identities or claim historical FCM delivery status. Other tables, including users and reference catalogs, are not required to be empty and are not modified.

## Preconditions and transaction

- Confirm project patient-boat-45032812, production branch br-plain-leaf-b5041ubn, database neondb and the direct endpoint by authenticated read-only checks immediately before execution.
- Confirm the exact candidate hash approved by the user. Git line-ending attributes keep both psql artifacts in canonical LF form; compute SHA256 on the actual executable file rather than assuming a checkout matches an earlier hash.
- Supply the owner connection privately through libpq variables. Use neondb_owner, verify-full TLS and a valid trusted CA store. The postgres:18 image alone lacks an OS CA bundle in the tested environment; mount a trusted public CA bundle read-only and configure SSL_CERT_FILE when using sslrootcert=system. Do not disable verification, put a password in arguments, echo an environment dump or enable SQL statement logging.
- The script checks the database/user, table ownership, runtime-role presence and original baseline columns/token type. It uses ON_ERROR_STOP, one transaction, a 5-second lock timeout, a 60-second statement timeout, search_path=pg_catalog and the same transaction advisory lock as the bootstrap.
- Exclusive locks on devices and notifications are acquired before checking emptiness or altering either table. Concurrent reads/writes to those tables can be blocked while the transaction runs; a lock timeout aborts rather than waiting indefinitely.
- A partial or repeated migration is rejected. Do not add IF NOT EXISTS, retry automatically, regenerate the applied baseline or enable Hibernate create/update/create-drop to bypass a refusal.

## Operator command after separate approval

Run from the backend root with the private libpq environment already supplied:

~~~text
psql -X --file docs/db/049-fcm-incremental.psql
~~~

Capture only sanitized command status and read-only metadata. A failed transaction rolls back its own changes; a confirmed COMMIT is durable. No destructive down-migration is provided. If the client disconnects without a confirmed outcome, inspect the schema read-only before requesting any retry.

After COMMIT, read-only checks must verify 8 device columns, 13 notification columns, nullable varchar(512) token, the new constraints/indexes, unchanged reference counts/runtime grants and zero rows in the two affected tables. Then validate the updated sanitized image with Hibernate validate, SQL initialization disabled and FCM disabled. Remote application startup, HTTP writes, enabling real push delivery, deployment and GitHub mutations remain separately gated.

## Local test gate

CloudFcmMigrationTest is opt-in and does not use the project's glifo or glifo_test databases. It creates random-name PostgreSQL 18 containers with no network, no published ports and tmpfs-only storage, initializes the original baseline through a NOSUPERUSER owner and removes only its own fixtures.

~~~powershell
$env:GLIFO_CLOUD_MIGRATION_DOCKER_TEST='true'
.\gradlew.bat test --tests '*CloudFcmMigrationTest' --rerun
~~~

The fixture image postgres:18 must already exist locally; tests use --pull=never. The eight cases cover empty-baseline migration and grants, populated devices, populated notifications, replay, wrong role, wrong database, forced late failure/rollback and unexpected token type. SQL-based guards are exercised rather than inferred from string inspection.

Local image validation and final review results belong in docs/cloud-deployment.md. A passing local gate does not prove the current remote destination is still unchanged or authorize applying the script.

## Cloud FCM configuration

The cloud profile supplies url.devices and url.sync. GLIFO_FCM_ENABLED defaults to false, credential paths default to empty and application-default credential discovery defaults to false. Disabled FCM returns NOT_CONFIGURED/FCM_DISABLED, never a simulated delivery success. Keep FCM_ENABLED unset or false as well, because the existing client recognizes that legacy alias.

Enabling actual push delivery requires an explicit operating decision, a private Firebase service-account file or an approved application-default identity, a matching project and a separately approved real-device smoke test. Do not embed a service-account JSON in the image/repository or add new public debug endpoints. The current preparation does not enable real sends.
