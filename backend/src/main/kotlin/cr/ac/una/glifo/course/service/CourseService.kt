package cr.ac.una.glifo.course.service

import cr.ac.una.glifo.common.exception.DuplicateResourceException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.course.dto.CourseResponse
import cr.ac.una.glifo.course.dto.CreateCourseRequest
import cr.ac.una.glifo.course.dto.EnrollmentResponse
import cr.ac.una.glifo.course.entity.Course
import cr.ac.una.glifo.course.entity.Enrollment
import cr.ac.una.glifo.course.mapper.toResponse
import cr.ac.una.glifo.course.repository.CourseRepository
import cr.ac.una.glifo.course.repository.EnrollmentRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val userRepository: UserRepository,
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

        val savedCourse = courseRepository.save(course)

        val enrollment = Enrollment().apply {
            this.user = owner
            this.course = savedCourse
            this.status = "OWNER"
        }
        enrollmentRepository.save(enrollment)

        return savedCourse.toResponse()
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
            .orElseThrow { ResourceNotFoundException("Course", id) }
        return course.toResponse()
    }

    @Transactional
    fun joinCourse(joinCode: String, userId: Long): EnrollmentResponse {
        val course = courseRepository.findByJoinCode(joinCode)
            ?: throw ResourceNotFoundException("Course", joinCode)

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, course.id)) {
            throw DuplicateResourceException("Enrollment", "userId and courseId")
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
}
