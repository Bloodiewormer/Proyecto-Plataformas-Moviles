package cr.ac.una.glifo.glossary

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.course.service.CourseService
import cr.ac.una.glifo.glossary.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.glossary.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.entity.CourseGlossary
import cr.ac.una.glifo.glossary.entity.GlossarySuggestion
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.glossary.repository.GlossarySuggestionRepository
import cr.ac.una.glifo.glossary.service.GlossaryService
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.access.AccessDeniedException
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pruebas UNITARIAS de GlossaryService: sin Spring ni base de datos, con repositorios
 * simulados. Cubren quién puede leer, escribir y moderar el glosario, y que cada cambio
 * sube glossary_version (D-17).
 */
class GlossaryServiceUnitTest {

    private lateinit var courseRepository: CourseRepository
    private lateinit var enrollmentRepository: EnrollmentRepository
    private lateinit var glossaryRepository: CourseGlossaryRepository
    private lateinit var suggestionRepository: GlossarySuggestionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var noteRepository: NoteRepository
    private lateinit var service: GlossaryService

    private val teacher = User().apply { id = 2L; firstName = "Mike"; lastName = "Guzmán" }
    private val student = User().apply { id = 1L; firstName = "Ana"; lastName = "Mora" }
    private val course = Course().apply { id = 10L; name = "Cálculo I"; owner = teacher; glossaryVersion = 3 }

    @BeforeEach
    fun setUp() {
        courseRepository = mock(CourseRepository::class.java)
        enrollmentRepository = mock(EnrollmentRepository::class.java)
        glossaryRepository = mock(CourseGlossaryRepository::class.java)
        suggestionRepository = mock(GlossarySuggestionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        noteRepository = mock(NoteRepository::class.java)
        val courseService = CourseService(
            courseRepository,
            enrollmentRepository,
            userRepository,
            mock(SyllabusTopicRepository::class.java)
        )
        service = GlossaryService(
            courseRepository, courseService, glossaryRepository,
            suggestionRepository, userRepository, noteRepository
        )

        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))
        `when`(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(1L, 10L, "ACTIVE")).thenReturn(true)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(student))
        // save devuelve lo mismo que recibe, como haría el repositorio real.
        `when`(glossaryRepository.save(any(CourseGlossary::class.java))).thenAnswer { it.arguments[0] }
        `when`(suggestionRepository.save(any(GlossarySuggestion::class.java))).thenAnswer { it.arguments[0] }
        `when`(courseRepository.save(any(Course::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun pendingSuggestion() = GlossarySuggestion().apply {
        id = 50L
        this.course = this@GlossaryServiceUnitTest.course
        user = student
        originalText = "cnv"
        suggestedCorrection = "converge"
        status = "PENDING"
    }

    // ---------- Leer ----------

    @Test
    fun `el estudiante matriculado lee el glosario con sus versiones`() {
        `when`(glossaryRepository.findByCourseIdOrderByTerm(10L)).thenReturn(
            listOf(CourseGlossary().apply { id = 1L; term = "ε"; canonicalForm = "\\varepsilon"; kind = "SYMBOL" })
        )

        val response = service.getGlossary(10L, 1L)

        assertEquals("Cálculo I", response.courseName)
        assertEquals(3, response.glossaryVersion)
        assertEquals("\\varepsilon", response.entries.single().canonicalForm)
    }

    @Test
    fun `quien no es miembro del curso recibe 404 al leer el glosario`() {
        `when`(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(99L, 10L, "ACTIVE")).thenReturn(false)

        assertThrows<ResourceNotFoundException> { service.getGlossary(10L, 99L) }
    }

    // ---------- Insert ----------

    @Test
    fun `el docente dueno agrega una entrada y sube la version del glosario`() {
        val response = service.addEntry(10L, 2L, CreateGlossaryEntryRequest("  Converge ", " conv. ", "TERM"))

        assertEquals("Converge", response.term)
        assertEquals("conv.", response.canonicalForm)
        assertEquals(4, course.glossaryVersion)
    }

    @Test
    fun `un termino repetido con el mismo tipo es 409 y no sube la version`() {
        `when`(glossaryRepository.existsByCourseIdAndTermIgnoreCaseAndKind(10L, "Converge", "TERM")).thenReturn(true)

        assertThrows<DuplicateResourceException> {
            service.addEntry(10L, 2L, CreateGlossaryEntryRequest("Converge", "conv.", "TERM"))
        }
        verify(glossaryRepository, never()).save(any(CourseGlossary::class.java))
        assertEquals(3, course.glossaryVersion)
    }

    @Test
    fun `un docente que no es dueno del curso recibe 403`() {
        assertThrows<AccessDeniedException> {
            service.addEntry(10L, 77L, CreateGlossaryEntryRequest("Converge", "conv.", "TERM"))
        }
    }

    // ---------- Delete ----------

    @Test
    fun `borrar una entrada sube la version del glosario`() {
        val entry = CourseGlossary().apply { id = 5L; this.course = this@GlossaryServiceUnitTest.course }
        `when`(glossaryRepository.findByIdAndCourseId(5L, 10L)).thenReturn(entry)

        service.deleteEntry(10L, 5L, 2L)

        verify(glossaryRepository).delete(entry)
        assertEquals(4, course.glossaryVersion)
    }

    // ---------- Sugerencias ----------

    @Test
    fun `el estudiante deja una sugerencia PENDING sin tocar el glosario`() {
        val response = service.suggest(10L, 1L, CreateGlossarySuggestionRequest(" cnv ", " converge "))

        assertEquals("PENDING", response.status)
        assertEquals("cnv", response.originalText)
        assertEquals("Ana Mora", response.suggestedBy)
        verify(glossaryRepository, never()).save(any(CourseGlossary::class.java))
    }

    @Test
    fun `no se puede citar el apunte de otra persona en una sugerencia`() {
        val othersNote = Note().apply { id = 7L; user = teacher; this.course = this@GlossaryServiceUnitTest.course }
        `when`(noteRepository.findById(7L)).thenReturn(Optional.of(othersNote))

        assertThrows<ResourceNotFoundException> {
            service.suggest(10L, 1L, CreateGlossarySuggestionRequest("cnv", "converge", noteId = 7L))
        }
    }

    @Test
    fun `aprobar una sugerencia la agrega al glosario`() {
        `when`(suggestionRepository.findByIdAndCourseId(50L, 10L)).thenReturn(pendingSuggestion())

        val response = service.resolveSuggestion(10L, 50L, 2L, ResolveGlossarySuggestionRequest("APPROVED", "TERM"))

        val saved = ArgumentCaptor.forClass(CourseGlossary::class.java)
        verify(glossaryRepository).save(saved.capture())
        assertEquals("cnv", saved.value.term)
        assertEquals("converge", saved.value.canonicalForm)
        assertEquals("APPROVED", response.status)
        assertNotNull(response.resolvedAt)
        assertEquals(4, course.glossaryVersion)
    }

    @Test
    fun `rechazar una sugerencia no toca el glosario`() {
        `when`(suggestionRepository.findByIdAndCourseId(50L, 10L)).thenReturn(pendingSuggestion())

        val response = service.resolveSuggestion(10L, 50L, 2L, ResolveGlossarySuggestionRequest("REJECTED"))

        assertEquals("REJECTED", response.status)
        verify(glossaryRepository, never()).save(any(CourseGlossary::class.java))
        assertEquals(3, course.glossaryVersion)
    }

    @Test
    fun `una sugerencia ya resuelta no se puede volver a resolver`() {
        val resolved = pendingSuggestion().apply { status = "REJECTED" }
        `when`(suggestionRepository.findByIdAndCourseId(50L, 10L)).thenReturn(resolved)

        assertThrows<ConflictException> {
            service.resolveSuggestion(10L, 50L, 2L, ResolveGlossarySuggestionRequest("APPROVED"))
        }
    }

    @Test
    fun `listar sugerencias con un estado desconocido es 400`() {
        assertThrows<BadRequestException> { service.getSuggestions(10L, 2L, "MAYBE") }
    }
}
