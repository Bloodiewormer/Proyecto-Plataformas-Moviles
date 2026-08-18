package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdAndReadAtIsNull(userId: Long): List<Notification>
}
