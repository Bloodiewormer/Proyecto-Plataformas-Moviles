package cr.ac.una.glifo.study.repository

import cr.ac.una.glifo.study.entity.Attempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AttemptRepository : JpaRepository<Attempt, Long> {
    fun findByUserIdAndStudyItemId(userId: Long, studyItemId: Long): List<Attempt>
}
