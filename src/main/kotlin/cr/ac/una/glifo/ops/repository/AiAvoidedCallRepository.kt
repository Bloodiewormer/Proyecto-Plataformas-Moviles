package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.AiAvoidedCall
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/** Llamadas evitadas de un tipo (ai_avoided_calls). */
data class AiAvoidedRow(
    val callType: String,
    val avoidedCalls: Long
)

@Repository
interface AiAvoidedCallRepository : JpaRepository<AiAvoidedCall, Long> {

    /** Evitadas del usuario; [courseId] null = todos los cursos (y las que no tienen curso). */
    @Query(
        """
        SELECT new cr.ac.una.glifo.ops.repository.AiAvoidedRow(a.callType, COUNT(a))
        FROM AiAvoidedCall a
        WHERE a.user.id = :userId AND (:courseId IS NULL OR a.course.id = :courseId)
        GROUP BY a.callType ORDER BY a.callType
        """
    )
    fun avoidedByUser(@Param("userId") userId: Long, @Param("courseId") courseId: Long?): List<AiAvoidedRow>

    /** Evitadas de todos los usuarios dentro de un curso, solo agregadas. */
    @Query(
        """
        SELECT new cr.ac.una.glifo.ops.repository.AiAvoidedRow(a.callType, COUNT(a))
        FROM AiAvoidedCall a
        WHERE a.course.id = :courseId
        GROUP BY a.callType ORDER BY a.callType
        """
    )
    fun avoidedByCourse(@Param("courseId") courseId: Long): List<AiAvoidedRow>
}
