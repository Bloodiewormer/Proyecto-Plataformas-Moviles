package cr.ac.una.glifo.study

import cr.ac.una.glifo.ai.LlmResult
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
import cr.ac.una.glifo.ai.service.GeneratedBatch
import cr.ac.una.glifo.ai.service.GeneratedStudyItem
import cr.ac.una.glifo.ai.service.GenerationService
import cr.ac.una.glifo.ai.service.TopicInput
import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.repository.StudyItemRepository
import cr.ac.una.glifo.study.service.StudyItemService
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
import kotlin.test.assertEquals

class StudyItemServiceTest {

    private lateinit var courseRepository: CourseRepository
    private lateinit var enrollmentRepository: EnrollmentRepository
    private lateinit var syllabusTopicRepository: SyllabusTopicRepository
    private lateinit var studyItemRepository: StudyItemRepository
    private lateinit var generationService: GenerationService
    private lateinit var service: StudyItemService

    private val teacher = User().apply { id = 2L }
    private val student = User().apply { id = 1L }
    private val course = Course().apply {
        id = 10L
        name = "Cálculo I"
        owner = teacher
    }

    private val topic101 = SyllabusTopic().apply {
        id = 101L
        code = "U1.1"
        title = "Definición de límite"
        this.course = this@StudyItemServiceTest.course
    }

    private val topic102 = SyllabusTopic().apply {
        id = 102L
        code = "U1.2"
        title = "Teorema del valor extremo"
        this.course = this@StudyItemServiceTest.course
    }

    @BeforeEach
    fun setUp() {
        courseRepository = mock(CourseRepository::class.java)
        enrollmentRepository = mock(EnrollmentRepository::class.java)
        syllabusTopicRepository = mock(SyllabusTopicRepository::class.java)
        studyItemRepository = mock(StudyItemRepository::class.java)
        generationService = mock(GenerationService::class.java)

        val courseService = CourseService(
            courseRepository,
            enrollmentRepository,
            mock(UserRepository::class.java),
            syllabusTopicRepository
        )
        service = StudyItemService(
            courseRepository = courseRepository,
            enrollmentRepository = enrollmentRepository,
            syllabusTopicRepository = syllabusTopicRepository,
            studyItemRepository = studyItemRepository,
            generationService = generationService,
            courseService = courseService
        )

        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))
        `when`(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true)
        `when`(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(1L, 10L, "ACTIVE")).thenReturn(true)
        `when`(syllabusTopicRepository.findByCourseIdOrderByOrderIndex(10L))
            .thenReturn(listOf(topic101, topic102))
        `when`(studyItemRepository.saveAll(anyList())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val list = invocation.arguments[0] as List<StudyItem>
            list.forEachIndexed { idx, item -> if (item.id == 0L) item.id = (idx + 1).toLong() }
            list
        }
    }

    private fun existingFlashcard(topic: SyllabusTopic, id: Long = 500L) = StudyItem().apply {
        this.id = id
        this.course = this@StudyItemServiceTest.course
        this.syllabusTopic = topic
        this.kind = "FLASHCARD"
        this.payload = """{"schemaVersion":1,"kind":"FLASHCARD","front":"F","back":"B","difficulty":"MEDIUM","sourceNoteIds":[]}"""
    }

    @Test
    fun `a - curso inexistente lanza ResourceNotFoundException (404)`() {
        `when`(courseRepository.findById(404L)).thenReturn(Optional.empty())
        assertThrows<ResourceNotFoundException> {
            service.generate(
                courseId = 404L,
                request = GenerateStudyItemsRequest(topicIds = listOf(101L)),
                userId = 1L
            )
        }
    }

    @Test
    fun `b - usuario no matriculado ni docente del curso lanza AccessDeniedException (403)`() {
        `when`(enrollmentRepository.existsByUserIdAndCourseId(99L, 10L)).thenReturn(false)
        assertThrows<AccessDeniedException> {
            service.generate(
                courseId = 10L,
                request = GenerateStudyItemsRequest(topicIds = listOf(101L)),
                userId = 99L
            )
        }
    }

    @Test
    fun `c - topicId que pertenece a otro curso lanza BadRequestException (400)`() {
        assertThrows<BadRequestException> {
            service.generate(
                courseId = 10L,
                request = GenerateStudyItemsRequest(topicIds = listOf(101L, 999L)),
                userId = 1L
            )
        }
    }

    /** any() de Mockito devuelve null; en Kotlin hace falta un valor no nulo de relleno. */
    private fun anyOrigin(): AiCallOrigin = org.mockito.ArgumentMatchers.any(AiCallOrigin::class.java) ?: AiCallOrigin(0L, null)

    @Test
    fun `d - tema que ya tiene items no llama a la IA`() {
        `when`(studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(1L, 10L, 101L))
            .thenReturn(listOf(existingFlashcard(topic101)))

        val outcome = service.generate(
            courseId = 10L,
            request = GenerateStudyItemsRequest(topicIds = listOf(101L), itemsPerTopic = 3),
            userId = 1L
        )

        assertEquals(1, outcome.items.size)
        assertEquals(0, outcome.generatedTopics)
        assertEquals(1, outcome.reusedTopics)
        assertEquals(0, outcome.generatedItems)

        verify(generationService, never()).generateStudyItems(
            anyString() ?: "",
            anyList() ?: emptyList(),
            anyInt(),
            anyOrigin()
        )
    }

    @Test
    fun `e - mezcla de temas nuevos y existentes envia solo los nuevos a la IA en una sola llamada`() {
        `when`(studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(1L, 10L, 101L))
            .thenReturn(listOf(existingFlashcard(topic101)))
        `when`(studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(1L, 10L, 102L))
            .thenReturn(emptyList())

        @Suppress("UNCHECKED_CAST")
        val topicsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<TopicInput>>
        val courseNameCaptor = ArgumentCaptor.forClass(String::class.java)
        val countCaptor = ArgumentCaptor.forClass(Int::class.java)
        val originCaptor = ArgumentCaptor.forClass(AiCallOrigin::class.java)

        val generatedPayload = """{"schemaVersion":1,"kind":"TRUE_FALSE","statement":"S","answer":true,"explanation":"E","difficulty":"EASY","sourceNoteIds":[]}"""
        `when`(
            generationService.generateStudyItems(
                courseNameCaptor.capture() ?: "",
                topicsCaptor.capture() ?: emptyList(),
                countCaptor.capture() ?: 3,
                originCaptor.capture() ?: AiCallOrigin(0L, null)
            )
        ).thenReturn(
            GeneratedBatch(
                items = listOf(GeneratedStudyItem(topicId = 102L, kind = "TRUE_FALSE", payload = generatedPayload)),
                llmResult = LlmResult(content = "{}", model = "gpt-4o-mini", inputTokens = 50, outputTokens = 80, latencyMs = 200L)
            )
        )

        val outcome = service.generate(
            courseId = 10L,
            request = GenerateStudyItemsRequest(topicIds = listOf(101L, 102L), itemsPerTopic = 2),
            userId = 1L
        )

        verify(generationService, times(1)).generateStudyItems(
            courseNameCaptor.value,
            topicsCaptor.value,
            countCaptor.value,
            originCaptor.value
        )
        assertEquals(listOf(102L), topicsCaptor.value.map { it.id })
        // ai_calls (#46): la llamada queda a nombre de quien la pidió y del curso.
        assertEquals(AiCallOrigin(userId = 1L, courseId = 10L), originCaptor.value)
        assertEquals(2, outcome.items.size)
        assertEquals(1, outcome.generatedTopics)
        assertEquals(1, outcome.reusedTopics)
        assertEquals(1, outcome.generatedItems)
    }

    @Test
    fun `f - list sin topicId devuelve todos los items del curso del usuario y con topicId filtra por tema`() {
        val item101 = existingFlashcard(topic101, id = 501L)
        val item102 = existingFlashcard(topic102, id = 502L)
        `when`(studyItemRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(listOf(item101, item102))
        `when`(studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(1L, 10L, 101L)).thenReturn(listOf(item101))

        val allItems = service.list(courseId = 10L, topicId = null, userId = 1L)
        assertEquals(2, allItems.size)
        assertEquals(listOf(501L, 502L), allItems.map { it.id })

        val filteredItems = service.list(courseId = 10L, topicId = 101L, userId = 1L)
        assertEquals(1, filteredItems.size)
        assertEquals(501L, filteredItems[0].id)
        assertEquals(101L, filteredItems[0].syllabusTopicId)
    }

    @Test
    fun `g - list responde 404 si el usuario no es miembro ni admin`() {
        `when`(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(99L, 10L, "ACTIVE")).thenReturn(false)

        assertThrows<ResourceNotFoundException> {
            service.list(courseId = 10L, topicId = null, userId = 99L)
        }
    }

    @Test
    fun `h - list permite acceso al admin aunque no este matriculado`() {
        val item101 = existingFlashcard(topic101, id = 501L)
        `when`(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(99L, 10L, "ACTIVE")).thenReturn(false)
        `when`(studyItemRepository.findByUserIdAndCourseId(99L, 10L)).thenReturn(listOf(item101))

        val items = service.list(courseId = 10L, topicId = null, userId = 99L, isAdmin = true)
        assertEquals(1, items.size)
    }

    @Test
    fun `i - regenerate true reemplaza items existentes del estudiante y llama a la IA`() {
        val oldItem = existingFlashcard(topic101, id = 701L)
        `when`(studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicIdIn(1L, 10L, listOf(101L)))
            .thenReturn(listOf(oldItem))

        val newPayload = """{"schemaVersion":1,"kind":"FLASHCARD","front":"Nuevo frente","back":"Nuevo dorso","difficulty":"EASY","sourceNoteIds":[]}"""
        val generatedBatch = GeneratedBatch(
            items = listOf(GeneratedStudyItem(topicId = 101L, kind = "FLASHCARD", payload = newPayload)),
            llmResult = LlmResult(content = "{}", model = "gpt-4o-mini", inputTokens = 40, outputTokens = 60, latencyMs = 150L)
        )
        `when`(
            generationService.generateStudyItems(
                anyString() ?: "",
                anyList() ?: emptyList(),
                anyInt(),
                anyOrigin()
            )
        ).thenReturn(generatedBatch)

        val outcome = service.generate(
            courseId = 10L,
            request = GenerateStudyItemsRequest(topicIds = listOf(101L), itemsPerTopic = 1, regenerate = true),
            userId = 1L
        )

        verify(generationService, times(1)).generateStudyItems(anyString() ?: "", anyList() ?: emptyList(), anyInt(), anyOrigin())
        verify(studyItemRepository, times(1)).deleteAll(listOf(oldItem))
        assertEquals(1, outcome.items.size)
        assertEquals(1, outcome.generatedTopics)
        assertEquals(0, outcome.reusedTopics)
    }
}
