package cr.ac.una.glifo.coverage

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.sql.Timestamp
import kotlin.test.assertEquals

/**
 * Integración de POST /courses/{id}/coverage/evaluate con seguridad, Hibernate y PostgreSQL sobre
 * los datos de import.sql, sin transacción de prueba y con open-in-view desactivado, para validar
 * las fronteras transaccionales reales. Fuerza la api key vacía: nunca llama a OpenAI. Al terminar
 * borra el apunte creado y restaura la cobertura sembrada del estudiante.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["openai.api-key=", "spring.jpa.open-in-view=false"])
class CoverageEvaluationIntegrationTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var jdbc: JdbcTemplate

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()
    private var userId = 0L
    private var courseId = 0L
    private lateinit var seeded: List<Map<String, Any?>>

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
        userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'estudiante@est.una.ac.cr'", Long::class.java)!!
        courseId = jdbc.queryForObject("SELECT id FROM courses WHERE join_code = 'GLF-CALC1'", Long::class.java)!!
        seeded = jdbc.queryForList("SELECT syllabus_topic_id, state, score, updated_at FROM topic_coverage WHERE user_id = ?", userId)
    }

    @AfterEach
    fun restore() {
        if (userId == 0L || !::seeded.isInitialized) return
        jdbc.update("DELETE FROM notes WHERE title = 'Prueba IA-04'")
        jdbc.update("DELETE FROM topic_coverage WHERE user_id = ?", userId)
        seeded.forEach {
            jdbc.update(
                "INSERT INTO topic_coverage (user_id, syllabus_topic_id, state, score, updated_at) VALUES (?, ?, ?, ?, ?)",
                userId, it["syllabus_topic_id"], it["state"], it["score"], it["updated_at"]
            )
        }
    }

    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun coverageRows(): Map<String, Pair<String, Timestamp?>> = jdbc.query(
        "SELECT t.code, tc.state, tc.updated_at FROM topic_coverage tc JOIN syllabus_topics t ON t.id = tc.syllabus_topic_id WHERE tc.user_id = ? AND t.course_id = ?",
        { rs, _ -> rs.getString(1) to (rs.getString(2) to rs.getTimestamp(3)) }, userId, courseId
    ).toMap()

    @Test
    fun `evaluar inserta lo resuelto por HTTP, no inserta ABSENT y repetirlo no cambia updated_at`() {
        jdbc.update("DELETE FROM topic_coverage WHERE user_id = ?", userId)
        jdbc.update(
            """INSERT INTO notes (user_id, course_id, syllabus_topic_id, class_date, title, status, content, content_generated_at, created_at)
               SELECT ?, ?, t.id, DATE '2026-10-20', 'Prueba IA-04', 'READY', CAST(? AS jsonb), now(), now() FROM syllabus_topics t WHERE t.course_id = ? AND t.code = 'U1.1'""",
            userId, courseId,
            """{"schemaVersion": 1, "blocks": [{"type": "PARAGRAPH", "spans": [{"text": "El límite de una función continua en un punto"}]}]}""",
            courseId
        )
        val token = bearer("estudiante@est.una.ac.cr", "Estudiante123")

        val body = mvc.post("/api/v1/courses/$courseId/coverage/evaluate") { header("Authorization", token) }
            .andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        val first = coverageRows()
        mvc.post("/api/v1/courses/$courseId/coverage/evaluate") { header("Authorization", token) }
            .andExpect { status { isOk() } }

        assertEquals(false, json.readTree(body)["meta"]["ai_called"].asBoolean())
        assertEquals("SOLID", first.getValue("U1.1").first)
        assertEquals(false, "U2" in first.keys)
        assertEquals(first, coverageRows())
    }

    @Test
    fun `sin token responde 401 y un admin sin COVERAGE_READ_OWN responde 403`() {
        mvc.post("/api/v1/courses/$courseId/coverage/evaluate").andExpect { status { isUnauthorized() } }
        mvc.post("/api/v1/courses/$courseId/coverage/evaluate") { header("Authorization", bearer("admin@una.ac.cr", "Admin1234")) }
            .andExpect { status { isForbidden() } }
    }
}
