package cr.ac.una.glifo.note.service

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

@Service
class NoteService(
    private val noteRepository: NoteRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository
) {

    @Transactional
    fun createNote(userId: Long, request: CreateNoteRequest): NoteResponse {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val course = courseRepository.findById(request.courseId).orElseThrow { IllegalArgumentException("Course not found") }
        val syllabusTopic = request.syllabusTopicId?.let {
            syllabusTopicRepository.findById(it).orElseThrow { IllegalArgumentException("SyllabusTopic not found") }
        }

        val note = Note().apply {
            this.user = user
            this.course = course
            this.syllabusTopic = syllabusTopic
            this.classDate = LocalDate.parse(request.classDate)
            this.title = request.title
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
    fun getNoteById(noteId: Long): NoteResponse {
        val note = noteRepository.findById(noteId).orElseThrow { IllegalArgumentException("Note not found") }
        return note.toResponse()
    }

    @Transactional
    fun updateNoteStatus(noteId: Long, newStatus: String) {
        val note = noteRepository.findById(noteId).orElseThrow { IllegalArgumentException("Note not found") }
        val currentStatus = NoteStatus.valueOf(note.status)
        val targetStatus = NoteStatus.valueOf(newStatus)

        val isValidTransition = when (currentStatus) {
            NoteStatus.DRAFT -> targetStatus == NoteStatus.PROCESSING || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.PROCESSING -> targetStatus == NoteStatus.READY || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.READY -> targetStatus == NoteStatus.ARCHIVED
            NoteStatus.ARCHIVED -> targetStatus == NoteStatus.DRAFT
        }

        if (!isValidTransition) {
            throw IllegalStateException("Invalid status transition from \$currentStatus to \$targetStatus")
        }

        note.status = targetStatus.name
        noteRepository.save(note)
    }
}
