package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.Device
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeviceRepository : JpaRepository<Device, Long> {
    fun findByUserId(userId: Long): List<Device>
    fun findByFcmToken(fcmToken: String): Device?
    fun existsByFcmToken(fcmToken: String): Boolean
}
