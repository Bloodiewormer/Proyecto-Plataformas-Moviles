package cr.ac.una.glifo.course

import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.user.repository.UserRepository
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
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Pruebas HTTP de orden jerárquico en GET /api/v1/courses/{id}/topics, /syllabus y /coverage
 * sobre PostgreSQL ("glifo_test").
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SyllabusTopicOrderHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var userRepository: UserRepository

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

    @Test
    fun `GET topics, syllabus y coverage devuelven cada padre seguido de sus hijos aunque compartan order_index`() {
        val teacher = userRepository.findByEmail("docente@una.ac.cr")!!
        val course = courseRepository.save(
            Course().apply {
                owner = teacher
                code = "MAT-202"
                name = "Cálculo II"
                term = "II-2026"
                joinCode = "ORD-${UUID.randomUUID().toString().take(6).uppercase()}"
            }
        )
        val teacherAuth = bearer("docente@una.ac.cr", "Docente123")

        val publishPayload = """
            {
              "source_uri": "programa-mat202.pdf",
              "topics": [
                {"code": "U1", "title": "Integración", "order_index": 1},
                {"code": "U1.1", "title": "Antiderivadas", "order_index": 1, "parent_code": "U1"},
                {"code": "U1.2", "title": "Sustitución", "order_index": 2, "parent_code": "U1"},
                {"code": "U1.3", "title": "Por partes", "order_index": 3, "parent_code": "U1"},
                {"code": "U2", "title": "Series", "order_index": 2},
                {"code": "U2.1", "title": "Sucesiones", "order_index": 1, "parent_code": "U2"},
                {"code": "U2.2", "title": "Criterios", "order_index": 2, "parent_code": "U2"},
                {"code": "U2.3", "title": "Potencias", "order_index": 3, "parent_code": "U2"}
              ]
            }
        """.trimIndent()

        val expectedOrder = listOf("U1", "U1.1", "U1.2", "U1.3", "U2", "U2.1", "U2.2", "U2.3")

        val publishBody = mvc.post("/api/v1/courses/${course.id}/syllabus") {
            header("Authorization", teacherAuth)
            contentType = MediaType.APPLICATION_JSON
            content = publishPayload
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        assertEquals(expectedOrder, extractCodes(json.readTree(publishBody)["data"]))

        val topicsBody = mvc.get("/api/v1/courses/${course.id}/topics") {
            header("Authorization", teacherAuth)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertEquals(expectedOrder, extractCodes(json.readTree(topicsBody)["data"]))

        val syllabusBody = mvc.get("/api/v1/courses/${course.id}/syllabus") {
            header("Authorization", teacherAuth)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertEquals(expectedOrder, extractCodes(json.readTree(syllabusBody)["data"]["topics"]))

        val coverageBody = mvc.get("/api/v1/courses/${course.id}/coverage") {
            header("Authorization", teacherAuth)
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertEquals(expectedOrder, extractCodes(json.readTree(coverageBody)["data"]["topics"]))
    }

    private fun extractCodes(arrayNode: tools.jackson.databind.JsonNode): List<String> =
        (0 until arrayNode.size()).map { idx -> arrayNode[idx]["code"].asString() }
}
