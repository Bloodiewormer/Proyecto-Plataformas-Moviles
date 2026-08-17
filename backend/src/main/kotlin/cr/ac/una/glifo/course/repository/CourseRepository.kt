package cr.ac.una.glifo.course.repository

import cr.ac.una.glifo.course.entity.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CourseRepository : JpaRepository<Course, Long> {
    fun findByJoinCode(joinCode: String): Course?
    fun findByOwnerId(ownerId: Long): List<Course>
}
