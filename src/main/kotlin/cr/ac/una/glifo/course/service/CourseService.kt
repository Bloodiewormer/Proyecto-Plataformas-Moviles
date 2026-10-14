package cr.ac.una.glifo.course.service

import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.CreateCourseRequest
import cr.ac.una.glifo.course.dto.CreateSyllabusTopicRequest
import cr.ac.una.glifo.course.dto.EnrollmentResponse
import cr.ac.una.glifo.course.dto.PublishSyllabusRequest
import cr.ac.una.glifo.course.dto.SyllabusTopicResponse
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.entity.SyllabusTopic
import cr.ac.una.glifo.course.mapper.toResponse
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.course.repository.SyllabusTopicRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val userRepository: UserRepository,
    private val syllabusTopicRepository: SyllabusTopicRepository,
) {

    @Transactional
    fun createCourse(request: CreateCourseRequest, ownerId: Long): CourseResponse {
        val owner = userRepository.findById(ownerId)
            .orElseThrow { ResourceNotFoundException("User", ownerId) }

        val course = Course().apply {
            this.owner = owner
            this.code = request.code
            this.name = request.name
            this.term = request.term
            this.joinCode = UUID.randomUUID().toString().substring(0, 8).uppercase()
        }

        // El docente queda ligado por courses.owner_user_id, no por una matrícula:
        // enrollments.status solo admite ACTIVE, PENDING o REMOVED.
        return courseRepository.save(course).toResponse()
    }

    @Transactional(readOnly = true)
    fun getUserCourses(userId: Long): List<CourseResponse> {
        val enrollments = enrollmentRepository.findByUserId(userId)
        val enrolledCourses = enrollments.mapNotNull { it.course?.toResponse() }
        
        val ownedCourses = courseRepository.findByOwnerId(userId).map { it.toResponse() }
        
        return (enrolledCourses + ownedCourses).distinctBy { it.id }
    }

    @Transactional(readOnly = true)
    fun getCourseById(id: Long): CourseResponse {
        val course = courseRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Curso no encontrado") }
        return course.toResponse()
    }

    @Transactional
    fun joinCourse(joinCode: String, userId: Long): EnrollmentResponse {
        val course = courseRepository.findByJoinCode(joinCode.trim())
            ?: throw ResourceNotFoundException("Código de curso inválido")

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, course.id)) {
            throw DuplicateResourceException("Ya estás matriculado en este curso")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User", userId) }

        val enrollment = Enrollment().apply {
            this.user = user
            this.course = course
        }

        val savedEnrollment = enrollmentRepository.save(enrollment)
        return savedEnrollment.toResponse()
    }

    @Transactional
    fun publishSyllabus(
        courseId: Long,
        request: PublishSyllabusRequest,
        userId: Long
    ): List<SyllabusTopicResponse> {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResourceNotFoundException("Curso no encontrado") }

        if (course.owner?.id != userId) {
            throw AccessDeniedException("Solo el docente dueño del curso puede publicar el temario")
        }

        if (course.syllabusParsedAt != null) {
            throw DuplicateResourceException("El temario de este curso ya fue publicado")
        }

        val existingTopics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val savedByCode = existingTopics.associateBy { it.code }.toMutableMap()
        val savedById = existingTopics.associateBy { it.id }.toMutableMap()

        request.topics.forEachIndexed { index, topicReq ->
            val parentTopic = when {
                topicReq.parentId != null -> savedById[topicReq.parentId]
                    ?: throw ResourceNotFoundException("Tema padre no encontrado")
                !topicReq.parentCode.isNullOrBlank() -> savedByCode[topicReq.parentCode]
                    ?: throw ResourceNotFoundException("Tema padre '${topicReq.parentCode}' no encontrado")
                else -> null
            }

            val trimmedCode = topicReq.code.trim()
            val topic = (savedByCode[trimmedCode] ?: SyllabusTopic()).apply {
                this.course = course
                this.parent = parentTopic
                this.code = trimmedCode
                this.title = topicReq.title.trim()
                this.orderIndex = if (topicReq.orderIndex > 0) topicReq.orderIndex else index + 1
            }

            val saved = syllabusTopicRepository.save(topic)
            savedByCode[saved.code] = saved
            savedById[saved.id] = saved
        }

        course.syllabusSourceUri = request.sourceUri ?: "syllabus-${course.code}.json"
        course.syllabusParsedAt = Instant.now()
        // La app compara esta versión con su copia offline del contexto del curso (D-17).
        course.syllabusVersion += 1
        courseRepository.save(course)

        return syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getCourseTopics(courseId: Long): List<SyllabusTopicResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw ResourceNotFoundException("Curso no encontrado")
        }
        return syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId).map { it.toResponse() }
    }

    @Transactional
    fun addSyllabusTopic(
        courseId: Long,
        request: CreateSyllabusTopicRequest,
        userId: Long
    ): SyllabusTopicResponse {
        val course = courseRepository.findById(courseId)
            .orElseThrow { ResourceNotFoundException("Curso no encontrado") }

        if (course.owner?.id != userId) {
            throw AccessDeniedException("Solo el docente dueño del curso puede modificar el temario")
        }

        val existingTopics = syllabusTopicRepository.findByCourseIdOrderByOrderIndex(courseId)
        val parentTopic = when {
            request.parentId != null -> existingTopics.find { it.id == request.parentId }
                ?: throw ResourceNotFoundException("Tema padre no encontrado")
            !request.parentCode.isNullOrBlank() -> existingTopics.find { it.code == request.parentCode }
                ?: throw ResourceNotFoundException("Tema padre '${request.parentCode}' no encontrado")
            else -> null
        }

        val nextOrderIndex = if (request.orderIndex > 0) {
            request.orderIndex
        } else {
            (existingTopics.maxOfOrNull { it.orderIndex } ?: 0) + 1
        }

        val topic = SyllabusTopic().apply {
            this.course = course
            this.parent = parentTopic
            this.code = request.code.trim()
            this.title = request.title.trim()
            this.orderIndex = nextOrderIndex
        }

        val saved = syllabusTopicRepository.save(topic)
        course.syllabusVersion += 1
        courseRepository.save(course)
        return saved.toResponse()
    }
}

