package cr.ac.una.glifo.study.repository

import cr.ac.una.glifo.study.entity.ReviewSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ReviewScheduleRepository : JpaRepository<ReviewSchedule, Long> {
    fun findByUserIdAndDueAtBefore(userId: Long, before: Instant): List<ReviewSchedule>
}
