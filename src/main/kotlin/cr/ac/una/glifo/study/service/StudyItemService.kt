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
import cr.ac.una.glifo.study.dto.GenerateStudyItemsRequest
import cr.ac.una.glifo.study.dto.StudyItemResponse
import cr.ac.una.glifo.study.entity.StudyItem
import cr.ac.una.glifo.study.mapper.toResponse
import cr.ac.una.glifo.study.repository.StudyItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val aiOrchestrator: AiOrchestrator
) {

    constructor(
        courseRepository: CourseRepository,
        enrollmentRepository: EnrollmentRepository,
        syllabusTopicRepository: SyllabusTopicRepository,
        studyItemRepository: StudyItemRepository,
        generationService: GenerationService
    ) : this(
        courseRepository = courseRepository,
        enrollmentRepository = enrollmentRepository,
        syllabusTopicRepository = syllabusTopicRepository,
        studyItemRepository = studyItemRepository,
        aiOrchestrator = AiOrchestrator(generationService)
    )

    /**
     * Genera ítems de estudio por lotes (IA-02) de forma idempotente:
     * - Los temas que ya cuentan con ítems no se regeneran; se reutilizan los existentes.
     * - No mantiene una transacción abierta durante la llamada HTTP a OpenAI: consulta primero,
     *   llama a la IA fuera de transacción y luego persiste con [StudyItemRepository.saveAll].
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

        for (topic in requestedTopics) {
            val existing = studyItemRepository.findByCourseIdAndSyllabusTopicId(courseId, topic.id)
            if (existing.isNotEmpty()) {
                reusedItems.addAll(existing)
            } else {
                topicsToGenerate.add(topic)
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

        // Llamada HTTP a OpenAI fuera de una transacción de base de datos.
        val generatedBatch = aiOrchestrator.generateStudyItems(
            courseName = course.name,
            topics = topicsToGenerate.map { TopicInput(id = it.id, code = it.code, title = it.title) },
            itemsPerTopic = request.itemsPerTopic,
            // El registro de ai_calls (#46) queda a nombre de quien pidió el material.
            origin = AiCallOrigin(userId = userId, courseId = courseId)
        )

        val entitiesToSave = generatedBatch.items.map { gen ->
            val topic = courseTopicsById.getValue(gen.topicId)
            StudyItem().apply {
                this.course = course
                this.syllabusTopic = topic
                this.kind = gen.kind
                this.payload = gen.payload
            }
        }

        // Persistencia transaccional delegada al repositorio (SimpleJpaRepository.saveAll).
        val savedItems = studyItemRepository.saveAll(entitiesToSave)
        val generatedTopicIds = savedItems.mapNotNull { it.syllabusTopic?.id }.toSet()

        val allItems = reusedItems + savedItems
        return StudyGenerationOutcome(
            items = allItems.map { it.toResponse() },
            generatedTopics = generatedTopicIds.size,
            reusedTopics = requestedTopics.size - topicsToGenerate.size,
            generatedItems = savedItems.size
        )
    }

    @Transactional(readOnly = true)
    fun list(
        courseId: Long,
        topicId: Long?,
        userId: Long
    ): List<StudyItemResponse> {
        requireCourseMember(courseId, userId)

        if (topicId != null) {
            val courseTopicsById = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
                .associateBy { it.id }
            if (topicId !in courseTopicsById) {
                throw BadRequestException("El tema con id $topicId no pertenece a este curso")
            }
            return studyItemRepository.findByCourseIdAndSyllabusTopicId(courseId, topicId)
                .map { it.toResponse() }
        }

        return studyItemRepository.findByCourseId(courseId).map { it.toResponse() }
    }

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
}
