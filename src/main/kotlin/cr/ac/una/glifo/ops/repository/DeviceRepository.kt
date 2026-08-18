package cr.ac.una.glifo.ops.repository

import cr.ac.una.glifo.ops.entity.Device
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceRepository : JpaRepository<Device, Long> {
    fun findByUserId(userId: Long): List<Device>
    fun findByFcmToken(fcmToken: String): Device?
}
