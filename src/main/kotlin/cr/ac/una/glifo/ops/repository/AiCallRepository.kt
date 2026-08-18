package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.AiCall
import org.springframework.data.jpa.repository.JpaRepository

interface AiCallRepository : JpaRepository<AiCall, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<AiCall>
    fun findByUserIdAndCallType(userId: Long, callType: String): List<AiCall>
}
