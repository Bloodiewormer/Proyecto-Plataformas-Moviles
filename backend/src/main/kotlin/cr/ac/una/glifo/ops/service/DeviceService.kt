package cr.ac.una.glifo.ops.service

import cr.ac.una.glifo.common.exception.BadRequestException
import cr.ac.una.glifo.common.exception.ResourceNotFoundException
import cr.ac.una.glifo.ops.dto.DeviceResponse
import cr.ac.una.glifo.ops.dto.RegisterDeviceRequest
import cr.ac.una.glifo.ops.dto.UpdateDeviceTokenRequest
import cr.ac.una.glifo.ops.entity.Device
import cr.ac.una.glifo.ops.fcm.TokenSanitizer
import cr.ac.una.glifo.ops.mapper.toResponse
import cr.ac.una.glifo.ops.repository.DeviceRepository
import cr.ac.una.glifo.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class DeviceRegistrationOutcome(
    val device: DeviceResponse,
    val created: Boolean,
)

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val userRepository: UserRepository,
) {

    private val log = LoggerFactory.getLogger(DeviceService::class.java)

    /**
     * Registra o reconcilia un dispositivo para el usuario autenticado.
     *
     * - Identifica el registro del dispositivo por `(user_id, installation_id)` para que
     *   su `device_id` (`id`) permanezca estable cuando rota el `fcm_token`.
     * - Si el mismo `fcm_token` estaba asociado a otro registro (por ejemplo, otra cuenta
     *   que inició sesión antes en el mismo teléfono físico), desvincula primero el token
     *   del registro anterior (`fcmToken = null`, `active = false`) sin borrar la fila
     *   para preservar la integridad referencial de `sync_queue` y `notifications`.
     */
    @Transactional
    fun registerOrUpdateDevice(userId: Long, request: RegisterDeviceRequest): DeviceRegistrationOutcome {
        val installationId = request.installationId.trim()
        if (installationId.length < 8) {
            throw BadRequestException("El installation_id debe tener al menos 8 caracteres")
        }

        val normalizedToken = request.fcmToken?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedToken != null && normalizedToken.length < 10) {
            throw BadRequestException("El fcm_token debe tener al menos 10 caracteres")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("Usuario no encontrado") }

        val now = Instant.now()
        val existingForInstallation = deviceRepository.findByUserIdAndInstallationId(userId, installationId)

        if (normalizedToken != null) {
            detachTokenFromOtherDeviceIfPresent(normalizedToken, existingForInstallation?.id, now)
        }

        if (existingForInstallation != null) {
            existingForInstallation.platform = request.platform
            if (normalizedToken != null) {
                existingForInstallation.fcmToken = normalizedToken
                existingForInstallation.active = true
            }
            existingForInstallation.updatedAt = now
            val saved = deviceRepository.saveAndFlush(existingForInstallation)
            log.info(
                "Dispositivo reconciliado (deviceId={}, userId={}, installationId={}, token={})",
                saved.id,
                userId,
                installationId,
                TokenSanitizer.mask(saved.fcmToken),
            )
            return DeviceRegistrationOutcome(device = saved.toResponse(), created = false)
        }

        val created = Device().apply {
            this.user = user
            this.installationId = installationId
            this.fcmToken = normalizedToken
            this.platform = request.platform
            this.active = normalizedToken != null
            this.registeredAt = now
            this.updatedAt = now
        }
        val saved = deviceRepository.saveAndFlush(created)
        log.info(
            "Dispositivo registrado (deviceId={}, userId={}, installationId={}, token={})",
            saved.id,
            userId,
            installationId,
            TokenSanitizer.mask(saved.fcmToken),
        )
        return DeviceRegistrationOutcome(device = saved.toResponse(), created = true)
    }

    /**
     * Actualiza el token FCM de un dispositivo ya registrado conservando su `device_id`.
     * Rechaza con 403 si el `device_id` pertenece a otro usuario.
     */
    @Transactional
    fun updateDeviceToken(userId: Long, deviceId: Long, request: UpdateDeviceTokenRequest): DeviceResponse {
        val device = findOwnedDeviceOrThrow(userId, deviceId)
        val normalizedToken = request.fcmToken.trim()
        if (normalizedToken.length < 10) {
            throw BadRequestException("El fcm_token debe tener al menos 10 caracteres")
        }

        val now = Instant.now()
        detachTokenFromOtherDeviceIfPresent(normalizedToken, device.id, now)

        device.fcmToken = normalizedToken
        device.active = true
        device.updatedAt = now
        val saved = deviceRepository.saveAndFlush(device)
        log.info(
            "Token FCM rotado para dispositivo (deviceId={}, userId={}, token={})",
            saved.id,
            userId,
            TokenSanitizer.mask(saved.fcmToken),
        )
        return saved.toResponse()
    }

    /**
     * Desvincula el token FCM del dispositivo al cerrar sesión.
     * No elimina la fila de `devices` porque `sync_queue` y `notifications` la referencian.
     */
    @Transactional
    fun unlinkDevice(userId: Long, deviceId: Long) {
        val device = findOwnedDeviceOrThrow(userId, deviceId)
        device.fcmToken = null
        device.active = false
        device.updatedAt = Instant.now()
        deviceRepository.saveAndFlush(device)
        log.info("Dispositivo desvinculado por cierre de sesión (deviceId={}, userId={})", deviceId, userId)
    }

    @Transactional(readOnly = true)
    fun getUserDevices(userId: Long): List<DeviceResponse> =
        deviceRepository.findByUserId(userId).map { it.toResponse() }

    private fun findOwnedDeviceOrThrow(userId: Long, deviceId: Long): Device {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { ResourceNotFoundException("Dispositivo no encontrado") }
        if (device.user?.id != userId) {
            throw AccessDeniedException("No tenés permiso para operar sobre este dispositivo")
        }
        return device
    }

    private fun detachTokenFromOtherDeviceIfPresent(fcmToken: String, keepDeviceId: Long?, now: Instant) {
        val holder = deviceRepository.findByFcmToken(fcmToken) ?: return
        if (keepDeviceId != null && holder.id == keepDeviceId) return

        val previousUserId = holder.user?.id
        holder.fcmToken = null
        holder.active = false
        holder.updatedAt = now
        deviceRepository.saveAndFlush(holder)
        log.info(
            "Token FCM desvinculado del dispositivo previo (previousDeviceId={}, previousUserId={}, token={})",
            holder.id,
            previousUserId,
            TokenSanitizer.mask(fcmToken),
        )
    }
}
