package cr.ac.una.glifo.usage

import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.ops.entity.AiAvoidedCall
import cr.ac.una.glifo.ops.entity.AiCall
import cr.ac.una.glifo.ops.repository.AiAvoidedCallRepository
import cr.ac.una.glifo.ops.repository.AiCallRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal

/**
 * Endpoints de uso de IA (#46) sobre la aplicación completa: JWT, @PreAuthorize, servicio y
 * PostgreSQL "glifo_test". Cada test corre en una transacción que se revierte al terminar,
 * así que las filas de ai_calls que agrega no quedan en la base.
 *
 * import.sql siembra una llamada IA_00 del estudiante en el curso demo (1850/120 tokens,
 * 0.000350 USD); las pruebas suman filas propias encima y miran la diferencia.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiUsageWebTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var aiCallRepository: AiCallRepository
    @Autowired lateinit var aiAvoidedCallRepository: AiAvoidedCallRepository

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun student() = bearer("estudiante@est.una.ac.cr", "Estudiante123")
    private fun teacher() = bearer("docente@una.ac.cr", "Docente123")
    private fun admin() = bearer("admin@una.ac.cr", "Admin1234")
    private fun calculusId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id

    private fun ownUsage(token: String, query: String = ""): tools.jackson.databind.JsonNode {
        val body = mvc.get("/api/v1/ai-usage/me$query") { header("Authorization", token) }
            .andExpect { status { isOk() } }.andReturn().response.contentAsString
        return json.readTree(body)["data"]
    }

    private fun insertCall(email: String, callType: String, withCourse: Boolean, success: Boolean, cost: BigDecimal?) {
        aiCallRepository.saveAndFlush(AiCall().apply {
            user = userRepository.findByEmail(email)
            course = if (withCourse) courseRepository.findByJoinCode("GLF-CALC1") else null
            this.callType = callType
            this.success = success
            outputAccepted = if (success) true else null
            errorCode = if (success) null else "PROVIDER_HTTP_500"
            model = if (success) "gpt-4o-mini" else null
            inputTokens = if (success) 200 else null
            outputTokens = if (success) 100 else null
            estimatedCost = cost
            latencyMs = 400
        })
    }

    @Test
    fun `GET ai-usage me devuelve los totales del estudiante con lo sembrado en import sql`() {
        mvc.get("/api/v1/ai-usage/me") { header("Authorization", student()) }.andExpect {
            status { isOk() }
            jsonPath("$.data.scope") { value("USER") }
            jsonPath("$.data.course_id") { value(nullValue()) }
            jsonPath("$.data.currency") { value("USD") }
            jsonPath("$.data.totals.total_calls") { value(1) }
            jsonPath("$.data.totals.successful_calls") { value(1) }
            jsonPath("$.data.totals.input_tokens") { value(1850) }
            jsonPath("$.data.totals.output_tokens") { value(120) }
            jsonPath("$.data.totals.estimated_cost_usd") { value("0.00035000") }
            jsonPath("$.data.totals.avoided_calls") { value(0) }
            jsonPath("$.data.by_call_type[0].call_type") { value("IA_00") }
        }
    }

    private fun insertAvoided(email: String, callType: String, level: String?) {
        aiAvoidedCallRepository.saveAndFlush(AiAvoidedCall().apply {
            user = userRepository.findByEmail(email)
            course = courseRepository.findByJoinCode("GLF-CALC1")
            this.callType = callType
            this.level = level
        })
    }

    @Test
    fun `las llamadas evitadas salen en avoided_calls sin cambiar total_calls ni el costo`() {
        insertAvoided("estudiante@est.una.ac.cr", "OCR_M", "N1_5")
        insertAvoided("estudiante@est.una.ac.cr", "OCR_M", "N1_5")
        insertAvoided("docente@una.ac.cr", "IA_02", null)

        val own = ownUsage(student())
        assertEqualsLong(1, own["totals"]["total_calls"].asLong())
        assertEqualsLong(2, own["totals"]["avoided_calls"].asLong())
        kotlin.test.assertEquals("0.00035000", own["totals"]["estimated_cost_usd"].asString())
        val ocr = own["by_call_type"].first { it["call_type"].asString() == "OCR_M" }["usage"]
        assertEqualsLong(0, ocr["total_calls"].asLong())
        assertEqualsLong(2, ocr["avoided_calls"].asLong())
        kotlin.test.assertTrue(ocr["estimated_cost_usd"].isNull)

        mvc.get("/api/v1/courses/${calculusId()}/ai-usage") { header("Authorization", teacher()) }.andExpect {
            status { isOk() }
            jsonPath("$.data.totals.total_calls") { value(1) }
            jsonPath("$.data.totals.avoided_calls") { value(3) }
        }
    }

    @Test
    fun `el uso propio no incluye llamadas de otros usuarios`() {
        val before = ownUsage(student())["totals"]["total_calls"].asLong()
        insertCall("docente@una.ac.cr", "IA_02", withCourse = true, success = true, cost = BigDecimal("0.00100000"))

        assertEqualsLong(before, ownUsage(student())["totals"]["total_calls"].asLong())
        assertEqualsLong(1, ownUsage(teacher())["totals"]["total_calls"].asLong())
    }

    @Test
    fun `fallos y costo desconocido se cuentan aparte, sin sumarse como cero`() {
        insertCall("estudiante@est.una.ac.cr", "IA_02", withCourse = true, success = true, cost = null)
        insertCall("estudiante@est.una.ac.cr", "IA_02", withCourse = true, success = false, cost = null)

        val totals = ownUsage(student())["totals"]
        assertEqualsLong(3, totals["total_calls"].asLong())
        assertEqualsLong(2, totals["successful_calls"].asLong())
        assertEqualsLong(1, totals["failed_calls"].asLong())
        assertEqualsLong(1, totals["unknown_token_calls"].asLong())
        assertEqualsLong(2, totals["unknown_cost_calls"].asLong())
        // Solo cuenta el costo conocido de la fila sembrada.
        kotlin.test.assertEquals("0.00035000", totals["estimated_cost_usd"].asString())
    }

    @Test
    fun `courseId filtra el uso propio y deja afuera las llamadas sin curso`() {
        insertCall("estudiante@est.una.ac.cr", "IA_01", withCourse = false, success = true, cost = BigDecimal("0.00010000"))

        assertEqualsLong(2, ownUsage(student())["totals"]["total_calls"].asLong())
        val filtered = ownUsage(student(), "?courseId=${calculusId()}")
        assertEqualsLong(calculusId(), filtered["course_id"].asLong())
        assertEqualsLong(1, filtered["totals"]["total_calls"].asLong())
    }

    @Test
    fun `el docente duenno ve el uso agregado de todo el curso`() {
        insertCall("docente@una.ac.cr", "IA_02", withCourse = true, success = true, cost = BigDecimal("0.00100000"))

        mvc.get("/api/v1/courses/${calculusId()}/ai-usage") { header("Authorization", teacher()) }.andExpect {
            status { isOk() }
            jsonPath("$.data.scope") { value("COURSE") }
            jsonPath("$.data.course_id") { value(calculusId()) }
            jsonPath("$.data.totals.total_calls") { value(2) }
            jsonPath("$.data.totals.estimated_cost_usd") { value("0.00135000") }
            jsonPath("$.data.by_call_type.length()") { value(2) }
            // Solo agregados: ninguna fila trae usuario ni mensajes de error.
            jsonPath("$.data.by_call_type[0].user_id") { doesNotExist() }
            jsonPath("$.data.by_call_type[0].usage.error_message") { doesNotExist() }
        }
    }

    @Test
    fun `un estudiante no puede ver el uso del curso - 403`() {
        mvc.get("/api/v1/courses/${calculusId()}/ai-usage") { header("Authorization", student()) }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `con USAGE_READ_COURSE pero sin ser el duenno del curso - 403`() {
        // El admin tiene USAGE_READ_COURSE pero el curso demo es del docente.
        mvc.get("/api/v1/courses/${calculusId()}/ai-usage") { header("Authorization", admin()) }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }
    }

    @Test
    fun `sin USAGE_READ_OWN no hay uso propio - 403`() {
        mvc.get("/api/v1/ai-usage/me") { header("Authorization", admin()) }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `curso inexistente - 404`() {
        mvc.get("/api/v1/courses/999999/ai-usage") { header("Authorization", teacher()) }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("NOT_FOUND") }
        }
    }

    @Test
    fun `courseId invalido - 400 y sin token - 401`() {
        mvc.get("/api/v1/ai-usage/me?courseId=0") { header("Authorization", student()) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
        mvc.get("/api/v1/ai-usage/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
    }

    private fun assertEqualsLong(expected: Long, actual: Long) = kotlin.test.assertEquals(expected, actual)
}
