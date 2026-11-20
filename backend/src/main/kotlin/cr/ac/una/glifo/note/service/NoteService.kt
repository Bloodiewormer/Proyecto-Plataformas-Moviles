package cr.ac.una.glifo.note.service

import cr.ac.una.glifo.ai.AiOrchestrator
import cr.ac.una.glifo.ai.service.NoteReconstructionOutcome
import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.BusinessException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.note.dto.CreateNotePageRequest
import cr.ac.una.glifo.note.dto.CreateNoteRequest
import cr.ac.una.glifo.note.dto.NotePageResponse
import cr.ac.una.glifo.note.dto.NoteResponse
import cr.ac.una.glifo.note.dto.UpdateNoteRequest
import cr.ac.una.glifo.note.entity.Note
import cr.ac.una.glifo.note.entity.NotePage
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.mapper.toResponse
import cr.ac.una.glifo.note.repository.NotePageRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** ProcessingLevel (Glifo_UML_Modeling.md §5.7); coincide con el CHECK de note_pages. */
private val PROCESSING_LEVELS = setOf("N0", "N1", "N1_5", "N2", "N3", "UNRESOLVED")

@Service
class NoteService @Autowired constructor(
    private val noteRepository: NoteRepository,
    private val notePageRepository: NotePageRepository,
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val aiOrchestrator: AiOrchestrator? = null
) {

    constructor(
        noteRepository: NoteRepository,
        notePageRepository: NotePageRepository,
        userRepository: UserRepository,
        courseRepository: CourseRepository,
        syllabusTopicRepository: SyllabusTopicRepository
    ) : this(
        noteRepository = noteRepository,
        notePageRepository = notePageRepository,
        userRepository = userRepository,
        courseRepository = courseRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        aiOrchestrator = null
    )

    @Transactional
    fun createNote(userId: Long, request: CreateNoteRequest): NoteResponse {
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("Usuario no encontrado") }
        val course = courseRepository.findById(request.courseId).orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        val syllabusTopic = request.syllabusTopicId?.let {
            syllabusTopicRepository.findById(it).orElseThrow { ResourceNotFoundException("Tema del temario no encontrado") }
        }
        val note = Note().apply {
            this.user = user
            this.course = course
            this.syllabusTopic = syllabusTopic
            this.classDate = parseClassDate(request.classDate)
            this.title = request.title.trim()
            this.status = NoteStatus.DRAFT.name
        }

        return noteRepository.save(note).toResponse()
    }

    /** PUT: reemplaza título, fecha y tema. El curso y el estado no se editan por aquí. */
    @Transactional
    fun updateNote(noteId: Long, userId: Long, request: UpdateNoteRequest): NoteResponse {
        val note = findOwnedNote(noteId, userId)
        val syllabusTopic = request.syllabusTopicId?.let { topicId ->
            val topic = syllabusTopicRepository.findById(topicId)
                .orElseThrow { ResourceNotFoundException("Tema del temario no encontrado") }
            if (topic.course?.id != note.course.id) {
                throw BadRequestException("El tema no pertenece al curso del apunte")
            }
            topic
        }

        note.title = request.title.trim()
        note.classDate = parseClassDate(request.classDate)
        note.syllabusTopic = syllabusTopic
        return noteRepository.save(note).toResponse(includeContent = true)
    }

    /**
     * El DTO ya valida el formato AAAA-MM-DD; esto atrapa fechas con ese formato que no
     * existen en el calendario, como 2026-02-30.
     */
    private fun parseClassDate(value: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        throw BadRequestException("La fecha de la clase no es una fecha válida")
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
    fun getNoteById(noteId: Long, userId: Long): NoteResponse =
        findOwnedNote(noteId, userId).toResponse(includeContent = true)

    /**
     * Guarda una página que el teléfono ya procesó (issue #26). La cola offline reintenta,
     * así que la misma página puede llegar dos veces: el UNIQUE (note_id, page_index) la
     * convierte en 409, que el cliente trata como "ya estaba".
     */
    @Transactional
    fun addPage(noteId: Long, userId: Long, request: CreateNotePageRequest): NotePageResponse {
        val note = findOwnedNote(noteId, userId)
        if (request.levelReached !in PROCESSING_LEVELS) {
            throw BusinessException("Nivel de procesamiento inválido: ${request.levelReached}", "VALIDATION_FAILED")
        }
        if (notePageRepository.existsByNoteIdAndPageIndex(noteId, request.pageIndex)) {
            throw DuplicateResourceException("La página ${request.pageIndex} de este apunte ya se subió")
        }

        val page = NotePage().apply {
            this.pageIndex = request.pageIndex
            this.perceptualHash = request.perceptualHash
            this.storageUri = request.storageUri ?: "device://${request.perceptualHash}"
            this.levelReached = request.levelReached
            this.overallConfidence = request.overallConfidence
            this.regions = request.regions?.toString()
            this.qualityMetrics = request.qualityMetrics?.toString()
            this.processedAt = Instant.now()
        }
        note.addPage(page)
        // Con la primera página el apunte deja de ser un borrador vacío (D-16).
        if (note.status == NoteStatus.DRAFT.name) note.status = NoteStatus.PROCESSING.name

        return notePageRepository.save(page).toResponse()
    }

    @Transactional(readOnly = true)
    fun getPages(noteId: Long, userId: Long): List<NotePageResponse> {
        findOwnedNote(noteId, userId)
        return notePageRepository.findByNoteIdOrderByPageIndex(noteId).map { it.toResponse() }
    }

    @Transactional
    fun deleteNote(noteId: Long, userId: Long) {
        noteRepository.delete(findOwnedNote(noteId, userId))
    }

    /**
     * Cambia el estado respetando el ciclo de vida del apunte. Un estado desconocido es un
     * 400; una transición no permitida es un 409, porque la petición es válida pero choca
     * con el estado actual.
     */
    @Transactional
    fun updateNoteStatus(noteId: Long, userId: Long, newStatus: String): NoteResponse {
        val note = findOwnedNote(noteId, userId)
        val currentStatus = NoteStatus.valueOf(note.status)
        val targetStatus = NoteStatus.entries.firstOrNull { it.name == newStatus }
            ?: throw BadRequestException("Estado desconocido: $newStatus")

        val isValidTransition = when (currentStatus) {
            NoteStatus.DRAFT -> targetStatus == NoteStatus.PROCESSING || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.PROCESSING -> targetStatus == NoteStatus.READY || targetStatus == NoteStatus.ARCHIVED
            NoteStatus.READY -> targetStatus == NoteStatus.ARCHIVED
            NoteStatus.ARCHIVED -> targetStatus == NoteStatus.DRAFT
        }

        if (!isValidTransition) {
            throw ConflictException("No se puede pasar un apunte de $currentStatus a $targetStatus")
        }

        note.status = targetStatus.name
        return noteRepository.save(note).toResponse(includeContent = true)
    }

    /**
     * IA-01: Reconstruye el apunte a partir de las regiones de sus páginas.
     * No abre una transacción global para no mantener conexiones durante la llamada HTTP a OpenAI.
     */
    fun reconstructNote(noteId: Long, userId: Long): NoteReconstructionOutcome {
        val orchestrator = aiOrchestrator
            ?: throw IllegalStateException("AiOrchestrator no configurado")
        return orchestrator.reconstructNote(noteId, userId)
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
