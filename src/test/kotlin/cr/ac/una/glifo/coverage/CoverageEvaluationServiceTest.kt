package cr.ac.una.glifo.coverage

import cr.ac.una.glifo.ai.AiProviderException
import cr.ac.una.glifo.ai.LlmClient
import cr.ac.una.glifo.ai.LlmRequest
import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.config.OpenAiProperties
import cr.ac.una.glifo.ai.service.SemanticJudgeService
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.coverage.service.CoverageEvaluationService
import cr.ac.una.glifo.coverage.service.CoveragePrefilter
import cr.ac.una.glifo.coverage.service.CoverageService
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.study.entity.TopicCoverage
import cr.ac.una.glifo.study.repository.TopicCoverageRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.any
import org.mockito.Mockito.anyList
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pruebas UNITARIAS de la evaluación de cobertura (issue #43): repositorios simulados, pre-filtro
 * real e IA-04 real sobre un LlmClient falso. Sin base de datos ni OpenAI.
 */
class CoverageEvaluationServiceTest {

    private class FakeLlmClient(var content: String) : LlmClient {
        val requests = mutableListOf<LlmRequest>()
        var failWith: RuntimeException? = null
        var onCall: (() -> Unit)? = null
        override fun completeJson(request: LlmRequest): LlmResult {
            requests.add(request)
            onCall?.invoke()
            failWith?.let { throw it }
            return LlmResult(content, "gpt-4o-mini", 900, 60, 400, finishReason = "stop")
        }
    }

    private val json = JsonMapper.builder().findAndAddModules().build()
    private val student = User().apply { id = 1L }
    private val course = Course().apply { id = 10L; name = "Cálculo I"; owner = User().apply { id = 2L } }
    private val unit = topic(100L, "U1", "Unidad 1", null)
    private val limits = topic(101L, "U1.1", "Límites de funciones", unit)
    private val integrals = topic(102L, "U1.2", "Integrales impropias", unit)
    private val series = topic(103L, "U1.3", "Series de potencias", unit)

    private lateinit var notes: NoteRepository
    private lateinit var coverageRows: TopicCoverageRepository
    private lateinit var topicsRepo: SyllabusTopicRepository
    private lateinit var llm: FakeLlmClient
    private lateinit var service: CoverageEvaluationService

    private fun topic(id: Long, code: String, title: String, parent: SyllabusTopic?) = SyllabusTopic().apply {
        this.id = id
        this.code = code
        this.title = title
        this.course = this@CoverageEvaluationServiceTest.course
        this.parent = parent
    }

    private fun note(topic: SyllabusTopic?, content: String?, status: String = NoteStatus.READY.name, title: String = "Apunte") = Note().apply {
        user = student
        course = this@CoverageEvaluationServiceTest.course
        syllabusTopic = topic
        this.title = title
        this.status = status
        this.content = content
    }

    private fun paragraph(text: String, uncertain: Boolean = false) =
        """{"schemaVersion": 1, "blocks": [{"type": "PARAGRAPH", "spans": [{"text": "$text", "isUncertain": $uncertain}]}]}"""

    private fun row(topic: SyllabusTopic, state: String, score: Float) = TopicCoverage().apply {
        user = student
        syllabusTopic = topic
        this.state = state
        this.score = score
        updatedAt = Instant.parse("2026-10-01T00:00:00Z")
    }

    private fun build(topics: List<SyllabusTopic> = listOf(unit, limits, integrals, series), apiKey: String = "key") {
        val courses = mock(CourseRepository::class.java)
        val enrollments = mock(EnrollmentRepository::class.java)
        topicsRepo = mock(SyllabusTopicRepository::class.java)
        val users = mock(UserRepository::class.java)
        `when`(courses.findById(10L)).thenReturn(Optional.of(course))
        `when`(enrollments.existsByUserIdAndCourseIdAndStatus(1L, 10L, "ACTIVE")).thenReturn(true)
        `when`(topicsRepo.findByCourseIdOrderByOrderIndex(10L)).thenReturn(topics)
        topics.forEach { t -> `when`(topicsRepo.getReferenceById(t.id)).thenReturn(t) }
        `when`(users.getReferenceById(1L)).thenReturn(student)
        `when`(coverageRows.save(any(TopicCoverage::class.java))).thenAnswer { it.arguments[0] }
        val courseService = CourseService(courses, enrollments, users, topicsRepo)
        service = CoverageEvaluationService(
            CoverageService(courses, enrollments, topicsRepo, coverageRows, courseService),
            topicsRepo, notes, coverageRows, users, CoveragePrefilter(),
            SemanticJudgeService(llm, OpenAiProperties(apiKey = apiKey), json, cr.ac.una.glifo.ai.discardingLedger()),
            TransactionOperations.withoutTransaction(), json
        )
    }

    @BeforeEach
    fun setUp() {
        notes = mock(NoteRepository::class.java)
        coverageRows = mock(TopicCoverageRepository::class.java)
        llm = FakeLlmClient("""{"verdicts": [{"topicId": 102, "state": "PARTIAL", "score": 0.5}]}""")
        `when`(notes.findByUserIdAndCourseId(1L, 10L)).thenReturn(
            listOf(
                note(limits, paragraph("El límite de una función en un punto")),
                note(null, paragraph("Repaso de integrales definidas")),
                note(series, paragraph("Series de potencias"), status = NoteStatus.ARCHIVED.name),
                note(series, null, status = NoteStatus.DRAFT.name, title = "Series de potencias"),
                note(series, paragraph("Series de potencias convergentes", uncertain = true))
            )
        )
        build()
    }

    private fun savedByTopic(): Map<Long, TopicCoverage> {
        val captor = ArgumentCaptor.forClass(TopicCoverage::class.java)
        verify(coverageRows, atLeastOnce()).save(captor.capture())
        return captor.allValues.associateBy { it.syllabusTopic!!.id }
    }

    @Test
    fun `el pre-filtro resuelve lo claro y solo lo ambiguo va a IA-04 en una llamada`() {
        val result = service.evaluate(10L, 1L)

        assertEquals(1, llm.requests.size)
        assertTrue(llm.requests.single().userPrompt.contains("\"topicId\":102"))
        assertFalse(llm.requests.single().userPrompt.contains("\"topicId\":101"))
        val saved = savedByTopic()
        assertEquals("SOLID", saved.getValue(101L).state)
        assertEquals("PARTIAL", saved.getValue(102L).state)
        assertEquals(2, result.prefilterResolved)
        assertEquals(1, result.adjudicated)
        assertEquals(0, result.uncertain)
        assertTrue(result.aiCalled)
    }

    @Test
    fun `borradores, archivados, titulos y fragmentos inciertos no cuentan como evidencia y el ABSENT nuevo no se inserta`() {
        val result = service.evaluate(10L, 1L)

        assertEquals("ABSENT", result.coverage.topics.first { it.topicId == 103L }.state)
        assertFalse(103L in savedByTopic().keys)
    }

    @Test
    fun `un tema que IA-04 no adjudica queda UNCERTAIN con score cero`() {
        llm.content = """{"verdicts": []}"""

        val result = service.evaluate(10L, 1L)

        val uncertain = savedByTopic().getValue(102L)
        assertEquals("UNCERTAIN", uncertain.state)
        assertEquals(0f, uncertain.score)
        assertTrue(result.aiCalled)
    }

    @Test
    fun `un ABSENT de IA-04 sin fila previa no se inserta`() {
        llm.content = """{"verdicts": [{"topicId": 102, "state": "ABSENT", "score": 0.2}]}"""

        service.evaluate(10L, 1L)

        assertFalse(102L in savedByTopic().keys)
    }

    @Test
    fun `si IA-04 omite un tema con evaluacion previa esta se conserva`() {
        val previous = row(integrals, "PARTIAL", 0.5f)
        `when`(coverageRows.findByUserIdAndSyllabusTopicIdIn(1L, listOf(101L, 102L, 103L))).thenReturn(listOf(previous))
        llm.content = """{"verdicts": []}"""

        service.evaluate(10L, 1L)

        assertFalse(102L in savedByTopic().keys)
    }

    @Test
    fun `sin IA disponible un tema ambiguo con evaluacion previa pasa a UNCERTAIN`() {
        build(apiKey = "")
        val previous = row(integrals, "SOLID", 0.9f)
        `when`(coverageRows.findByUserIdAndSyllabusTopicIdIn(1L, listOf(101L, 102L, 103L))).thenReturn(listOf(previous))

        service.evaluate(10L, 1L)

        assertEquals("UNCERTAIN", savedByTopic().getValue(102L).state)
        assertEquals(0f, previous.score)
    }

    @Test
    fun `una adjudicacion previa no se degrada a UNCERTAIN ni se reescribe por variacion del score`() {
        val previous = row(integrals, "PARTIAL", 0.5f)
        `when`(coverageRows.findByUserIdAndSyllabusTopicIdIn(1L, listOf(101L, 102L, 103L))).thenReturn(listOf(previous))
        llm.content = """{"verdicts": [{"topicId": 102, "state": "PARTIAL", "score": 0.6}]}"""

        service.evaluate(10L, 1L)

        assertFalse(102L in savedByTopic().keys)
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), previous.updatedAt)
    }

    @Test
    fun `sin api key los ambiguos quedan UNCERTAIN y el resto se resuelve sin IA`() {
        build(apiKey = "")

        val result = service.evaluate(10L, 1L)

        assertTrue(llm.requests.isEmpty())
        assertFalse(result.aiCalled)
        assertEquals("UNCERTAIN", savedByTopic().getValue(102L).state)
        assertEquals("SOLID", savedByTopic().getValue(101L).state)
    }

    @Test
    fun `una fila del pre-filtro sin cambios no se guarda`() {
        val unchanged = row(limits, "SOLID", 1f)
        `when`(coverageRows.findByUserIdAndSyllabusTopicIdIn(1L, listOf(101L, 102L, 103L))).thenReturn(listOf(unchanged))

        service.evaluate(10L, 1L)

        assertFalse(101L in savedByTopic().keys)
    }

    @Test
    fun `una segunda evaluacion con IA dentro del minuto responde 409 sin otra llamada`() {
        service.evaluate(10L, 1L)

        val ex = assertThrows<ConflictException> { service.evaluate(10L, 1L) }

        assertEquals("COVERAGE_EVALUATION_COOLDOWN", ex.code)
        assertEquals(1, llm.requests.size)
    }

    @Test
    fun `si el proveedor falla no se guarda nada ni se activa la espera`() {
        llm.failWith = AiProviderException("caido")
        assertThrows<AiProviderException> { service.evaluate(10L, 1L) }
        verify(coverageRows, never()).save(any(TopicCoverage::class.java))

        llm.failWith = null
        service.evaluate(10L, 1L)

        assertEquals(2, llm.requests.size)
    }

    @Test
    fun `una evaluacion simultanea del mismo usuario y curso responde 409`() {
        var nested: Throwable? = null
        llm.onCall = { nested = runCatching { service.evaluate(10L, 1L) }.exceptionOrNull() }

        service.evaluate(10L, 1L)

        assertEquals("COVERAGE_EVALUATION_IN_PROGRESS", (nested as ConflictException).code)
        assertEquals(1, llm.requests.size)
    }

    @Test
    fun `mas de 25 temas ambiguos envian solo 25 y el resto queda UNCERTAIN`() {
        val many = (1..30).map { topic(200L + it, "T$it", "Integrales impropias $it", null) }
        build(topics = many)
        llm.content = """{"verdicts": []}"""

        val result = service.evaluate(10L, 1L)

        val prompt = llm.requests.single().systemPrompt + llm.requests.single().userPrompt
        assertEquals(CoverageEvaluationService.MAX_TOPICS_PER_BATCH, Regex("\"code\":\"T\\d+\"").findAll(prompt).count())
        assertTrue(result.aiCalled)
        assertEquals(30, savedByTopic().values.count { it.state == "UNCERTAIN" })
    }

    @Test
    fun `sin acceso al curso responde 404 sin evaluar`() {
        assertThrows<ResourceNotFoundException> { service.evaluate(10L, 3L) }
        assertTrue(llm.requests.isEmpty())
        verify(coverageRows, never()).findByUserIdAndSyllabusTopicIdIn(anyLong(), anyList())
    }
}
