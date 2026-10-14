package cr.ac.una.glifo.glossary.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ConflictException
import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.glossary.dto.CourseGlossaryResponse
import cr.ac.una.glifo.glossary.dto.CreateGlossaryEntryRequest
import cr.ac.una.glifo.glossary.dto.CreateGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.dto.GlossaryEntryResponse
import cr.ac.una.glifo.glossary.dto.GlossarySuggestionResponse
import cr.ac.una.glifo.glossary.dto.ResolveGlossarySuggestionRequest
import cr.ac.una.glifo.glossary.entity.CourseGlossary
import cr.ac.una.glifo.glossary.entity.GlossarySuggestion
import cr.ac.una.glifo.glossary.mapper.toGlossaryResponse
import cr.ac.una.glifo.glossary.mapper.toResponse
import cr.ac.una.glifo.glossary.repository.CourseGlossaryRepository
import cr.ac.una.glifo.glossary.repository.GlossarySuggestionRepository
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** SuggestionStatus (Glifo_UML_Modeling.md §5.7); coincide con el CHECK de glossary_suggestions. */
private val SUGGESTION_STATUSES = setOf("PENDING", "APPROVED", "REJECTED")

/**
 * Glosario canónico de notación del curso y moderación de sugerencias (issue #33, D-15).
 *
 * Reglas:
 * - Leer el glosario: cualquier miembro del curso (matriculado o docente dueño). Si no es
 *   miembro, 404, para no revelar que el curso existe.
 * - Escribir el glosario y moderar: solo el docente dueño. El privilegio GLOSSARY_WRITE lo
 *   revisa el controller; aquí se revisa que además sea SU curso (403 si no lo es).
 * - El estudiante nunca escribe en course_glossary: deja una sugerencia PENDING que el
 *   docente aprueba (y entonces sí entra al glosario) o rechaza.
 * - Cada cambio del glosario sube courses.glossary_version, así la app sabe que su copia
 *   offline quedó vieja (D-17).
 */
@Service
class GlossaryService(
    private val courseRepository: CourseRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val glossaryRepository: CourseGlossaryRepository,
    private val suggestionRepository: GlossarySuggestionRepository,
    private val userRepository: UserRepository,
    private val noteRepository: NoteRepository
) {

    @Transactional(readOnly = true)
    fun getGlossary(courseId: Long, userId: Long): CourseGlossaryResponse {
        val course = findMemberCourse(courseId, userId)
        return course.toGlossaryResponse(glossaryRepository.findByCourseIdOrderByTerm(courseId))
    }

    @Transactional
    fun addEntry(courseId: Long, userId: Long, request: CreateGlossaryEntryRequest): GlossaryEntryResponse {
        val course = findOwnedCourse(courseId, userId)
        val entry = createEntry(course, request.term.trim(), request.canonicalForm.trim(), request.kind)
            ?: throw DuplicateResourceException("El término '${request.term.trim()}' ya está en el glosario como ${request.kind}")
        return entry.toResponse()
    }

    @Transactional
    fun deleteEntry(courseId: Long, entryId: Long, userId: Long) {
        val course = findOwnedCourse(courseId, userId)
        val entry = glossaryRepository.findByIdAndCourseId(entryId, courseId)
            ?: throw ResourceNotFoundException("Entrada del glosario no encontrada")
        glossaryRepository.delete(entry)
        bumpGlossaryVersion(course)
    }

    @Transactional
    fun suggest(courseId: Long, userId: Long, request: CreateGlossarySuggestionRequest): GlossarySuggestionResponse {
        val course = findMemberCourse(courseId, userId)
        val user = userRepository.findById(userId).orElseThrow { ResourceNotFoundException("Usuario no encontrado") }
        val note = request.noteId?.let { noteId ->
            val note = noteRepository.findById(noteId).orElseThrow { ResourceNotFoundException("Apunte no encontrado") }
            // Solo se puede citar un apunte propio y de este mismo curso.
            if (note.user.id != userId || note.course.id != courseId) {
                throw ResourceNotFoundException("Apunte no encontrado")
            }
            note
        }

        val suggestion = GlossarySuggestion().apply {
            this.course = course
            this.user = user
            this.note = note
            this.originalText = request.originalText.trim()
            this.suggestedCorrection = request.suggestedCorrection.trim()
            this.status = "PENDING"
        }
        return suggestionRepository.save(suggestion).toResponse()
    }

    /** Por defecto solo las PENDING, que son las que el docente tiene que atender. */
    @Transactional(readOnly = true)
    fun getSuggestions(courseId: Long, userId: Long, status: String?): List<GlossarySuggestionResponse> {
        findOwnedCourse(courseId, userId)
        val suggestions = when {
            status == null -> suggestionRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, "PENDING")
            status == "ALL" -> suggestionRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
            status in SUGGESTION_STATUSES -> suggestionRepository.findByCourseIdAndStatusOrderByCreatedAtDesc(courseId, status)
            else -> throw BadRequestException("Estado desconocido: $status")
        }
        return suggestions.map { it.toResponse() }
    }

    /**
     * Aprobar agrega la corrección al glosario (término = texto original, forma canónica =
     * corrección). Si esa entrada ya existía no se duplica: la sugerencia igual queda aprobada.
     * Una sugerencia ya resuelta no se puede volver a resolver (409).
     */
    @Transactional
    fun resolveSuggestion(
        courseId: Long,
        suggestionId: Long,
        userId: Long,
        request: ResolveGlossarySuggestionRequest
    ): GlossarySuggestionResponse {
        val course = findOwnedCourse(courseId, userId)
        val suggestion = suggestionRepository.findByIdAndCourseId(suggestionId, courseId)
            ?: throw ResourceNotFoundException("Sugerencia no encontrada")
        if (suggestion.status != "PENDING") {
            throw ConflictException("La sugerencia ya fue resuelta (${suggestion.status})")
        }

        if (request.status == "APPROVED") {
            createEntry(course, suggestion.originalText, suggestion.suggestedCorrection, request.kind)
        }
        suggestion.status = request.status
        suggestion.resolvedAt = Instant.now()
        return suggestionRepository.save(suggestion).toResponse()
    }

    /** Devuelve null si ya existe el mismo término con el mismo tipo en el curso. */
    private fun createEntry(course: Course, term: String, canonicalForm: String, kind: String): CourseGlossary? {
        if (glossaryRepository.existsByCourseIdAndTermIgnoreCaseAndKind(course.id, term, kind)) return null
        val entry = CourseGlossary().apply {
            this.course = course
            this.term = term
            this.canonicalForm = canonicalForm
            this.kind = kind
        }
        val saved = glossaryRepository.save(entry)
        bumpGlossaryVersion(course)
        return saved
    }

    private fun bumpGlossaryVersion(course: Course) {
        course.glossaryVersion += 1
        courseRepository.save(course)
    }

    private fun findMemberCourse(courseId: Long, userId: Long): Course {
        val course = courseRepository.findById(courseId).orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        if (course.owner?.id != userId && !enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw ResourceNotFoundException("Curso no encontrado")
        }
        return course
    }

    /** Un docente con GLOSSARY_WRITE pero que no es dueño del curso recibe 403. */
    private fun findOwnedCourse(courseId: Long, userId: Long): Course {
        val course = courseRepository.findById(courseId).orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        if (course.owner?.id != userId) {
            throw AccessDeniedException("Solo el docente del curso puede modificar su glosario")
        }
        return course
    }
}
