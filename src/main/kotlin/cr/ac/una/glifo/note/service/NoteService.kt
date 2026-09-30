package cr.ac.una.glifo.note.service

import cr.ac.una.glifo.common.exception.BusinessException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.mapper.toResponse
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository
) {

    @Transactional
    fun createNote(userId: Long, request: CreateNoteRequest): NoteResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("Usuario no encontrado") }
        val course = courseRepository.findById(request.courseId).orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        val syllabusTopic = request.syllabusTopicId?.let {
            syllabusTopicRepository.findById(it).orElseThrow { ResourceNotFoundException("Tema del temario no encontrado") }
        }
        val classDate = try {
            LocalDate.parse(request.classDate)
        } catch (_: DateTimeParseException) {
            throw BusinessException("La fecha de la clase debe tener el formato AAAA-MM-DD", "VALIDATION_FAILED")
        }

        val note = Note().apply {
            this.user = user
            this.course = course
            this.syllabusTopic = syllabusTopic
            this.classDate = classDate
            this.title = request.title.trim()
            this.status = NoteStatus.DRAFT.name
        }

        return noteRepository.save(note).toResponse()
    }

    @Transactional(readOnly = true)
    fun getNotesByUser(userId: Long, courseId: Long?): List<NoteResponse> {
        val notes = if (courseId != null) {
            noteRepository.findByUserIdAndCourseId(userId, courseId)
        } else {
            noteRepository.findByUserId(userId)
        }
        return notes.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getNoteById(noteId: Long, userId: Long): NoteResponse = findOwnedNote(noteId, userId).toResponse()

    @Transactional
    fun deleteNote(noteId: Long, userId: Long) {
        noteRepository.delete(findOwnedNote(noteId, userId))
    }

    @Transactional
    fun updateNoteStatus(noteId: Long, userId: Long, newStatus: String) {
        val note = findOwnedNote(noteId, userId)
        val currentStatus = NoteStatus.valueOf(note.status)
        val targetStatus = NoteStatus.valueOf(newStatus)

        val isValidTransition = when (currentStatus) {
            NoteStatus.DRAFT -> targetStatus == NoteStatus.PROCESSING || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.PROCESSING -> targetStatus == NoteStatus.READY || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.READY -> targetStatus == NoteStatus.ARCHIVED
            NoteStatus.ARCHIVED -> targetStatus == NoteStatus.DRAFT
        }

        if (!isValidTransition) {
            throw IllegalStateException("Invalid status transition from $currentStatus to $targetStatus")
        }

        note.status = targetStatus.name
        noteRepository.save(note)
    }

    /**
     * Un apunte solo lo ve o lo borra su dueño. Si es de otra persona se responde 404 (no
     * 403), para no revelar que ese id existe.
     */
    private fun findOwnedNote(noteId: Long, userId: Long): Note {
        val note = noteRepository.findById(noteId).orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
        if (note.user.id != userId) throw ResourceNotFoundException("Apunte no encontrado")
        return note
    }
}
