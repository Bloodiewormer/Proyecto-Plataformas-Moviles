package cr.ac.una.glifo.note

import cr.ac.una.glifo.common.exception.BusinessException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.note.service.NoteService
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
import java.util.Optional
import kotlin.test.assertEquals

/**
 * Pruebas UNITARIAS de NoteService: prueban solo la lógica del servicio, aislada.
 * No levantan Spring ni usan base de datos: los repositorios son simulados (mocks de
 * Mockito) y cada prueba les dice qué responder. Así se verifica que el servicio decide
 * bien qué insertar, buscar, actualizar o borrar, y que rechaza lo inválido.
 */
class NoteServiceUnitTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var userRepository: UserRepository
    private lateinit var courseRepository: CourseRepository
    private lateinit var service: NoteService

    private val student = User().apply { id = 1L; email = "ana@est.una.ac.cr" }
    private val course = Course().apply { id = 10L; name = "Cálculo I" }

    @BeforeEach
    fun setUp() {
        // Repositorios falsos: no hay base de datos detrás.
        noteRepository = mock(NoteRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        courseRepository = mock(CourseRepository::class.java)
        service = NoteService(
            noteRepository,
            mock(NotePageRepository::class.java),
            userRepository,
            courseRepository,
            mock(SyllabusTopicRepository::class.java)
        )
        // save devuelve el mismo apunte que recibe, como haría el repositorio real.
        `when`(noteRepository.save(any(Note::class.java))).thenAnswer { it.arguments[0] }
    }

    private fun noteOf(owner: User, status: String) = Note().apply {
        id = 100L
        user = owner
        this.course = this@NoteServiceUnitTest.course
        title = "Límites"
        this.status = status
    }

    // ---------- Insert ----------

    @Test
    fun `insert crea el apunte en DRAFT y lo guarda con el repositorio`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))

        val response = service.createNote(1L, CreateNoteRequest(courseId = 10L, classDate = "2026-10-07", title = "  Derivadas  "))

        // Se revisa QUÉ le mandó el servicio al repositorio.
        val saved = ArgumentCaptor.forClass(Note::class.java)
        verify(noteRepository).save(saved.capture())
        assertEquals("Derivadas", saved.value.title)   // quitó los espacios
        assertEquals("DRAFT", saved.value.status)       // estado inicial
        assertEquals(10L, response.courseId)
    }

    @Test
    fun `insert con un curso que no existe falla y no guarda nada`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.createNote(1L, CreateNoteRequest(courseId = 99L, classDate = "2026-10-07", title = "Derivadas"))
        }
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    @Test
    fun `insert con una fecha mal escrita falla y no guarda nada`() {
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(student))
        `when`(courseRepository.findById(10L)).thenReturn(Optional.of(course))

        assertThrows<BusinessException> {
            service.createNote(1L, CreateNoteRequest(courseId = 10L, classDate = "07/10/2026", title = "Derivadas"))
        }
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    // ---------- Find ----------

    @Test
    fun `find por curso usa la consulta de usuario y curso`() {
        `when`(noteRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(listOf(noteOf(student, "DRAFT")))

        val notes = service.getNotesByUser(1L, 10L)

        assertEquals(1, notes.size)
        assertEquals("Límites", notes[0].title)
    }

    @Test
    fun `find de un apunte ajeno responde no encontrado`() {
        val otherStudent = User().apply { id = 2L }
        `when`(noteRepository.findById(100L)).thenReturn(Optional.of(noteOf(otherStudent, "DRAFT")))

        assertThrows<ResourceNotFoundException> { service.getNoteById(100L, 1L) }
    }

    // ---------- Update ----------

    @Test
    fun `update de DRAFT a PROCESSING cambia el estado y lo guarda`() {
        val note = noteOf(student, "DRAFT")
        `when`(noteRepository.findById(100L)).thenReturn(Optional.of(note))

        service.updateNoteStatus(100L, 1L, "PROCESSING")

        assertEquals("PROCESSING", note.status)
        verify(noteRepository).save(note)
    }

    @Test
    fun `update a un estado no permitido falla y no guarda`() {
        `when`(noteRepository.findById(100L)).thenReturn(Optional.of(noteOf(student, "READY")))

        assertThrows<IllegalStateException> { service.updateNoteStatus(100L, 1L, "DRAFT") }
        verify(noteRepository, never()).save(any(Note::class.java))
    }

    // ---------- Delete ----------

    @Test
    fun `delete borra el apunte del dueño`() {
        val note = noteOf(student, "DRAFT")
        `when`(noteRepository.findById(100L)).thenReturn(Optional.of(note))

        service.deleteNote(100L, 1L)

        verify(noteRepository).delete(note)
    }

    @Test
    fun `delete de un apunte ajeno no borra nada`() {
        val note = noteOf(User().apply { id = 2L }, "DRAFT")
        `when`(noteRepository.findById(100L)).thenReturn(Optional.of(note))

        assertThrows<ResourceNotFoundException> { service.deleteNote(100L, 1L) }
        verify(noteRepository, never()).delete(any(Note::class.java))
    }
}
