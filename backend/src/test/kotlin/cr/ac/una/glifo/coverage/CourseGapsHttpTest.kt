package cr.ac.una.glifo.coverage

import cr.ac.una.glifo.course.repository.CourseRepository
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

/**
 * GET /api/v1/courses/{id}/coverage/gaps: brechas del grupo para el docente dueño.
 * Usa el curso demo MAT-101 de import.sql (un estudiante matriculado) sobre "glifo_test".
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CourseGapsHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    // Login con una cuenta demo y devuelve el encabezado Authorization
    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun gapsUrl() = "/api/v1/courses/${courseRepository.findByJoinCode("GLF-CALC1")!!.id}/coverage/gaps"

    @Test
    fun `el docente dueño ve las brechas con los estudiantes matriculados`() {
        mvc.get(gapsUrl()) {
            header("Authorization", bearer("docente@una.ac.cr", "Docente123"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.enrolled_students") { value(1) }
            jsonPath("$.data.topics") { isArray() }
            jsonPath("$.data.topics[0].students_with_gap") { exists() }
            jsonPath("$.data.topics[0].average_pct") { exists() }
        }
    }

    @Test
    fun `el estudiante no puede ver las brechas del grupo`() {
        mvc.get(gapsUrl()) {
            header("Authorization", bearer("estudiante@est.una.ac.cr", "Estudiante123"))
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `sin token responde 401`() {
        mvc.get(gapsUrl()).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `curso inexistente responde 404`() {
        mvc.get("/api/v1/courses/999999/coverage/gaps") {
            header("Authorization", bearer("docente@una.ac.cr", "Docente123"))
        }.andExpect { status { isNotFound() } }
    }
}
