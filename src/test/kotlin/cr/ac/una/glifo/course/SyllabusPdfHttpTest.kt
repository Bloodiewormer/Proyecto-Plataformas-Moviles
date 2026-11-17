package cr.ac.una.glifo.course

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pruebas HTTP de POST /api/v1/courses/{id}/syllabus/pdf y GET /api/v1/courses/{id}/syllabus
 * sobre PostgreSQL ("glifo_test") con LlmClient simulado (sin llamadas reales a OpenAI).
 */
@SpringBootTest
@ActiveProfiles("test")
class SyllabusPdfHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var jdbc: JdbcTemplate

    @MockitoBean lateinit var llmClient: LlmClient

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()
    private val createdCourseIds = mutableListOf<Long>()

    @BeforeEach
    fun setUp() {
        reset(llmClient)
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @AfterEach
    fun cleanUp() {
        if (createdCourseIds.isNotEmpty()) {
            createdCourseIds.forEach { courseId ->
                jdbc.update("DELETE FROM ai_calls WHERE course_id = ?", courseId)
                jdbc.update("DELETE FROM courses WHERE id = ?", courseId)
            }
            createdCourseIds.clear()
        }
    }

    private fun bearer(email: String, password: String): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun teacherBearer() = bearer("docente@una.ac.cr", "Docente123")
    private fun studentBearer() = bearer("estudiante@est.una.ac.cr", "Estudiante123")

    private fun createOwnedCourse(): Course {
        val teacher = userRepository.findByEmail("docente@una.ac.cr")!!
        val course = courseRepository.save(
            Course().apply {
                owner = teacher
                code = "EIF411"
                name = "Plataformas Móviles"
                term = "II-2026"
                joinCode = "GLF-${UUID.randomUUID().toString().take(6).uppercase()}"
            }
        )
        createdCourseIds.add(course.id)
        return course
    }

    private fun buildPdfBytes(vararg lines: String): ByteArray =
        ByteArrayOutputStream().use { baos ->
            PDDocument().use { doc ->
                val page = PDPage()
                doc.addPage(page)
                if (lines.isNotEmpty()) {
                    PDPageContentStream(doc, page).use { cs ->
                        cs.beginText()
                        cs.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                        cs.newLineAtOffset(50f, 700f)
                        lines.forEachIndexed { idx, line ->
                            if (idx > 0) cs.newLineAtOffset(0f, -16f)
                            cs.showText(line)
                        }
                        cs.endText()
                    }
                }
                doc.save(baos)
            }
            baos.toByteArray()
        }

    private fun stubValidTree() {
        val treeJson = """
            {
              "units": [
                {
                  "code": "U1",
                  "title": "Arquitectura Android",
                  "order_index": 1,
                  "subtopics": [
                    {"code": "U1.1", "title": "Compose y estado", "order_index": 1, "subtopics": []}
                  ]
                },
                {
                  "code": "U2",
                  "title": "Persistencia local",
                  "order_index": 2,
                  "subtopics": []
                }
              ]
            }
        """.trimIndent()
        `when`(llmClient.completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", ""))).thenReturn(
            LlmResult(
                content = treeJson,
                model = "gpt-4o-mini",
                inputTokens = 420,
                outputTokens = 110,
                latencyMs = 310L
            )
        )
    }

    @Test
    fun `PDF valido devuelve 201, guarda temas, sube version y GET devuelve el silabo sin llamar a la IA`() {
        val course = createOwnedCourse()
        val pdfBytes = buildPdfBytes("Unidad 1: Arquitectura Android", "1.1 Compose y estado", "Unidad 2: Persistencia")
        val part = MockMultipartFile("file", "programa-eif411.pdf", "application/pdf", pdfBytes)
        stubValidTree()

        val teacherAuth = teacherBearer()
        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherAuth)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.course_id") { value(course.id) }
            jsonPath("$.data.file_name") { value("programa-eif411.pdf") }
            jsonPath("$.data.size_bytes") { value(pdfBytes.size) }
            jsonPath("$.data.parsed_at") { isString() }
            jsonPath("$.data.syllabus_version") { value(2) }
            jsonPath("$.data.topics.length()") { value(3) }
            jsonPath("$.data.topics[0].code") { value("U1") }
        }

        val updatedCourse = courseRepository.findById(course.id).orElseThrow()
        assertEquals(2, updatedCourse.syllabusVersion)
        assertNotNull(updatedCourse.syllabusParsedAt)
        assertEquals("programa-eif411.pdf", updatedCourse.syllabusSourceUri)

        // GET no debe llamar al LLM y devuelve size_bytes = null
        mvc.get("/api/v1/courses/${course.id}/syllabus") {
            header("Authorization", teacherAuth)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.course_id") { value(course.id) }
            jsonPath("$.data.file_name") { value("programa-eif411.pdf") }
            jsonPath("$.data.size_bytes") { value(nullValue()) }
            jsonPath("$.data.parsed_at") { isString() }
            jsonPath("$.data.syllabus_version") { value(2) }
            jsonPath("$.data.topics.length()") { value(3) }
        }

        verify(llmClient, times(1)).completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", ""))
    }

    @Test
    fun `curso con temario ya publicado responde 409 sin llamar a la IA`() {
        val demoCourseId = courseRepository.findByJoinCode("GLF-CALC1")!!.id
        val part = MockMultipartFile("file", "programa.pdf", "application/pdf", buildPdfBytes("Unidad 1: Limites"))

        mvc.multipart("/api/v1/courses/$demoCourseId/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect { status { isConflict() } }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `archivo que no es PDF responde 400 sin llamar a la IA`() {
        val course = createOwnedCourse()
        val part = MockMultipartFile("file", "notas.txt", "text/plain", "esto no es un pdf".toByteArray())

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `PDF sin texto extraible responde 400 sin llamar a la IA`() {
        val course = createOwnedCourse()
        val blankPdf = buildPdfBytes()
        val part = MockMultipartFile("file", "escaneado.pdf", "application/pdf", blankPdf)

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `estudiante intentando subir silabo responde 403`() {
        val demoCourseId = courseRepository.findByJoinCode("GLF-CALC1")!!.id
        val part = MockMultipartFile("file", "silabo.pdf", "application/pdf", buildPdfBytes("Tema 1"))

        mvc.multipart("/api/v1/courses/$demoCourseId/syllabus/pdf") {
            file(part)
            header("Authorization", studentBearer())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("FORBIDDEN") }
        }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `sin token responde 401 en POST y GET`() {
        val demoCourseId = courseRepository.findByJoinCode("GLF-CALC1")!!.id
        val part = MockMultipartFile("file", "silabo.pdf", "application/pdf", buildPdfBytes("Tema 1"))

        mvc.multipart("/api/v1/courses/$demoCourseId/syllabus/pdf") {
            file(part)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }

        mvc.get("/api/v1/courses/$demoCourseId/syllabus").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `curso inexistente responde 404 en POST y GET`() {
        val part = MockMultipartFile("file", "silabo.pdf", "application/pdf", buildPdfBytes("Tema 1"))

        mvc.multipart("/api/v1/courses/999999/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isNotFound() }
        }

        mvc.get("/api/v1/courses/999999/syllabus") {
            header("Authorization", studentBearer())
        }.andExpect {
            status { isNotFound() }
        }

        verifyNoInteractions(llmClient)
    }

    @Test
    fun `fallo de proveedor IA responde 502 y sin API key responde 503`() {
        val course = createOwnedCourse()
        val part = MockMultipartFile("file", "silabo.pdf", "application/pdf", buildPdfBytes("Unidad 1: Tema"))

        `when`(llmClient.completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", "")))
            .thenThrow(AiProviderException("Error de OpenAI"))

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isBadGateway() }
            jsonPath("$.code") { value("AI_PROVIDER_ERROR") }
        }

        reset(llmClient)
        `when`(llmClient.completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", "")))
            .thenThrow(AiProviderException.notConfigured())

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("AI_NOT_CONFIGURED") }
        }
    }

    @Test
    fun `acepta application octet-stream con firma PDF y respuesta inutilizable de IA da 400`() {
        val course = createOwnedCourse()
        val pdfBytes = buildPdfBytes("Unidad 1: Conjuntos")
        stubValidTree()

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(MockMultipartFile("file", "silabo.pdf", "application/octet-stream", pdfBytes))
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.syllabus_version") { value(2) }
        }

        `when`(llmClient.completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", ""))).thenReturn(
            LlmResult("""{"units": []}""", "gpt-4o-mini", 100, 10, 100L)
        )

        val unpublishedCourse = createOwnedCourse()
        mvc.multipart("/api/v1/courses/${unpublishedCourse.id}/syllabus/pdf") {
            file(MockMultipartFile("file", "silabo2.pdf", "application/pdf", pdfBytes))
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
    }

    @Test
    fun `PDF mayor a 10MB responde 413 con mensaje claro`() {
        val course = createOwnedCourse()
        val oversized = ByteArray(10 * 1024 * 1024 + 1).apply {
            "%PDF-1.4".toByteArray(Charsets.US_ASCII).copyInto(this)
        }
        val part = MockMultipartFile("file", "enorme.pdf", "application/pdf", oversized)

        mvc.multipart("/api/v1/courses/${course.id}/syllabus/pdf") {
            file(part)
            header("Authorization", teacherBearer())
        }.andExpect {
            status { isContentTooLarge() }
            jsonPath("$.code") { value("PAYLOAD_TOO_LARGE") }
            jsonPath("$.message") { isString() }
        }

        verifyNoInteractions(llmClient)
    }
}
