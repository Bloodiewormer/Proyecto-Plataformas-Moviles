package cr.ac.una.glifo.study

import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.RoleRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

/**
 * Pruebas HTTP de generacion y consulta de material de estudio por usuario:
 * - Dos estudiantes del mismo curso reciben su propio material (aislamiento por usuario).
 * - regenerate=false reutiliza los items existentes del usuario sin llamar a la IA.
 * - regenerate=true reemplaza los items del usuario para los temas indicados.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudyItemHttpTest {

    @Autowired lateinit var context: WebApplicationContext
    @Autowired lateinit var courseRepository: CourseRepository
    @Autowired lateinit var topicRepository: SyllabusTopicRepository
    @Autowired lateinit var enrollmentRepository: EnrollmentRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var roleRepository: RoleRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    @Autowired lateinit var jdbc: org.springframework.jdbc.core.JdbcTemplate

    @MockitoBean lateinit var llmClient: LlmClient

    private var aiCallsBefore = 0L
    private var avoidedBefore = 0L

    private lateinit var mvc: MockMvc
    private val json = JsonMapper.builder().findAndAddModules().build()

    private val student1Email = "estudiante@est.una.ac.cr"
    private val student2Email = "estudiante2@est.una.ac.cr"
    private val defaultPassword = "Estudiante123"

    @org.junit.jupiter.api.AfterEach
    fun cleanUpAiLedger() {
        jdbc.update("DELETE FROM ai_calls WHERE id > ?", aiCallsBefore)
        jdbc.update("DELETE FROM ai_avoided_calls WHERE id > ?", avoidedBefore)
    }

    @BeforeEach
    fun setUp() {
        aiCallsBefore = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM ai_calls", Long::class.java) ?: 0L
        avoidedBefore = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM ai_avoided_calls", Long::class.java) ?: 0L
        reset(llmClient)
        mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()

        // Asegura que exista el segundo estudiante y este matriculado en GLF-CALC1
        if (userRepository.findByEmail(student2Email) == null) {
            val studentRole = roleRepository.findByName("ROLE_STUDENT")
                ?: throw IllegalStateException("ROLE_STUDENT no encontrado")
            val user2 = userRepository.save(
                User().apply {
                    email = student2Email
                    passwordHash = passwordEncoder.encode(defaultPassword)!!
                    firstName = "Carla"
                    lastName = "Solis"
                    isActive = true
                    roles.add(studentRole)
                }
            )
            val course = courseRepository.findByJoinCode("GLF-CALC1")!!
            enrollmentRepository.save(
                Enrollment().apply {
                    this.user = user2
                    this.course = course
                    this.status = "ACTIVE"
                    this.joinedAt = Instant.now()
                }
            )
        }
    }

    private fun bearer(email: String, password: String = defaultPassword): String {
        val body = mvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return "Bearer ${json.readTree(body)["token"].asString()}"
    }

    private fun courseId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id

    private fun firstTopicId(courseId: Long) =
        topicRepository.findByCourseIdOrderByOrderIndex(courseId).first().id

    private fun stubLlmBatch(topicId: Long, frontText: String) {
        val payload = """
            {
              "items": [
                {
                  "topicId": $topicId,
                  "kind": "FLASHCARD",
                  "front": "$frontText",
                  "back": "Dorso pedagogico",
                  "difficulty": "MEDIUM"
                }
              ]
            }
        """.trimIndent()
        `when`(llmClient.completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", "")))
            .thenReturn(
                LlmResult(
                    content = payload,
                    model = "gpt-4o-mini",
                    inputTokens = 100,
                    outputTokens = 120,
                    latencyMs = 250L
                )
            )
    }

    @Test
    fun `dos estudiantes del mismo curso reciben material propio y aislado`() {
        val cId = courseId()
        val tId = firstTopicId(cId)

        // 1. Estudiante 1 genera su material
        stubLlmBatch(tId, "Flashcard de Estudiante 1")
        mvc.post("/api/v1/courses/$cId/study-items/generate") {
            header("Authorization", bearer(student1Email))
            contentType = MediaType.APPLICATION_JSON
            content = """{"topic_ids":[$tId],"items_per_topic":1,"regenerate":false}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data", hasSize<Any>(1))
            jsonPath("$.data[0].payload.front", equalTo("Flashcard de Estudiante 1"))
        }

        // 2. Estudiante 2 genera su propio material para el mismo tema
        reset(llmClient)
        stubLlmBatch(tId, "Flashcard de Estudiante 2")
        mvc.post("/api/v1/courses/$cId/study-items/generate") {
            header("Authorization", bearer(student2Email))
            contentType = MediaType.APPLICATION_JSON
            content = """{"topic_ids":[$tId],"items_per_topic":1,"regenerate":false}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data", hasSize<Any>(1))
            jsonPath("$.data[0].payload.front", equalTo("Flashcard de Estudiante 2"))
        }

        // 3. Estudiante 1 solo ve sus flashcards
        mvc.get("/api/v1/courses/$cId/study-items") {
            header("Authorization", bearer(student1Email))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data", hasSize<Any>(1))
            jsonPath("$.data[0].payload.front", equalTo("Flashcard de Estudiante 1"))
        }

        // 4. Estudiante 2 solo ve las suyas
        mvc.get("/api/v1/courses/$cId/study-items") {
            header("Authorization", bearer(student2Email))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data", hasSize<Any>(1))
            jsonPath("$.data[0].payload.front", equalTo("Flashcard de Estudiante 2"))
        }
    }

    @Test
    fun `regenerate false reutiliza material existente y regenerate true reemplaza`() {
        val cId = courseId()
        val tId = firstTopicId(cId)

        // Generacion inicial para Estudiante 1
        stubLlmBatch(tId, "Version Inicial")
        mvc.post("/api/v1/courses/$cId/study-items/generate") {
            header("Authorization", bearer(student1Email))
            contentType = MediaType.APPLICATION_JSON
            content = """{"topic_ids":[$tId],"items_per_topic":1,"regenerate":false}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.meta.generatedTopics", equalTo(1))
        }

        // regenerate=false: reutiliza, responde 200 OK y no invoca a la IA
        reset(llmClient)
        mvc.post("/api/v1/courses/$cId/study-items/generate") {
            header("Authorization", bearer(student1Email))
            contentType = MediaType.APPLICATION_JSON
            content = """{"topic_ids":[$tId],"items_per_topic":1,"regenerate":false}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.meta.reusedTopics", equalTo(1))
            jsonPath("$.meta.generatedTopics", equalTo(0))
            jsonPath("$.data[0].payload.front", equalTo("Version Inicial"))
        }
        verifyNoInteractions(llmClient)

        // regenerate=true: reemplaza el material con la nueva version
        stubLlmBatch(tId, "Version Regenerada")
        mvc.post("/api/v1/courses/$cId/study-items/generate") {
            header("Authorization", bearer(student1Email))
            contentType = MediaType.APPLICATION_JSON
            content = """{"topic_ids":[$tId],"items_per_topic":1,"regenerate":true}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.meta.generatedTopics", equalTo(1))
            jsonPath("$.meta.reusedTopics", equalTo(0))
            jsonPath("$.data[0].payload.front", equalTo("Version Regenerada"))
        }
        verify(llmClient, times(1)).completeJson(org.mockito.ArgumentMatchers.any() ?: LlmRequest("", ""))

        // Consulta GET confirma que la version vieja fue reemplazada
        mvc.get("/api/v1/courses/$cId/study-items") {
            header("Authorization", bearer(student1Email))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data", hasSize<Any>(1))
            jsonPath("$.data[0].payload.front", equalTo("Version Regenerada"))
        }
    }
}
