package cr.ac.una.glifo.note

import cr.ac.una.glifo.common.exception.BusinessException
import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.note.dto.CreateNotePageRequest
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.service.NoteService
import cr.ac.una.glifo.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Issue #26: lo que sube la cola offline del teléfono. Una nota creada, sus páginas ya
 * procesadas en el dispositivo y los reintentos que llegan duplicados.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotePageSyncTest {

    @Autowired lateinit var noteService: NoteService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var courseRepository: CourseRepository

    private val mapper = JsonMapper.builder().findAndAddModules().build()

    private fun studentId() = userRepository.findByEmail("estudiante@est.una.ac.cr")!!.id
    private fun teacherId() = userRepository.findByEmail("docente@una.ac.cr")!!.id
    private fun courseId() = courseRepository.findByJoinCode("GLF-CALC1")!!.id

    private fun pageRequest(index: Int) = mapper.readValue(
        """
        {"page_index": $index, "perceptual_hash": "c3a1f0e98b7d6245", "level_reached": "N1",
         "overall_confidence": 0.82,
         "regions": [{"id": "r1", "kind": "TEXT", "confidence": 0.93, "text": "Límite"}],
         "quality_metrics": {"blurVariance": 142.7, "isUsable": true}}
        """.trimIndent(),
        CreatePageRequestType
    )

    @Test
    fun `una pagina subida queda guardada con sus regiones y la nota pasa a PROCESSING`() {
        val note = noteService.createNote(studentId(), CreateNoteRequest(courseId(), null, "2026-09-30", "Captura offline"))
        assertEquals("DRAFT", note.status)

        val page = noteService.addPage(note.id, studentId(), pageRequest(0))

        assertEquals("c3a1f0e98b7d6245", page.perceptualHash)
        assertEquals("device://c3a1f0e98b7d6245", page.storageUri)
        assertEquals("PROCESSING", noteService.getNoteById(note.id, studentId()).status)
        val pages = noteService.getPages(note.id, studentId())
        assertEquals(1, pages.size)
        assertTrue(pages.single().regions!!.contains("\"Límite\""))
    }

    @Test
    fun `un reintento de la misma pagina responde 409 en vez de duplicarla`() {
        val note = noteService.createNote(studentId(), CreateNoteRequest(courseId(), null, "2026-09-30", "Reintento"))
        noteService.addPage(note.id, studentId(), pageRequest(0))

        assertThrows<DuplicateResourceException> { noteService.addPage(note.id, studentId(), pageRequest(0)) }
        assertEquals(1, noteService.getPages(note.id, studentId()).size)
    }

    @Test
    fun `otro usuario no puede subir paginas a un apunte ajeno`() {
        val note = noteService.createNote(studentId(), CreateNoteRequest(courseId(), null, "2026-09-30", "Privado"))

        assertThrows<ResourceNotFoundException> { noteService.addPage(note.id, teacherId(), pageRequest(0)) }
    }

    @Test
    fun `un nivel de procesamiento desconocido se rechaza`() {
        val note = noteService.createNote(studentId(), CreateNoteRequest(courseId(), null, "2026-09-30", "Nivel"))
        val bad = pageRequest(0).copy(levelReached = "N9")

        assertThrows<BusinessException> { noteService.addPage(note.id, studentId(), bad) }
    }

    @Test
    fun `el detalle incluye content como JSON y el listado no`() {
        val seeded = noteService.getNotesByUser(studentId(), courseId()).first { it.status == "READY" }
        assertNull(seeded.content)

        val detail = noteService.getNoteById(seeded.id, studentId())
        val json = mapper.readTree(mapper.writeValueAsString(detail))

        // @JsonRawValue: content viaja como objeto, no como un string con JSON adentro.
        assertTrue(json["content"].isObject)
        assertEquals(1, json["content"]["schemaVersion"].asInt())
    }

    @Test
    fun `las regiones de una pagina viajan como arreglo JSON`() {
        val response = NotePageResponse(1, 2, 0, "c3a1f0e98b7d6245", "device://x", "N1", 0.8f, null, """[{"id":"r1"}]""")
        val json = mapper.readTree(mapper.writeValueAsString(response))

        assertTrue(json["regions"].isArray)
        assertEquals("r1", json["regions"][0]["id"].asString())
    }

    private companion object {
        val CreatePageRequestType = CreateNotePageRequest::class.java
    }
}
