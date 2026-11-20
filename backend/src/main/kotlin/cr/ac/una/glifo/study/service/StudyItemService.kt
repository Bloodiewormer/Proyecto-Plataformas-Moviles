package cr.ac.una.glifo.study.service

import cr.ac.una.glifo.ai.AiOrchestrator
import cr.ac.una.glifo.ai.ledger.AiCallOrigin
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
import cr.ac.una.glifo.note.entity.NoteStatus
import cr.ac.una.glifo.note.repository.NoteRepository
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.dto.StudyItemResponse
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.mapper.toResponse
import cr.ac.una.glifo.study.repository.StudyItemRepository
import cr.ac.una.glifo.study.repository.TopicCoverageRepository
import cr.ac.una.glifo.user.entity.User
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionOperations
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

data class StudyGenerationOutcome(
    val items: List<StudyItemResponse>,
    val generatedTopics: Int,
    val reusedTopics: Int,
    val generatedItems: Int
)

@Service
class StudyItemService @Autowired constructor(
    private val courseRepository: CourseRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
    private val studyItemRepository: StudyItemRepository,
    private val aiOrchestrator: AiOrchestrator,
    private val courseService: CourseService,
    private val userRepository: UserRepository? = null,
    private val noteRepository: NoteRepository? = null,
    private val topicCoverageRepository: TopicCoverageRepository? = null,
    private val transactions: TransactionOperations? = null,
    private val jsonMapper: JsonMapper? = null
) {

    constructor(
        courseRepository: CourseRepository,
        enrollmentRepository: EnrollmentRepository,
        syllabusTopicRepository: SyllabusTopicRepository,
        studyItemRepository: StudyItemRepository,
        generationService: GenerationService,
        courseService: CourseService
    ) : this(
        courseRepository = courseRepository,
        enrollmentRepository = enrollmentRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        studyItemRepository = studyItemRepository,
        aiOrchestrator = AiOrchestrator(generationService),
        courseService = courseService,
        userRepository = null,
        noteRepository = null,
        topicCoverageRepository = null,
        transactions = null,
        jsonMapper = null
    )

    constructor(
        courseRepository: CourseRepository,
        enrollmentRepository: EnrollmentRepository,
        syllabusTopicRepository: SyllabusTopicRepository,
        studyItemRepository: StudyItemRepository,
        generationService: GenerationService,
        courseService: CourseService,
        userRepository: UserRepository?,
        noteRepository: NoteRepository?,
        topicCoverageRepository: TopicCoverageRepository?,
        transactions: TransactionOperations? = null,
        jsonMapper: JsonMapper? = null
    ) : this(
        courseRepository = courseRepository,
        enrollmentRepository = enrollmentRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        studyItemRepository = studyItemRepository,
        aiOrchestrator = AiOrchestrator(generationService),
        courseService = courseService,
        userRepository = userRepository,
        noteRepository = noteRepository,
        topicCoverageRepository = topicCoverageRepository,
        transactions = transactions,
        jsonMapper = jsonMapper
    )

    /**
     * Genera ítems de estudio por lotes (IA-02) personalizados para el estudiante:
     * - Si regenerate=false, reutiliza los ítems existentes de ESE estudiante para esos temas.
     * - Si regenerate=true, reemplaza los ítems de ese estudiante para los temas solicitados.
     * - Incorpora el texto reconstruido de los apuntes del estudiante y su estado de cobertura.
     * - No mantiene una transacción abierta durante la llamada HTTP a OpenAI: consulta primero,
     *   llama a la IA fuera de transacción y luego persiste en una transacción corta.
     */
    fun generate(
        courseId: Long,
        request: GenerateStudyItemsRequest,
        userId: Long
    ): StudyGenerationOutcome {
        val course = requireCourseMember(courseId, userId)
        val courseTopicsById = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
            .associateBy { it.id }

        val requestedIds = request.topicIds.distinct()
        val invalidTopicId = requestedIds.firstOrNull { it !in courseTopicsById }
        if (invalidTopicId != null) {
            throw BadRequestException("El tema con id $invalidTopicId no pertenece a este curso")
        }

        val requestedTopics = requestedIds.map { courseTopicsById.getValue(it) }
        val reusedItems = mutableListOf<StudyItem>()
        val topicsToGenerate = mutableListOf<SyllabusTopic>()

        if (request.regenerate) {
            topicsToGenerate.addAll(requestedTopics)
        } else {
            for (topic in requestedTopics) {
                val existing = studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(userId, courseId, topic.id)
                if (existing.isNotEmpty()) {
                    reusedItems.addAll(existing)
                } else {
                    topicsToGenerate.add(topic)
                }
            }
        }

        if (topicsToGenerate.isEmpty()) {
            // Nada que generar: la llamada IA-02 queda como evitada en ai_avoided_calls (#46).
            aiOrchestrator.recordReusedStudyItems(AiCallOrigin(userId = userId, courseId = courseId))
            return StudyGenerationOutcome(
                items = reusedItems.map { it.toResponse() },
                generatedTopics = 0,
                reusedTopics = requestedTopics.size,
                generatedItems = 0
            )
        }

        val topicIdsToGenerate = topicsToGenerate.map { it.id }
        val coverageByTopic = topicCoverageRepository
            ?.findByUserIdAndSyllabusTopicIdIn(userId, topicIdsToGenerate)
            ?.associateBy { it.syllabusTopic?.id }
            .orEmpty()

        val userNotes = noteRepository
            ?.findByUserIdAndCourseId(userId, courseId)
            ?.filter { it.status == NoteStatus.READY.name }
            .orEmpty()

        val topicInputs = topicsToGenerate.map { topic ->
            val directNotes = userNotes.filter { it.syllabusTopic?.id == topic.id }
            val matchingNotes = if (directNotes.isNotEmpty()) {
                directNotes
            } else if (topic.parent != null) {
                userNotes.filter { it.syllabusTopic?.id == topic.parent?.id }
            } else {
                emptyList()
            }

            val extractedTexts = matchingNotes.mapNotNull { note ->
                note.content?.takeIf { it.isNotBlank() }?.let { extractNoteText(it) }
            }.filter { it.isNotBlank() }

            val combinedNotesText = if (extractedTexts.isNotEmpty()) {
                extractedTexts.joinToString("\n\n").take(MAX_NOTE_CONTEXT_CHARS)
            } else {
                null
            }

            val coverage = coverageByTopic[topic.id]?.state ?: "ABSENT"

            TopicInput(
                id = topic.id,
                code = topic.code,
                title = topic.title,
                coverageState = coverage,
                notesText = combinedNotesText
            )
        }

        // Llamada HTTP a OpenAI fuera de una transacción de base de datos.
        val generatedBatch = aiOrchestrator.generateStudyItems(
            courseName = course.name,
            topics = topicInputs,
            itemsPerTopic = request.itemsPerTopic,
            // El registro de ai_calls (#46) queda a nombre de quien pidió el material.
            origin = AiCallOrigin(userId = userId, courseId = courseId)
        )

        val userRef = userRepository?.getReferenceById(userId) ?: User().apply { id = userId }
        val entitiesToSave = generatedBatch.items.map { gen ->
            val topic = courseTopicsById.getValue(gen.topicId)
            StudyItem().apply {
                this.course = course
                this.syllabusTopic = topic
                this.user = userRef
                this.kind = gen.kind
                this.payload = gen.payload
            }
        }

        val txOps = transactions ?: TransactionOperations.withoutTransaction()
        val savedItems = txOps.execute {
            if (request.regenerate) {
                val oldItems = studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicIdIn(
                    userId,
                    courseId,
                    topicIdsToGenerate
                )
                if (oldItems.isNotEmpty()) {
                    studyItemRepository.deleteAll(oldItems)
                    studyItemRepository.flush()
                }
            }
            studyItemRepository.saveAll(entitiesToSave)
        } ?: emptyList()

        val generatedTopicIds = savedItems.mapNotNull { it.syllabusTopic?.id }.toSet()
        val allItems = reusedItems + savedItems
        return StudyGenerationOutcome(
            items = allItems.map { it.toResponse() },
            generatedTopics = generatedTopicIds.size,
            reusedTopics = requestedTopics.size - topicsToGenerate.size,
            generatedItems = savedItems.size
        )
    }

    /**
     * Consulta ítems de estudio filtrados por el usuario autenticado.
     * Los ítems antiguos sin usuario (legacy user_id IS NULL) no se devuelven a nadie.
     */
    @Transactional(readOnly = true)
    fun list(
        courseId: Long,
        topicId: Long?,
        userId: Long,
        isAdmin: Boolean = false
    ): List<StudyItemResponse> {
        courseService.findAccessibleCourse(courseId, userId, isAdmin)

        if (topicId != null) {
            val courseTopicsById = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
                .associateBy { it.id }
            if (topicId !in courseTopicsById) {
                throw BadRequestException("El tema con id $topicId no pertenece a este curso")
            }
            return studyItemRepository.findByUserIdAndCourseIdAndSyllabusTopicId(userId, courseId, topicId)
                .map { it.toResponse() }
        }

        return studyItemRepository.findByUserIdAndCourseId(userId, courseId).map { it.toResponse() }
    }

    private fun extractNoteText(content: String): String {
        val mapper = jsonMapper ?: JsonMapper.builder().findAndAddModules().build()
        val blocks = runCatching { mapper.readTree(content).path("blocks") }.getOrNull()
        if (blocks == null || !blocks.isArray) {
            return content.takeIf { !it.trim().startsWith("{") } ?: ""
        }
        val parts = mutableListOf<String>()
        for (block in blocks) {
            val synthesizedHeading = block.path("type").asString("") == "HEADING" && block.stringOrNull("regionId").isNullOrBlank()
            if (block.isUncertain() || synthesizedHeading) continue
            listOf("text", "latex", "caption").mapNotNullTo(parts) { block.stringOrNull(it) }
            block.path("spans").forEach { span -> if (!span.isUncertain()) span.stringOrNull("text")?.let(parts::add) }
        }
        return parts.joinToString("\n")
    }

    private fun JsonNode.isUncertain() =
        path("isUncertain").asBoolean(false) || path("resolvedAt").asString("").equals("UNRESOLVED", ignoreCase = true)

    private fun JsonNode.stringOrNull(field: String): String? = get(field)?.takeIf { it.isString }?.asString()

    private fun requireCourseMember(courseId: Long, userId: Long): Course {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResourceNotFoundException("Curso no encontrado") }

        val isOwner = course.owner?.id == userId
        val isEnrolled = enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)
        if (!isOwner && !isEnrolled) {
            throw AccessDeniedException("No tenés permiso para acceder a este curso")
        }
        return course
    }

    companion object {
        const val MAX_NOTE_CONTEXT_CHARS = 1500
    }
}
