package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdAndReadAtIsNull(userId: Long): List<Notification>
    fun findByUserId(userId: Long): List<Notification>
    fun countByUserIdAndReadAtIsNull(userId: Long): Long
    fun findByIdempotencyKey(idempotencyKey: UUID): Notification?
    fun findByDeviceIdOrderByCreatedAtDesc(deviceId: Long): List<Notification>
}
