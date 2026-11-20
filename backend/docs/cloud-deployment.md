# Cloud deployment of the monolithic backend

Status: the original reviewed baseline is initialized in Neon and the preparation branch is published. Upstream FCM changes are now integrated in an isolated local candidate with cloud configuration and an incremental migration; local tests and image/HTTP smoke passed. This new migration has NOT been applied to Neon and the integration has no merge commit or push. Independent/human review, exact remote authorization and Render deployment remain pending.

Publication gate: the isolated candidate incorporates main3da8b04 without changing the original checkout. Do not deploy updated code against Neon until its separately reviewed incremental migration is explicitly authorized and verified there. Previous bootstrap/push approvals do not cover this new operation.

## Artifact and configuration

The root Dockerfile builds the application with the repository's Gradle wrapper and Java 21, then copies only the executable JAR into a Java 21 JRE image. Runtime UID/GID is 10001:10001, not root. The image starts with the cloud profile explicitly selected.

The Docker context uses an allowlist: wrapper, Gradle build descriptors and main application sources only. Local Gradle configuration, Git metadata, tests, build output and environment files are excluded. Development application.properties and import.sql are explicitly excluded. Inside the build stage, application-cloud.properties also becomes the base application.properties, so the image does not package development credentials or demonstration accounts. An ordinary local bootJar is NOT this sanitized deployment artifact: do not upload it instead.

The cloud profile uses Hibernate validate and disables SQL initialization. It will refuse an empty or incompatible schema rather than creating/recreating one or loading demo data. Environment overrides can still change Spring configuration; never set schema creation, SQL initialization, debug logging or development-profile overrides in the provider dashboard.

A key-only comparison with the current development configuration accounts for every base key. The only intentional omissions are spring.profiles.active (selected explicitly by the image entrypoint) and spring.config.import (no personal/local configuration imports in the image). PostgreSQL driver and Hibernate UTF-8 settings are preserved. A regression test detects any future base key that is neither represented in cloud nor explicitly excluded. No development values were copied into the cloud configuration.

## Required environment

Set these at runtime through the provider's secret controls. Never pass credentials with Docker build arguments, commit them, print environment dumps or put them in a public issue/PR.

| Variable | Value | Requirement |
| --- | --- | --- |
| DB_JDBC_URL | jdbc:postgresql://HOST:5432/DATABASE?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory | Verified provider endpoint; validate certificate and hostname using JVM trust, as checked by the JDBC probe. sslmode=require is not equivalent to this verification |
| DB_USERNAME | Dedicated application database role | No superuser, schema owner or DDL privileges |
| DB_PASSWORD | Database password | Secret, no default |
| JWT_SECRET | Random signing secret of at least 32 UTF-8 bytes | Secret, unique to this environment; no development value |
| CORS_ALLOWED_ORIGIN | Exact permitted HTTPS browser origin | One origin, not a comma-separated list; native Android requests do not require a browser origin |
| PORT | Provider-assigned HTTP port | Default 8080; bind address is 0.0.0.0 |
| DB_MAX_POOL_SIZE | JDBC pool size | Optional, default 5; respect provider connection limits |
| JWT_EXPIRATION_MS | Token lifetime | Optional, default 86400000 |
| OPENAI_API_KEY | Optional AI service credential | Empty by default; existing AI operations remain unconfigured without it. Setting it may enable billable requests, which need separate approval |
| OPENAI_MODEL | AI model identifier | Optional; retains the existing application default, gpt-4o-mini |
| OPENAI_BASE_URL | AI API endpoint | Optional; retains the existing application default. Send no credential to an unreviewed endpoint |
| OPENAI_TIMEOUT_SECONDS | AI request timeout | Optional; default 60 |
| GLIFO_FCM_ENABLED | Explicit FCM opt-in | Optional, default false; keep legacy FCM_ENABLED unset/false; enabling sends needs separate approval |
| GLIFO_FCM_PROJECT_ID | Firebase project ID | Optional, upstream default glifoapp; verify against the selected private credentials before enabling |
| GLIFO_FCM_CREDENTIALS_PATH | Private service-account file path | Empty by default; never embed JSON in the image/repository |
| GLIFO_FCM_CREDENTIALS_FILE | Alternative private credential path | Empty by default; prefer one explicit configured path |
| GLIFO_FCM_USE_APPLICATION_DEFAULT | Credential discovery opt-in | Optional, default false; do not enable implicitly |
| GLIFO_FCM_MAX_ATTEMPTS | Bounded send attempts | Optional, default 3; current service clamps to 1-5 |
| GLIFO_FCM_RETRY_BACKOFF_MS | Retry delay milliseconds | Optional, default 200 |

Required means the operator must supply the variable, not that every missing variable produces the same startup error. Strict Spring property resolution rejects unresolved markers, but the actual Boot Binder preserves unresolved DB markers as literal strings. Tests verify that behavior without connecting to PostgreSQL. A missing DB setting therefore has no development fallback, but its failure may appear during driver/connection/authentication initialization rather than placeholder resolution. A successful configuration unit test is not proof of a successful application startup.

The base images use the major Java 21 tags rather than immutable digests. Resolve and review their digests and vulnerability scan before a published release; the present recipe does not claim bit-for-bit reproducibility.

## Database prerequisite: explicit operator gate

PostgreSQL must already contain a schema matching these JPA entities and the required reference catalogs (roles, privileges and their associations). Creating a blank managed database alone is insufficient. Schema provisioning is NOT automated in this change and is a blocker for the first live deployment.

Before a deployment, obtain a schema-only export from a separately verified compatible database or prepare reviewed migrations, inspect it for ownership/extension requirements, and apply it using a separate privileged migration role only after explicit approval for the exact destination and operation. A schema-only export does not include the role/privilege application catalogs: provision those from a separately reviewed, minimal reference-data script. Do not export real users, personal data, demo password hashes or the development import.sql seed. The local candidate, isolated validation and subsequently authorized Neon initialization are recorded below. Future migrations still require their own reviewed destination, operation and approval.

Keep an approved backup/restore procedure for an existing destination. Runtime credentials must not be the migration credentials. Do not work around a schema-validation failure by enabling create, create-drop or update. The full repository test suite also recreates a schema and is not part of deployment validation.

## Local verification without database changes

From this backend repository:

~~~powershell
.\gradlew.bat test --tests "*CloudConfigurationTest" --tests "*ApiContractJsonTest"
.\gradlew.bat bootJar
~~~

These tasks do not start the application or connect to PostgreSQL. The regular JAR is only a compilation check and still contains the existing development resources. Do not upload it.

Once a Linux-container Docker engine is available, the next local gate is:

Build a release candidate only from a clean checkout of the exact reviewed commit. The present working tree includes preexisting changes outside this issue (including settings.gradle.kts and User.kt), so an image built directly from it would include those changes and would not represent the reviewed commit. Do not discard or commit those files to make the tree appear clean. Use an approved clean checkout/export when validating the release artifact.

~~~text
docker build --tag glifo-api:local .
~~~

Inspect the image's JAR to confirm that import.sql is absent and both application property files contain only cloud placeholders/settings. Confirm UID 10001, Java 21 and the cloud startup arguments. These checks passed for the local artifact recorded below, not for an arbitrary future release. Gradle compilation alone does not validate a Dockerfile.

The runtime gate must also measure JVM/container memory under the selected plan and exercise LatexValidator in the actual JRE image, including its AWT/font requirements in headless operation. There is currently no dedicated HTTP LaTeX-validation endpoint; use an agreed temporary in-container test harness or a real application call path that invokes the validator, without inventing an endpoint or exposing a debug route. Host LatexValidatorTest results do not validate fonts/native dependencies inside the container. The limited in-image LaTeX smoke check below passed; full application load/memory checks remain pending. Set and verify a memory budget before choosing JVM sizing options; do not assume a small instance is sufficient.

Do not start the container against a real database until its destination, schema and credentials have been reviewed. A future authorized run can use an external, private environment file (not committed) and expose only the application port. Never use the development docker-compose.yml as production infrastructure.

## Render deployment checklist (proposed, not executed)

Render is a documented option, not a selected or provisioned provider. Check current pricing, database retention, region, memory and network limits before creating anything. Do not assume that a free database or sleeping service is suitable for persistent production data.

1. Obtain approval for the provider, plan/cost, region, repository/commit and each resource creation. Keep automatic deploys disabled; this change must not introduce a hook that publishes on push.
2. Provision a private PostgreSQL service and reviewed schema/catalogs through the separate operator gate above. Keep the runtime role restricted.
3. Create a Docker web service from this backend repo with context root and Dockerfile at the repo root. Set the required runtime variables in the dashboard. A postgres:// URL is not a JDBC URL: construct the jdbc:postgresql:// endpoint explicitly, preserving the provider's TLS requirements.
4. Use /api/v1/health as the HTTP health-check path. The existing endpoint is public and reports application liveness only; it does not query PostgreSQL or prove schema/business readiness.
5. After approval of the exact deployment, inspect redacted startup logs, confirm schema validation, then perform HTTPS health, authentication and authorized read checks. Any write used for a smoke test requires its own known test account/data and approval; never test with real user data by default.
6. Record URL, deployed commit/image digest, schema version, health/auth results and rollback procedure before declaring the deployment successful. Roll back to the previous compatible image without deleting data. An incompatible schema requires an operator decision, not an automatic destructive rollback.

No API route or Android base URL is changed by this preparation. Before closing #49, the application must actually run with managed PostgreSQL, satisfy the checks and pass the team's human review. GitHub push, PR, comments and issue closure each require their own confirmation. No deployment or resource creation is authorized merely by following this file.

## Local Docker evidence: 2026-10-28

Built from a temporary export of committed base 6e8d5a01799b0d0543b0f8dfd6a7e644290a7711 plus only the six issue files (Dockerfile, .dockerignore, cloud properties, configuration tests, this runbook and the README link). Protected settings.gradle.kts, User.kt and EntityRelationshipsTest.kt matched the committed base after CRLF normalization. No prior local changes or personal Gradle configuration were included. This is an uncommitted-patch validation artifact, not a reviewed release commit.

- Command: docker build --progress=plain --tag glifo-api:issue49-local . from that temporary context; successful. Gradle inside the build completed in 2m23s.
- Local image ID: sha256:ec08768cd1fffcf052b87593706a0f3c6015981ae22d7b803a73a64026532cb1; size 158,900,017 bytes. No registry push occurred.
- Resolved build base: eclipse-temurin:21-jdk-jammy@sha256:4e61d0b3471137e239397789661f6adeaf8db9adcfae45916b22f628c8a32dc0.
- Resolved runtime base: eclipse-temurin:21-jre-jammy@sha256:9603446cf51296842eab342514857cf7ab55a8891cd564e6c68ec237fae65a16.
- Extracted image JAR SHA256: 34117667384cf6a08a5a12a6decfb09196a0b093d34ef15b2719b02b868f5e11. Both packaged property files matched cloud configuration; import.sql was absent.
- Image inspection and isolated execution confirmed UID 10001, the explicit cloud entrypoint and Temurin Java 21.0.12.1.
- A temporary Java smoke harness loaded the actual packaged LatexValidator and dependencies from that image. With no network, read-only root and read-only harness mount, headless mode accepted three formula cases and rejected one unknown command. Under a 512 MiB container cap, the harness reported max heap 129,761,280 bytes and cgroup memory peak 61,685,760 bytes. These figures describe only the short validator smoke, not Spring Boot startup, database usage or production load.
- Application launch without required variables exited 1 with unresolved JWT_SECRET. A separate launch with dummy validation-only settings and unreachable 127.0.0.1:1 database also exited 1 with a connection-refused error. Both used network=none, a 60-second timeout and a 1 GiB memory cap; neither connected to a database nor initialized any schema. No credential values were taken from development configuration.

Temporary test containers were removed; no application is left running. Scratch harness/artifacts/logs are local only. A first harness command failed because PowerShell split an unquoted JVM -D argument; quoting it fixed invocation without changing application/image code. The current Dockerfile, .dockerignore and cloud configuration are the ones tested; later runbook-only edits do not change that Docker context.

## Isolated PostgreSQL smoke evidence: 2026-10-28

The user explicitly approved a new disposable local database and initialization of only its empty schema. This is a test-only exception, not a production provisioning procedure. No existing database, development compose service, external service or paid API was used.

- PostgreSQL 18 ran on the uniquely labeled internal Docker network glifo49-370990dd. Both database and application had no published host ports. The database had only a 512 MiB tmpfs at /var/lib/postgresql; no bind-mounted or persistent/anonymous data volume was attached. Public table count was verified as zero before initialization.
- One bootstrap container used the same previously recorded application image with an explicit test-only --spring.jpa.hibernate.ddl-auto=update override. It created 21 public tables in glifo_cloud49_fixture and started without ERROR log entries, then was stopped. No development import.sql was loaded; users were verified empty. This override must never be copied to deployment configuration.
- A single ROLE_STUDENT row was inserted as disposable reference data; the remaining production role/privilege catalogs were not provisioned or certified. Fresh normal application containers then used the unmodified image entrypoint and cloud validate default, with no DDL override. Both started without ERROR log entries.
- A temporary Java HTTP harness executed inside the application container: GET /api/v1/health returned 200 with data.status=UP; anonymous GET /api/v1/courses returned 401; registration of cloud49@example.invalid returned 201; login returned 200; GET /api/v1/courses with its JWT returned 200 with a data field. Passwords and tokens were not printed or committed.
- After stopping the first normal application and starting a second against the same temporary database, login and authenticated read passed again without registration. All 21 table names/OIDs were unchanged and exactly one fixture user remained. This supports persistence across application replacement without table recreation; it does not prove disk durability across database replacement, because storage was deliberately tmpfs.
- Each application container had a 1 GiB memory cap and two CPUs. Cgroup peaks were 550,268,928 and 401,825,792 bytes; these include the separate Java HTTP harness inside the container, not application-only RSS. This was a short smoke test, not a load test, and does not establish suitability for a 512 MiB hosting plan.
- Disposable PostgreSQL bootstrap credentials were also used for application access in this isolated test. A restricted runtime database role, production grants, TLS and managed-database behavior remain unverified. This fixture must not be reused as production credentials or least-privilege evidence.
- All four owned containers and the owned internal network were removed, destroying only this approved tmpfs database. Temporary environment files were removed. The application image and noncredential test logs/harness remain local. No application or database from this test remains running.

Local evidence is isolated-validation-summary.json plus isolated-*-startup.log and isolated-http*.log in the existing glifo-cloud49-yO13v1 temporary directory; it is not versioned with this runbook. Summary SHA256: 6d5b7fb5e6d62c3045ed3930190e0192124bbf9aef4156b229048f9855039d74. The HTTP harness was initially compiled with JDK 25 and rejected by the JRE 21 image; recompilation with javac --release 21 resolved this harness-only error before the checks passed. No application/image code changed.

Still not verified: reviewed production migrations/reference catalogs, restricted database-role operation, full application load/memory, vulnerability scan, release-commit build, remote deployment/HTTPS and human approval.

## Render-sized isolated smoke evidence: 2026-10-28

A second new disposable PostgreSQL fixture used the same verified application image and isolation rules: internal network glifo49-free-c1c959c4, no published ports, only tmpfs database storage, and zero public tables before initialization. A stopped bootstrap container initialized only this fixture with the test-only update override. No existing local database or Neon database was contacted.

The normal application then ran with the unchanged cloud validate entrypoint, 512 MiB memory, 0.1 CPU, UID 10001, and read-only root. Its dedicated JDBC role glifo_runtime had no superuser, database/role creation, replication, bypass-RLS, schema CREATE, or table ownership rights. Explicit grants were database CONNECT, schema USAGE, table SELECT/INSERT/UPDATE/DELETE, and sequence USAGE/SELECT. PostgreSQL active-session inspection confirmed that the application actually connected as glifo_runtime. Separate transactional negative checks rejected both CREATE TABLE and ALTER TABLE for that role.

Spring reported startup in 346.777 seconds (375.697 seconds of JVM process runtime). An HTTP client in a separate container passed health 200 UP, anonymous courses 401, fixture registration 201, login 200, retained identity, and JWT-authenticated courses 200. The entire client process/check sequence took 56.814 seconds; this is not an individual request latency measurement. Application cgroup peak was 332,730,368 bytes, with 331,866,112 bytes current at the final measurement. The separate HTTP client JVM is excluded from these application figures. This is a short smoke result, not a load test or guarantee of actual Render hardware, startup time or cold-start performance. Startup was slow enough to require explicit review before relying on a live demo.

All three owned persistent test containers, the auto-removed probe, the internal network and four temporary credential files were removed. Only this disposable tmpfs database was discarded. Noncredential local evidence: free-512m-validation-summary.json, free-512m-startup.log, free-512m-http-smoke.log and free-512m-application.log in the previously recorded temporary directory. Summary SHA256: 951ffaed96c154978a6156dd9d6ee4453e3a4418c8adb71df0cd809af221565f. No application code/image was changed for this test. Full production catalogs, Neon TLS/connectivity, managed-database permissions and sustained load remain unverified.

## Existing Neon project: connection preparation, not deployment

Historical selection, before remote inspection: Render Free with the user's existing Neon project. The later read-only verification below confirms Neon metadata and JDBC/TLS connectivity, not Render eligibility or remote application deployment.

Historical authentication attempt: the official Neon CLI 8.13.1 was installed at the user's request, and the first browser login timed out after 60 seconds. The user subsequently completed login; read-only inspection and a private JDBC/TLS probe succeeded as recorded below. Authentication is no longer pending. No init, link, deploy, remote schema change or remote application deployment has been executed.

This is Kotlin/Spring Boot, not a Node application: retain the existing PostgreSQL JDBC driver, Hikari pool and Gradle wrapper. Do not add a Node application, npm build script, Neon serverless JavaScript driver or neon.ts merely to follow generic onboarding. The existing DB_JDBC_URL, DB_USERNAME and DB_PASSWORD runtime variables already configure JDBC. A Neon postgresql:// connection URI is not a JDBC URL; use the console's Java/JDBC format and keep credentials separate. Provider-exported .env/.env.local files are not automatically loaded by this cloud configuration; inject secrets as runtime environment variables instead, never as Docker build arguments.

After successful login, first inspect only the exact user-selected organization/project and production branch. If local CLI linking is needed, skip agent setup/config generation and automatic env-file pulls; inspect its changes and keep local context out of commits. Do not run generic neon init -y or neon deploy blindly: init installs/configures agent tooling by default, and deploy applies a neon.ts service policy rather than building this Spring application or supplying reviewed JPA migrations. A new empty Neon schema must still pass the separate provisioning operator gate above. Inspect destination and TLS without displaying credentials; any migration/reference-data write or real deployment requires a reviewed concrete operation and authorization.

Official connection/CLI references consulted on 2026-10-28: https://neon.com/docs/guides/java and https://neon.com/docs/reference/cli-init. Build evidence for this project is Gradle bootJar plus the focused tests, not npm run build.

## Verified Neon read-only connection: 2026-10-28

After the user completed login, read-only Neon API inspection confirmed the exact project patient-boat-45032812 (glifo-app), organization org-lucky-paper-59961031, production branch br-plain-leaf-b5041ubn, AWS us-east-2 (Ohio), PostgreSQL 18, and the reported free_v3 subscription. Only neondb and neondb_owner were listed; no application-specific runtime role was present. The current account's project access is ADMIN; that is not least-privilege application access.

A temporary JDBC probe used the PostgreSQL driver/dependencies extracted from the already verified image. It first ran on the host and then passed inside the actual image's Java 21 JRE, with a read-only filesystem and a 256 MiB probe cap. This was a separate harness, not a Spring application startup. The connection used sslmode=verify-full with a recording subclass of DefaultJavaSSLFactory that delegated to the default JVM trust factory; hostname validation remained enabled in pgJDBC. The client negotiated TLSv1.3 / TLS_AES_256_GCM_SHA384. For the application JDBC URL, the nonrecording factory is org.postgresql.ssl.DefaultJavaSSLFactory; no probe class belongs in production configuration.

All database queries ran in a verified transaction_read_only=on transaction, with a local statement timeout and rollback. Result: database neondb, role neondb_owner, zero public base tables and zero public views. The role has database/role-creation privileges and is not the restricted runtime role required for deployment. No schema/data changes, account creation, Neon deploy or application startup were performed. Cloud validate cannot pass until an explicitly reviewed schema and required reference data exist; do not work around this by setting update/create in the production runtime.

An initial probe incorrectly treated pg_stat_ssl.ssl=false as evidence that the client had no TLS. That check failed without any database write. The successful probe measured the actual client SSLSocket instead: TLS and peer/hostname verification succeeded, while the backend-reported SSL flag remained false. Client and backend observations are recorded separately; the latter must not be used alone to conclude that the client connection is unencrypted.

The credential-bearing temporary JSON was removed after each attempt and the image probe auto-removed its container. No credentials were printed or put in the repository. Noncredential local evidence: neon-readonly-report.json in the previously recorded temporary directory, SHA256 c8bc8a55e8465c3d6b48a73e93406b21bbff542b9db999cd46cb7b8d819895ab. The report is not versioned with this runbook. Production schema/reference-data initialization and runtime-role creation remain explicit operator approval gates; connection success is not an end-to-end application deployment.

## Reviewed local initialization candidate: 2026-10-28

The one-time operator candidate is docs/db/cloud-bootstrap.psql, documented in docs/db/cloud-bootstrap.md. Its final SQL SHA256 is 9b0306e216ed6beea0f6fa2de3c441c763f81f9e4c814ff58f4039971d7baf82. It was not executed on Neon. It creates 21 tables, 19 identity sequences, three application-role rows, 16 privileges and 30 role/privilege links, with zero users/demo records; glifo_runtime has DML on 18 application tables, read-only access to three authorization catalogs and usage/select on 17 noncatalog identity sequences. The runtime password must be provided privately through GLIFO_RUNTIME_PASSWORD; no password is embedded in the file.

An independent static review found one medium search_path issue and one low incomplete-type guard. Both were corrected: all catalog targets/references are public-qualified, transaction search_path is pg_catalog, and standalone public types are rejected alongside relations/routines. Local execution also found that psql 18 ignores an exit-code argument to quit, so the missing-secret guard now raises a SQL exception under ON_ERROR_STOP instead of returning a false success. These narrow fixes were verified by execution; they do not count as a second independent review. The later final acceptance audit and authorized remote execution are recorded below; this paragraph describes the earlier local checkpoint.

The final script passed in new internal-network/tmpfs PostgreSQL 18 fixtures, never an existing database. The exact candidate was applied by neondb_owner with NOSUPERUSER, CREATEDB and CREATEROLE, matching the relevant observed Neon owner flags rather than relying on the fixture administrator. Negative cases passed: missing secret, wrong database, wrong initialization user, preexisting standalone type and preexisting glifo_runtime. A fault-injected copy (identical except SELECT 1/0 immediately before COMMIT) failed and rolled back all created public objects/reference data and the runtime role. The normal candidate was then applied with inherited search_path=shadow,public and compatible shadow catalogs containing sentinel rows: public received 3/16/30 rows, shadow catalogs remained 1/1/0. Reapplication failed without changing catalog counts or grants.

The normal image then started against this exact SQL-produced schema as glifo_runtime using cloud validate, not an ORM creation override. Actual HTTP health 200 UP, anonymous courses 401, fixture registration 201, login 200 and JWT-authenticated courses 200 passed. Runtime CREATE TABLE, ALTER TABLE and INSERT into roles were denied. The JDBC user was observed in active sessions; runtime role memberships were zero. App cap was 512 MiB with two CPUs and measured peak 519,143,424 bytes, close to the limit. The client ran separately. This short smoke must not be treated as load/headroom evidence or as the prior 0.1 CPU measurement; the slow free-plan-sized startup and variable memory headroom remain deployment risks.

Four owned containers, the auto-removed HTTP client, the private internal network and six temporary credential files were removed; only those disposable tmpfs databases were discarded. Noncredential evidence is baseline-validation-report.json plus baseline-strict-initialization.log, baseline-runtime-validate.log and baseline-http-smoke.log in the existing glifo-cloud49-yO13v1 temporary directory. Report SHA256: 0c5156633d5829b9def473433fc411121ff449afb226b02e98ce5b210ece8057. These local artifacts are not versioned. Protected working-tree changes were not used or modified. No GitHub mutation, commit, remote schema/data initialization or deployment occurred.

Before applying this candidate remotely, complete the acceptance review, ask approval for the exact project/production branch/neondb, script hash and effect, obtain the runtime password through a private secret channel, recheck the destination/preconditions, and use the operator procedure. Approval to prepare or locally test this file is not approval to execute it on Neon.

## Neon initialization completed: 2026-10-28

The user explicitly authorized only the identified initialization after receiving the project, branch, database, script hash, effects (including revoking schema CREATE from PUBLIC) and risk. The runtime password was supplied privately; it is not stored in this repository. Authentication for subsequent deployment requires the password retained by the user.

Destination was revalidated immediately before execution: project patient-boat-45032812, organization org-lucky-paper-59961031, production branch br-plain-leaf-b5041ubn, direct endpoint ep-broad-glitter-b5crra22, database neondb, PostgreSQL 18.6. A read-only transaction confirmed no public relations/routines/types and no glifo_runtime role. The exact approved docs/db/cloud-bootstrap.psql SHA256 was 9b0306e216ed6beea0f6fa2de3c441c763f81f9e4c814ff58f4039971d7baf82.

A single psql execution ran from 17:17:17 to 17:17:32 UTC, exited 0, and returned COMMIT with 21 CREATE TABLE commands and one CREATE ROLE. It was not retried. Read-only postchecks confirmed 21 tables, 19 identity sequences, 3 application roles, 16 privileges, 30 associations and zero users. PUBLIC no longer has schema CREATE. glifo_runtime has LOGIN but no SUPERUSER, CREATEDB, CREATEROLE, REPLICATION, BYPASSRLS, membership, schema/database CREATE or table ownership. Authentication as that role succeeded and effective privileges match DML on 18 tables, SELECT-only on 3 catalogs and USAGE/SELECT on 17 sequences, without TRUNCATE or sequence UPDATE. No production DDL-denial/write smoke was attempted.

The postgres:18 client image lacked an OS CA bundle, so its first strict-TLS read-only connection failed before SQL. A public CA bundle from the reviewed Java21 runtime image was mounted read-only and selected with SSL_CERT_FILE; sslrootcert=system and sslmode=verify-full remained enabled. No verification was disabled. The runtime JDBC probe in the actual Java21 image independently connected with verify-full/JVM trust, observed TLSv1.3/TLS_AES_256_GCM_SHA384, read the 21 tables as glifo_runtime and rolled back a verified read-only transaction. Backend pg_stat_ssl=false was recorded separately and does not describe the verified client TLS socket. The Spring application was not started.

Local sanitized evidence: neon-initialization-report.json SHA256 25c964099bac73e6f2f2cfe58fe590031246564ecce92897e094fbb0e7ef0eed in the temporary evidence directory. The private request files were removed, credential variables cleared, and ephemeral clients auto-removed; the user CLI login remains available. The preexisting glifo-postgres container was not touched. No commit, push, GitHub mutation, HTTP registration or Render deployment occurred.

The final independent static acceptance audit found no new SQL blocker; its TLS-example and historical-state documentation observations were corrected before authorization. It did not reproduce the tests or independently corroborate every prior guard/permission result from complete traces. Human review, publication, Render secrets/deployment and remote application end-to-end checks remain pending. Local memory/startup limits still apply; database initialization alone does not complete issue frontend#49.

## Publication checkpoint and upstream drift: 2026-10-28

Before publication, the safe filtered test command was rerun: test --tests "*CloudConfigurationTest" --tests "*ApiContractJsonTest" --tests "*LatexValidatorTest" --rerun. BUILD SUCCESSFUL in 30 seconds; current JUnit XML records 73 tests, zero failures and zero errors. These results apply to base 6e8d5a0 plus this packaging patch, not the newer upstream code.

A read-only remote check and fetch found origin/main at 3da8b04fcab8517f0f0d68ed72a62f94b8e09164, one commit ahead of this branch. Its GLI-048 FCM changes modify Device and Notification mappings, add Firebase Admin dependencies and controllers requiring url.devices and url.sync, and add deployment settings. The current Neon baseline and reviewed runtime image remain tied to 6e8d5a0; they must not be described as compatible with the newer main. Publishing this preparation branch is not authorization to merge or deploy either version.

Before deployment from updated main, integrate the upstream changes in an isolated checkout, account for new cloud configuration/credentials without enabling unapproved external calls, prepare and test a non-destructive incremental migration from the already-applied baseline, and obtain explicit authorization for that new remote schema operation. Do not rerun or silently regenerate the applied bootstrap, use Hibernate update/create, or discard protected local changes. No such migration, merge, commit, push or deployment was performed at this checkpoint.

## Isolated FCM integration candidate: 2026-10-28

A separate local worktree, C:/Users/Bloodie/.t3/worktrees/glifo49-fcm-integration on chore/49-cloud-fcm-integration, combines published preparation 0cba94b with upstream main 3da8b04 using a conflict-free, uncommitted merge. The original checkout and its protected user changes were not modified. No merge commit, new push, PR or remote migration was created for this integration.

The cloud profile now provides url.devices/url.sync and all new FCM keys. FCM and application-default credential discovery are disabled by default, with empty credential paths; actual sends remain out of scope. The candidate [incremental migration](db/049-fcm-incremental.md) adds only the mapped Device/Notification columns, constraints and indexes after the existing baseline. It rejects populated target tables, wrong database/user, missing baseline ownership/role, unexpected original token type, partial migration and replay. Exclusive table locks protect the emptiness check; no historical data is rewritten. Runtime grants and reference catalogs are unchanged. The applied bootstrap remains the original published LF blob SHA256 9b0306e216ed6beea0f6fa2de3c441c763f81f9e4c814ff58f4039971d7baf82; it must not be rerun. The two psql files have scoped eol=lf attributes for deterministic future checkouts.

Candidate docs/db/049-fcm-incremental.psql SHA256: 769c237a84b9f936eb191106f88e9ee428d6e6db51ac3788bb2fed4eb7a7dd96. It has only run in disposable local fixtures. No additional Neon schema/data changes have been made. A separate exact-destination/hash/effects authorization is required before applying it remotely.

The full safe filtered gate completed BUILD SUCCESSFUL in 59 seconds: CloudConfigurationTest 12, CloudFcmMigrationTest 8, ApiContractJsonTest 11, LatexValidatorTest 53 and FirebaseAdminFcmClientUnitTest 3; total 87 tests, zero failures/errors/skips. Migration tests are explicitly opt-in and create only random-name PostgreSQL18 containers with no network/ports and tmpfs data, through a NOSUPERUSER owner. One initial replay-test timeout was caused by waiting for a process before draining its long stdout; concurrent output capture fixed it. The focused eight migration cases and then the full gate passed. Neither existing glifo nor glifo_test was used.

Docker build completed successfully. The inspected updated image is sha256:18ca1ab8a3aa7e797825d9c4ea0b6657c021f31d606ce5a7950bf021f9944848. Packaged base/cloud properties match, Hibernate is validate, SQL init is never, FCM defaults to disabled, and neither import.sql nor a Firebase credential JSON is present. Firebase Admin 9.4.3 is packaged from upstream. The regular host bootJar remains unsuitable for publication because it is not the sanitized artifact.

A separate internal-network PostgreSQL18 fixture received the exact original LF baseline and new migration, then the actual updated image authenticated as glifo_runtime and started with Hibernate validate. At 512MiB/2CPU, application startup was 18.94 seconds, measured cgroup memory peak was 463699968 bytes and OOMKilled=false. There were no ERROR-level startup/smoke logs. This short local test is not Render0.1CPU hardware, a cold-start guarantee, load testing or evidence of stable memory headroom. The internal network exposed no public port; an attempted host-port query was not used for testing. The HTTP client ran inside the same internal network.

All 13 HTTP checks passed: health, anonymous devices401, fixture registration/login/JWT/courses, device registration with a token longer than255 characters, token-safe response, persisted device list, sync completion, explicit non-success while FCM is disabled, idempotent replay preserving notification identity, and unlink204 with nullable token. Read-only local postchecks found one fixture user/device/sync-entry/notification, notification FAILED_PERMANENT with FCM_DISABLED and one attempt, and inactive device with null token. These are disposable fixture rows, not production data. No real Firebase delivery or Android end-to-end test was performed; external egress was unavailable.

Sanitized evidence: C:/Users/Bloodie/AppData/Local/Temp/glifo49-fcm-validation-PrhmB7/validation-report.json, SHA256 585eda54cde19e9b6ac18c0bf2660925299ebea4f05eaeb9646223c5b74febc9, with application-sanitized.log, http-smoke.log and the temporary Java client source. Application/database containers, artifact reader and internal network were removed; all fixture data was tmpfs-only and fixture credential variables cleared. Existing user containers were not touched. The original checkout still contains only its prior protected changes. Independent reviews and human review remain pending. Five trailing-whitespace warnings in inherited upstream docs/db/schema.sql were left unchanged; the integration does not execute that demo seed.

## Sources checked on 2026-10-28

- Spring Boot database initialization: https://docs.spring.io/spring-boot/how-to/data-initialization.html
- Docker build contexts and ignore rules: https://docs.docker.com/build/concepts/context/
- Render Docker deployments: https://render.com/docs/docker
- Render ports and service setup: https://render.com/docs/web-services
- Render health checks: https://render.com/docs/health-checks
# Independent acceptance review of the local FCM candidate

The independent SQL/configuration review found no required correction in its bounded scope. The acceptance review corroborated the five JUnit XML reports (87 tests, no failures, errors or skips), the migration hash and the recorded local HTTP results. Neither review executed the validation again or substitutes for human review.

The acceptance review identified a weak assertion in the temporary HTTP client: two missing notification identifiers could compare equal after numeric conversion. The client now requires integral, positive identifiers in both responses before comparing them. This strengthened client has not been rerun; the existing validation report and client hash describe the earlier run, not the revised client. The earlier replay assertion therefore remains subject to that coverage limitation. The single-account smoke test also does not establish cross-owner isolation.

Remote schema migration, deployment, remote HTTP writes and enabling FCM remain pending and require their own explicit authorization. Local acceptance does not close frontend issue 49.

## Neon FCM incremental migration applied: 2026-10-28

The user explicitly authorized only this migration after receiving the project, branch, database, command, script hash, effects and risk. Destination was revalidated immediately before execution with authenticated read-only Neon checks: project patient-boat-45032812 (glifo-app), organization org-lucky-paper-59961031, default production branch br-plain-leaf-b5041ubn, single read_write direct endpoint ep-broad-glitter-b5crra22, database neondb owned by neondb_owner, roles neondb_owner and glifo_runtime. A read-only transaction confirmed PostgreSQL 18.6, 21 public tables, the original 5 device and 6 notification columns and zero rows in both tables.

The executable file SHA256 was rechecked as 769c237a84b9f936eb191106f88e9ee428d6e6db51ac3788bb2fed4eb7a7dd96 immediately before a single psql 18 execution from 18:48:25 to 18:48:29 UTC. It exited 0 and returned COMMIT. It was not retried. The client used verify-full with sslrootcert=system and the previously recorded public CA bundle mounted read-only; owner credentials were supplied through a private temporary env file that was deleted afterwards and never printed.

Read-only postchecks confirmed 8 device columns, 13 notification columns, nullable varchar(512) fcm_token, the new uq_devices_user_installation, uq_notifications_idempotency, ck_notifications_status and fk_notifications_device constraints, idx_devices_user_active and idx_notifications_device_status, zero rows in devices, notifications and users, 21 public tables, 3 application roles, 16 privileges, and 75 glifo_runtime table grants including DML on both affected tables.

Sanitized local evidence: migration.log SHA256 8af0db42d15fe791f0dd277010c1844825027877be333fac8cbc521c55427b6a and postcheck.log SHA256 4327dfb838731b42cc1a9bcd6e997ecd3b9563426891c93b6f0084ce15589ee8 in the glifo49-neon-mig-EdOe6E temporary directory; not versioned. The preexisting glifo-postgres container was not touched. The application was not started against Neon. Remote HTTP writes, enabling FCM, deployment, commit, push and GitHub mutations remain separately gated, and human review is pending.

## Application validation against Neon: 2026-10-28

After the migration, image glifo-api:issue49-fcm-local (sha256:18ca1ab8a3aa7e797825d9c4ea0b6657c021f31d606ce5a7950bf021f9944848) started locally against the Neon direct endpoint as glifo_runtime with JDBC verify-full, the cloud profile, Hibernate validate, SQL initialization disabled, FCM disabled, 512 MiB and port published only on 127.0.0.1. Schema validation passed and the application started in 20.06 seconds without OOM. GET /api/v1/health returned 200 UP; anonymous GET /api/v1/courses and /api/v1/devices returned 401. No HTTP writes were made. The container, temporary env file and private password file were removed; the JWT secret was random and discarded. This is not a Render deployment.


## Pending Neon incremental migrations before starting Render (#46 and #43)

Since `docs/db/cloud-bootstrap.psql` and `docs/db/049-fcm-incremental.psql` are already applied in Neon (`neondb`), David must apply the remaining incremental migrations as `neondb_owner` in the following exact order **before** starting the service on Render (so `spring.jpa.hibernate.ddl-auto=validate` and runtime `UNCERTAIN` coverage writes succeed):

1. **`docs/db/046-ai-calls-incremental.psql`** — Updates `public.ai_calls` (nullable `level`, `input_tokens`, `output_tokens`, `estimated_cost numeric(12,8)`, plus `model`, `success`, `output_accepted`, `error_code`, `error_message` and `ck_ai_calls_outcome`), creates `public.ai_avoided_calls` (`idx_ai_avoided_calls_user_created`) and grants DML/sequence privileges to `glifo_runtime`.
2. **`docs/db/043-topic-coverage-uncertain.psql`** — Replaces constraint `ck_topic_coverage_state` on `public.topic_coverage` to include `'UNCERTAIN'` (`CHECK (state IN ('SOLID', 'PARTIAL', 'ABSENT', 'UNCERTAIN'))`).
