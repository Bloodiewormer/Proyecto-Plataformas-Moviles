package cr.ac.una.glifo.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@EnabledIfEnvironmentVariable(named = "GLIFO_CLOUD_MIGRATION_DOCKER_TEST", matches = "true")
class CloudFcmMigrationTest {
    private val containerName = "glifo49-fcm-test-" + UUID.randomUUID().toString().take(8)
    private val runtimePassword = UUID.randomUUID().toString() + UUID.randomUUID().toString()
    private val migration = Files.readString(Path.of("docs/db/049-fcm-incremental.psql"))

    private data class CommandResult(val exitCode: Int, val output: String)

    private fun docker(
        vararg arguments: String,
        input: String = "",
        environment: Map<String, String> = emptyMap()
    ): CommandResult {
        val builder = ProcessBuilder(listOf("docker") + arguments).redirectErrorStream(true)
        builder.environment().putAll(environment)
        val process = builder.start()
        val output = CompletableFuture.supplyAsync { process.inputStream.bufferedReader().readText() }
        process.outputStream.use { stream -> stream.write(input.toByteArray(Charsets.UTF_8)) }
        if (!process.waitFor(90, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Disposable PostgreSQL command timed out")
        }
        return CommandResult(process.exitValue(), output.get(5, TimeUnit.SECONDS).replace(runtimePassword, "[REDACTED]"))
    }

    private fun sql(
        statement: String,
        role: String = "neondb_owner",
        database: String = "neondb"
    ): CommandResult = docker(
        "exec", "-i", "--env", "GLIFO_RUNTIME_PASSWORD", containerName,
        "psql", "-X", "--no-password", "-qAt", "--set=ON_ERROR_STOP=on", "-U", role, "-d", database, "--file=-",
        input = statement,
        environment = mapOf("GLIFO_RUNTIME_PASSWORD" to runtimePassword)
    )

    private fun successful(statement: String, role: String = "neondb_owner", database: String = "neondb"): String {
        val result = sql(statement, role, database)
        assertEquals(0, result.exitCode, result.output)
        return result.output.trim()
    }

    private fun awaitPostgres() {
        repeat(100) {
            val result = docker("exec", containerName, "pg_isready", "-h", "127.0.0.1", "-U", "fixture_admin", "-d", "postgres")
            if (result.exitCode == 0) return
            Thread.sleep(100)
        }
        error("Disposable PostgreSQL did not become ready")
    }

    @BeforeEach
    fun initializeDisposableBaseline() {
        val started = docker(
            "run", "-d", "--pull=never", "--network", "none", "--memory", "256m",
            "--tmpfs", "/var/lib/postgresql", "--env", "POSTGRES_HOST_AUTH_METHOD=trust",
            "--env", "POSTGRES_USER=fixture_admin", "--name", containerName, "postgres:18"
        )
        assertEquals(0, started.exitCode, started.output)
        awaitPostgres()
        successful(
            "CREATE ROLE neondb_owner LOGIN NOSUPERUSER CREATEDB CREATEROLE; CREATE DATABASE neondb OWNER neondb_owner;",
            role = "fixture_admin", database = "postgres"
        )
        successful(Files.readString(Path.of("docs/db/cloud-bootstrap.psql")))
        assertEquals("f", successful("SELECT rolsuper FROM pg_roles WHERE rolname=current_user;"))
    }

    @AfterEach
    fun removeOnlyOwnedDisposableContainer() {
        val removed = docker("rm", "-f", containerName)
        assertEquals(0, removed.exitCode, removed.output)
    }

    private fun fingerprint(): String = successful("""
        SELECT string_agg(signature, E'\n' ORDER BY signature) FROM (
            SELECT 'column:' || relation.relname || ':' || attribute.attname || ':' ||
                format_type(attribute.atttypid, attribute.atttypmod) || ':' || attribute.attnotnull AS signature
            FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
            JOIN pg_attribute attribute ON attribute.attrelid=relation.oid
            WHERE namespace.nspname='public' AND relation.relname IN ('devices','notifications')
                AND attribute.attnum>0 AND NOT attribute.attisdropped
            UNION ALL
            SELECT 'constraint:' || relation.relname || ':' || constraint_record.conname || ':' ||
                pg_get_constraintdef(constraint_record.oid)
            FROM pg_constraint constraint_record JOIN pg_class relation ON relation.oid=constraint_record.conrelid
            JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
            WHERE namespace.nspname='public' AND relation.relname IN ('devices','notifications')
            UNION ALL
            SELECT 'index:' || indexname || ':' || indexdef FROM pg_indexes
            WHERE schemaname='public' AND tablename IN ('devices','notifications')
        ) objects;
    """.trimIndent())

    private fun seedUser() {
        successful("""
            INSERT INTO public.users (created_at,email,is_active,password_hash,updated_at)
            VALUES (CURRENT_TIMESTAMP,'migration-fixture@example.invalid',true,'fixture-only-hash',CURRENT_TIMESTAMP);
        """.trimIndent())
    }

    @Test
    fun migratesEmptyBaselineWithoutChangingCatalogsOrRuntimeGrants() {
        successful(migration)
        assertEquals("8|13|512|YES", successful("""
            SELECT (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='devices'),
                (SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='notifications'),
                character_maximum_length,is_nullable FROM information_schema.columns
            WHERE table_schema='public' AND table_name='devices' AND column_name='fcm_token';
        """.trimIndent()))
        assertEquals("3|16|30|0|0|0", successful("""
            SELECT (SELECT count(*) FROM public.roles),(SELECT count(*) FROM public.privileges),
                (SELECT count(*) FROM public.role_privileges),(SELECT count(*) FROM public.users),
                (SELECT count(*) FROM public.devices),(SELECT count(*) FROM public.notifications);
        """.trimIndent()))
        assertEquals("4|2", successful("""
            SELECT (SELECT count(*) FROM pg_constraint WHERE conrelid IN ('public.devices'::regclass,'public.notifications'::regclass)
                AND conname IN ('uq_devices_user_installation','uq_notifications_idempotency','ck_notifications_status','fk_notifications_device')),
                (SELECT count(*) FROM pg_indexes WHERE schemaname='public'
                AND indexname IN ('idx_devices_user_active','idx_notifications_device_status'));
        """.trimIndent()))
        assertEquals("t|t|f|f|f", successful("""
            SELECT has_table_privilege(current_user,'public.devices','INSERT'),
                has_table_privilege(current_user,'public.notifications','UPDATE'),
                has_table_privilege(current_user,'public.roles','INSERT'),
                has_schema_privilege(current_user,'public','CREATE'),
                has_table_privilege(current_user,'public.devices','TRUNCATE');
        """.trimIndent(), role = "glifo_runtime"))
    }

    @Test
    fun refusesPopulatedDevicesWithoutChangingRowsOrSchema() {
        seedUser()
        successful("""
            INSERT INTO public.devices (fcm_token,platform,registered_at,user_id)
            SELECT 'fixture-device-token','ANDROID',CURRENT_TIMESTAMP,id FROM public.users;
        """.trimIndent())
        val before = fingerprint()
        val result = sql(migration)
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("requires empty devices and notifications"), result.output)
        assertEquals(before, fingerprint())
        assertEquals("fixture-device-token", successful("SELECT fcm_token FROM public.devices;"))
    }

    @Test
    fun refusesPopulatedNotificationsWithoutChangingRowsOrSchema() {
        seedUser()
        successful("INSERT INTO public.notifications (kind,payload,user_id) SELECT 'SYNC_COMPLETE','{}'::jsonb,id FROM public.users;")
        val before = fingerprint()
        val result = sql(migration)
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("requires empty devices and notifications"), result.output)
        assertEquals(before, fingerprint())
        assertEquals("1", successful("SELECT count(*) FROM public.notifications;"))
    }

    @Test
    fun refusesReplayWithoutChangingMigratedSchema() {
        successful(migration)
        val before = fingerprint()
        val result = sql(migration)
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("partial or repeated migrations are refused"), result.output)
        assertEquals(before, fingerprint())
    }

    @Test
    fun refusesWrongRoleEvenWhenItIsSuperuser() {
        val before = fingerprint()
        val result = sql(migration, role = "fixture_admin")
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("Expected database neondb and initialization role"), result.output)
        assertEquals(before, fingerprint())
    }

    @Test
    fun refusesWrongDatabaseBeforeSchemaChanges() {
        val before = fingerprint()
        val result = sql(migration, database = "postgres")
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("Expected database neondb and initialization role"), result.output)
        assertEquals(before, fingerprint())
    }

    @Test
    fun rollsBackEverySchemaChangeAfterLateFailure() {
        val before = fingerprint()
        val result = sql(migration.replace("COMMIT;", "SELECT 1 / 0;\nCOMMIT;"))
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("division by zero"), result.output)
        assertEquals(before, fingerprint())
        assertEquals("3|16|30", successful("SELECT (SELECT count(*) FROM public.roles),(SELECT count(*) FROM public.privileges),(SELECT count(*) FROM public.role_privileges);"))
    }

    @Test
    fun refusesUnexpectedBaselineTokenType() {
        successful("ALTER TABLE public.devices ALTER COLUMN fcm_token TYPE character varying(300);")
        val before = fingerprint()
        val result = sql(migration)
        assertNotEquals(0, result.exitCode)
        assertTrue(result.output.contains("original non-null varchar(255)"), result.output)
        assertEquals(before, fingerprint())
    }
}
