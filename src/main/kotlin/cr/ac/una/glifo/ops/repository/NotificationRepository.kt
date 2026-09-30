package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdAndReadAtIsNull(userId: Long): List<Notification>
    fun findByUserId(userId: Long): List<Notification>
    fun countByUserIdAndReadAtIsNull(userId: Long): Long
}
