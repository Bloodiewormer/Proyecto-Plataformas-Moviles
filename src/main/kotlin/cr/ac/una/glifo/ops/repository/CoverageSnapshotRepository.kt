package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.CoverageSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface CoverageSnapshotRepository : JpaRepository<CoverageSnapshot, Long> {
    /** Serie de cobertura de un estudiante en un curso, de la más vieja a la más nueva. */
    fun findByUserIdAndCourseIdOrderByTakenAtAsc(userId: Long, courseId: Long): List<CoverageSnapshot>

    /** La última foto antes de [before]: base para el "qué cambió desde…" (delta de cobertura). */
    fun findFirstByUserIdAndCourseIdAndTakenAtBeforeOrderByTakenAtDesc(
        userId: Long,
        courseId: Long,
        before: Instant
    ): CoverageSnapshot?
}
