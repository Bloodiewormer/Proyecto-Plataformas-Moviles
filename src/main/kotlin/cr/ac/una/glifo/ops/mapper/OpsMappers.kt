package cr.ac.una.glifo.ops.mapper

import cr.ac.una.glifo.ops.dto.DeviceResponse
import cr.ac.una.glifo.ops.entity.Device

fun Device.toResponse(): DeviceResponse =
    DeviceResponse(
        id = id,
        userId = user?.id ?: 0L,
        installationId = installationId,
        platform = platform,
        active = active,
        tokenRegistered = !fcmToken.isNullOrBlank(),
        registeredAt = registeredAt.toString(),
        updatedAt = updatedAt.toString(),
    )
