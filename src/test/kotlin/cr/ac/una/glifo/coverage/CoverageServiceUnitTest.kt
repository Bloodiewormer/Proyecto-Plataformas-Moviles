package cr.ac.una.glifo.coverage

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.coverage.service.CoverageService
import cr.ac.una.glifo.study.entity.TopicCoverage
import cr.ac.una.glifo.study.repository.TopicCoverageRepository
import cr.ac.una.glifo.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pruebas UNITARIAS de CoverageService: sin Spring ni base de datos, con repositorios
 * simulados. El temario de prueba es el mismo que carga import.sql:
 *
 *   U1 Límites y continuidad (padre)
 *     U1.1 Límite de una función     SOLID   0.86
 *     U1.2 Teorema del valor extremo PARTIAL 0.45
 *   U2 Derivadas                     ABSENT  0.00
 */
class CoverageServiceUnitTest {

    private lateinit var courseRepository: CourseRepository
    private lateinit var enrollmentRepository: EnrollmentRepository
    private lateinit var topicRepository: SyllabusTopicRepository
    private lateinit var coverageRepository: TopicCoverageRepository
    private lateinit var service: CoverageService

    private val teacher = User().apply { id = 2L }
    private val student = User().apply { id = 1L }
    private val course = Course().apply { id = 10L; owner = teacher }

    private val u1 = topic(100L, "U1", parent = null)
    private val u11 = topic(101L, "U1.1", parent = u1)
    private val u12 = topic(102L, "U1.2", parent = u1)
    private val u2 = topic(200L, "U2", parent = null)
    private val topics = listOf(u1, u11, u12, u2)
    private val topicIds = topics.map { it.id }

    @BeforeEach
    fun setUp() {
        courseRepository = mock(CourseRepository::class.java)
        enrollmentRepository = mock(EnrollmentRepository::class.java)
        topicRepository = mock(SyllabusTopicRepository::class.java)
        coverageRepository = mock(TopicCoverageRepository::class.java)
        service = CoverageService(courseRepository, enrollmentRepository, topicRepository, coverageRepository)

        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))
        `when`(enrollmentRepository.existsByUserIdAndCourseId(1L, 10L)).thenReturn(true)
        `when`(topicRepository.findByCourseIdOrderByOrderIndex(10L)).thenReturn(topics)
    }

    private fun topic(id: Long, code: String, parent: SyllabusTopic?) = SyllabusTopic().apply {
        this.id = id
        this.code = code
        this.title = "Tema $code"
        this.course = this@CoverageServiceUnitTest.course
        this.parent = parent
    }

    private fun coverage(topic: SyllabusTopic, state: String, score: Float) = TopicCoverage().apply {
        user = student
        syllabusTopic = topic
        this.state = state
        this.score = score
    }

    // ---------- GET /coverage ----------

    @Test
    fun `el porcentaje es el promedio de los temas hoja, sin contar el tema padre`() {
        `when`(coverageRepository.findByUserIdAndSyllabusTopicIdIn(1L, topicIds)).thenReturn(
            listOf(coverage(u11, "SOLID", 0.86f), coverage(u12, "PARTIAL", 0.45f), coverage(u2, "ABSENT", 0f))
        )

        val response = service.getCoverage(10L, 1L)

        // (0.86 + 0.45 + 0) / 3 = 0.4366 → 43.7 %
        assertEquals(43.7f, response.coveragePct)
        assertEquals(mapOf("SOLID" to 1, "PARTIAL" to 1, "ABSENT" to 1, "UNCERTAIN" to 0), response.stateCounts)
    }

    @Test
    fun `el tema padre no tiene estado y su score es el promedio de sus subtemas`() {
        `when`(coverageRepository.findByUserIdAndSyllabusTopicIdIn(1L, topicIds)).thenReturn(
            listOf(coverage(u11, "SOLID", 0.8f), coverage(u12, "PARTIAL", 0.4f))
        )

        val parent = service.getCoverage(10L, 1L).topics.first { it.code == "U1" }

        assertNull(parent.state)
        assertEquals(0.6f, parent.score, 0.0001f)
    }

    @Test
    fun `un tema sin fila de cobertura cuenta como ABSENT con score 0`() {
        `when`(coverageRepository.findByUserIdAndSyllabusTopicIdIn(1L, topicIds)).thenReturn(emptyList())

        val response = service.getCoverage(10L, 1L)

        assertEquals(0f, response.coveragePct)
        assertEquals(3, response.stateCounts["ABSENT"])
        assertEquals("ABSENT", response.topics.first { it.code == "U2" }.state)
    }

    @Test
    fun `un curso sin temario responde 0 por ciento y sin temas`() {
        `when`(topicRepository.findByCourseIdOrderByOrderIndex(10L)).thenReturn(emptyList())

        val response = service.getCoverage(10L, 1L)

        assertEquals(0f, response.coveragePct)
        assertEquals(0, response.topics.size)
    }

    @Test
    fun `el docente dueno puede ver la cobertura sin estar matriculado`() {
        `when`(coverageRepository.findByUserIdAndSyllabusTopicIdIn(2L, topicIds)).thenReturn(emptyList())

        val response = service.getCoverage(10L, 2L)

        assertEquals(10L, response.courseId)
    }

    @Test
    fun `quien no esta matriculado recibe 404 y no 403`() {
        `when`(enrollmentRepository.existsByUserIdAndCourseId(99L, 10L)).thenReturn(false)

        assertThrows<ResourceNotFoundException> { service.getCoverage(10L, 99L) }
    }

    @Test
    fun `un curso inexistente es 404`() {
        `when`(courseRepository.findById(404L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> { service.getCoverage(404L, 1L) }
    }

    // ---------- GET /coverage/delta ----------

    @Test
    fun `el delta trae solo los temas que cambiaron desde la fecha`() {
        val since = Instant.now().minus(7, ChronoUnit.DAYS)
        `when`(coverageRepository.findByUserIdAndSyllabusTopicIdInAndUpdatedAtAfter(1L, topicIds, since))
            .thenReturn(listOf(coverage(u11, "SOLID", 0.9f)))

        val delta = service.getCoverageDelta(10L, 1L, since)

        assertEquals(listOf("U1.1"), delta.changedTopics.map { it.code })
        assertEquals(1, delta.changedCounts["SOLID"])
        assertEquals(0, delta.changedCounts["ABSENT"])
    }

    @Test
    fun `el delta rechaza una fecha en el futuro`() {
        val tomorrow = Instant.now().plus(1, ChronoUnit.DAYS)

        assertThrows<BadRequestException> { service.getCoverageDelta(10L, 1L, tomorrow) }
    }
}
