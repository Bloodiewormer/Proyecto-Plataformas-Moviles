package cr.ac.una.glifo.course.repository

import cr.ac.una.glifo.course.entity.Enrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EnrollmentRepository : JpaRepository<Enrollment, Long> {
    fun findByUserId(userId: Long): List<Enrollment>
    fun findByUserIdAndCourseId(userId: Long, courseId: Long): Enrollment?
    fun existsByUserIdAndCourseId(userId: Long, courseId: Long): Boolean
}
