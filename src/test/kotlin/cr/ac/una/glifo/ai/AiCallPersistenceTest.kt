package cr.ac.una.glifo.ai

import cr.ac.una.glifo.ai.ledger.AiCallContext
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.ledger.AiCallType
import cr.ac.una.glifo.ai.ledger.CostLedgerService
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.entity.AiCall
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Integración con PostgreSQL (perfil test, base glifo_test): ai_calls se guarda en su propia
 * transacción y sobrevive al rollback de la operación de negocio que hizo la llamada.
 *
 * A propósito NO es @Transactional: si la prueba entera corriera en una transacción que se
 * revierte al final, no se podría ver qué confirmó REQUIRES_NEW y qué no. Cada prueba borra
 * las filas que creó (las marca con un modelo único).
 */
@SpringBootTest
@ActiveProfiles("test")
class AiCallPersistenceTest {

    @Autowired lateinit var ledger: CostLedgerService
    @Autowired lateinit var aiCallRepository: AiCallRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var transactionManager: PlatformTransactionManager
    @Autowired lateinit var jdbc: JdbcTemplate

    private val marker = "itest-${UUID.randomUUID()}"
    // ai_avoided_calls no tiene una columna donde marcar la fila: se borra lo creado después de este id.
    private var avoidedBefore = 0L

    @BeforeEach
    fun rememberAvoided() {
        avoidedBefore = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM ai_avoided_calls", Long::class.java)!!
    }

    private fun studentId() = userRepository.findByEmail("estudiante@est.una.ac.cr")!!.id
    private fun courseId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id
    private fun context() = AiCallContext(AiCallOrigin(studentId(), courseId()), AiCallType.IA_02, level = null)

    private fun rowsWithModel(model: String): List<Map<String, Any?>> =
        jdbc.queryForList("SELECT * FROM ai_calls WHERE model = ?", model)

    @AfterEach
    fun cleanUp() {
        jdbc.update("DELETE FROM ai_calls WHERE model LIKE ?", "$marker%")
        jdbc.update("DELETE FROM ai_calls WHERE error_message = ?", marker)
        jdbc.update("DELETE FROM ai_avoided_calls WHERE id > ?", avoidedBefore)
    }

    @Test
    fun `una llamada evitada se guarda en ai_avoided_calls aunque se revierta la operacion y no toca ai_calls`() {
        val callsBefore = aiCallRepository.count()
        TransactionTemplate(transactionManager).executeWithoutResult { status ->
            ledger.avoided(AiCallContext(AiCallOrigin(studentId(), courseId()), AiCallType.OCR_M, level = "N1_5"))
            status.setRollbackOnly()
        }

        val row = jdbc.queryForList("SELECT * FROM ai_avoided_calls WHERE id > ?", avoidedBefore).single()
        assertEquals(studentId(), (row["user_id"] as Number).toLong())
        assertEquals(courseId(), (row["course_id"] as Number).toLong())
        assertEquals("OCR_M", row["call_type"])
        assertEquals("N1_5", row["level"])
        assertEquals(callsBefore, aiCallRepository.count())
    }

    @Test
    fun `si no se puede guardar la evitada la operacion sigue`() {
        // Usuario inexistente: la FK rechaza el INSERT y avoided() solo lo deja en el log.
        ledger.avoided(AiCallContext(AiCallOrigin(Long.MAX_VALUE, null), AiCallType.IA_01, level = null))

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ai_avoided_calls WHERE id > ?", Long::class.java, avoidedBefore))
    }

    @Test
    fun `el registro sobrevive al rollback de la operacion de negocio`() {
        val businessModel = "$marker-negocio"
        TransactionTemplate(transactionManager).executeWithoutResult { status ->
            ledger.track(context(), call = { LlmResult("{}", marker, inputTokens = 120, outputTokens = 80, latencyMs = 450L) }, accept = { })

            // Control: algo guardado en la transacción de negocio SÍ se revierte.
            aiCallRepository.save(AiCall().apply {
                user = userRepository.getReferenceById(studentId())
                callType = "IA_02"; model = businessModel; success = true; outputAccepted = true; latencyMs = 1
            })
            status.setRollbackOnly()
        }

        assertEquals(1, rowsWithModel(marker).size, "La fila de REQUIRES_NEW se confirmó por su cuenta")
        assertEquals(0, rowsWithModel(businessModel).size, "La transacción de negocio se revirtió")
    }

    @Test
    fun `se persisten contexto, modelo, resultado, consumo y latencia tal cual`() {
        ledger.track(context(), call = { LlmResult("{}", marker, inputTokens = 120, outputTokens = null, latencyMs = 450L) }, accept = { })

        val row = rowsWithModel(marker).single()
        assertEquals(studentId(), (row["user_id"] as Number).toLong())
        assertEquals(courseId(), (row["course_id"] as Number).toLong())
        assertEquals("IA_02", row["call_type"])
        assertNull(row["level"])
        assertEquals(120, row["input_tokens"])
        // Consumo de salida no informado: NULL, y por eso el costo también es desconocido.
        assertNull(row["output_tokens"])
        assertNull(row["estimated_cost"])
        assertEquals(450, row["latency_ms"])
        assertEquals(true, row["success"])
        assertEquals(true, row["output_accepted"])
        assertNull(row["error_code"])
    }

    @Test
    fun `una llamada fallida queda registrada aunque la excepcion revierta la transaccion de negocio`() {
        assertThrows<AiProviderException> {
            TransactionTemplate(transactionManager).executeWithoutResult {
                ledger.track<Unit>(context(), call = { throw AiProviderException(marker) }, accept = { })
            }
        }

        val row = jdbc.queryForList("SELECT * FROM ai_calls WHERE error_message = ?", marker).single()
        assertEquals(false, row["success"])
        assertNull(row["output_accepted"])
        assertNull(row["model"])
        assertEquals("PROVIDER_INVALID_RESPONSE", row["error_code"])
    }

    @Test
    fun `el costo se guarda con 8 decimales`() {
        val call = AiCall().apply {
            user = userRepository.getReferenceById(studentId())
            callType = "IA_01"; model = "$marker-costo"; success = true; outputAccepted = true; latencyMs = 1
            inputTokens = 1; outputTokens = 0; estimatedCost = BigDecimal("0.00000001")
        }
        aiCallRepository.saveAndFlush(call)

        val stored = jdbc.queryForObject("SELECT estimated_cost FROM ai_calls WHERE model = ?", BigDecimal::class.java, "$marker-costo")
        assertEquals(BigDecimal("0.00000001"), stored)
    }

    @Test
    fun `la migracion 046 lleva el ai_calls de la nube al esquema que valida Hibernate`() {
        // Esquema temporal con el ai_calls original de Neon (cloud-bootstrap.psql) y el cuerpo de
        // 046-ai-calls-incremental.psql encima; public.ai_calls y public.ai_avoided_calls son lo
        // que generan las entidades. Los GRANT van fuera del cuerpo: glifo_test no tiene glifo_runtime.
        val schema = "mig046_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val bootstrap = Files.readString(Path.of("docs/db/cloud-bootstrap.psql"))
        val baseline = Regex("""CREATE TABLE public\.ai_calls \(.*?\n\);""", RegexOption.DOT_MATCHES_ALL).find(bootstrap)!!.value
        val migration = Files.readString(Path.of("docs/db/046-ai-calls-incremental.psql"))
            .substringAfter("-- BEGIN MIGRATION BODY").substringAfter('\n').substringBefore("-- END MIGRATION BODY")

        fun columns(tableSchema: String, table: String = "ai_calls") = jdbc.queryForList(
            """
            SELECT column_name, data_type, character_maximum_length, numeric_precision, numeric_scale, is_nullable
            FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY column_name
            """.trimIndent(),
            tableSchema, table
        )
        fun constraints(table: String) = jdbc.queryForList(
            "SELECT conname || ': ' || pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid = ?::regclass ORDER BY conname",
            String::class.java, table
        )

        jdbc.execute("CREATE SCHEMA $schema")
        try {
            jdbc.execute(baseline.replace("public.ai_calls", "$schema.ai_calls"))
            jdbc.execute(
                migration.replace("public.ai_calls", "$schema.ai_calls")
                    .replace("public.ai_avoided_calls", "$schema.ai_avoided_calls")
            )

            assertEquals(columns("public"), columns(schema))
            assertEquals(columns("public", "ai_avoided_calls"), columns(schema, "ai_avoided_calls"))
            // Misma PK, CHECK y FK (con los nombres que les da Hibernate, ON DELETE incluido).
            assertEquals(constraints("public.ai_avoided_calls"), constraints("$schema.ai_avoided_calls"))
            val checks = jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE conrelid = '$schema.ai_calls'::regclass AND contype = 'c' ORDER BY conname",
                String::class.java
            )
            assertEquals(listOf("ck_ai_calls_level", "ck_ai_calls_outcome", "ck_ai_calls_type"), checks)
        } finally {
            jdbc.execute("DROP SCHEMA $schema CASCADE")
        }
    }

    @Test
    fun `el CHECK impide un fallo sin codigo`() {
        assertThrows<Exception> {
            aiCallRepository.saveAndFlush(AiCall().apply {
                user = userRepository.getReferenceById(studentId())
                callType = "IA_02"; model = "$marker-check"; success = false; latencyMs = 1
            })
        }
    }
}
