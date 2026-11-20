package cr.ac.una.glifo.ops.dto

import com.fasterxml.jackson.annotation.JsonProperty
import cr.ac.una.glifo.ops.fcm.TokenSanitizer
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class RegisterDeviceRequest(
    @field:NotBlank(message = "El installation_id es obligatorio")
    @field:Size(min = 8, max = 64, message = "El installation_id debe tener entre 8 y 64 caracteres")
    @JsonProperty("installation_id")
    val installationId: String,

    @field:Size(min = 10, max = 512, message = "El fcm_token debe tener entre 10 y 512 caracteres")
    @JsonProperty("fcm_token")
    val fcmToken: String? = null,

    @field:NotBlank(message = "La plataforma es obligatoria")
    @field:Pattern(regexp = "ANDROID|IOS|WEB", message = "La plataforma debe ser ANDROID, IOS o WEB")
    val platform: String = "ANDROID",
) {
    override fun toString(): String =
        "RegisterDeviceRequest(installationId='$installationId', fcmToken='${TokenSanitizer.mask(fcmToken)}', platform='$platform')"
}

data class UpdateDeviceTokenRequest(
    @field:NotBlank(message = "El fcm_token es obligatorio")
    @field:Size(min = 10, max = 512, message = "El fcm_token debe tener entre 10 y 512 caracteres")
    @JsonProperty("fcm_token")
    val fcmToken: String,
) {
    override fun toString(): String =
        "UpdateDeviceTokenRequest(fcmToken='${TokenSanitizer.mask(fcmToken)}')"
}

data class DeviceResponse(
    val id: Long,
    @get:JsonProperty("user_id")
    val userId: Long,
    @get:JsonProperty("installation_id")
    val installationId: String,
    val platform: String,
    @get:JsonProperty("is_active")
    val active: Boolean,
    @get:JsonProperty("token_registered")
    val tokenRegistered: Boolean,
    @get:JsonProperty("registered_at")
    val registeredAt: String,
    @get:JsonProperty("updated_at")
    val updatedAt: String,
)

data class SyncCompleteRequest(
    @field:Positive(message = "El device_id debe ser positivo")
    @JsonProperty("device_id")
    val deviceId: Long,

    @field:NotBlank(message = "El idempotency_key es obligatorio")
    @field:Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "El idempotency_key debe ser un UUID válido",
    )
    @JsonProperty("idempotency_key")
    val idempotencyKey: String,

    @field:Min(value = 0, message = "uploaded_count no puede ser negativo")
    @field:Max(value = 10000, message = "uploaded_count excede el límite permitido")
    @JsonProperty("uploaded_count")
    val uploadedCount: Int,

    @field:Min(value = 0, message = "pending_count no puede ser negativo")
    @JsonProperty("pending_count")
    val pendingCount: Int = 0,

    @field:Min(value = 0, message = "failed_count no puede ser negativo")
    @JsonProperty("failed_count")
    val failedCount: Int = 0,

    @field:Size(max = 500, message = "note_ids no puede superar 500 elementos")
    @JsonProperty("note_ids")
    val noteIds: List<Long> = emptyList(),
)

data class SyncCompleteResponse(
    @get:JsonProperty("sync_entry_id")
    val syncEntryId: Long?,
    @get:JsonProperty("notification_id")
    val notificationId: Long?,
    @get:JsonProperty("device_id")
    val deviceId: Long,
    @get:JsonProperty("idempotency_key")
    val idempotencyKey: String,
    @get:JsonProperty("queue_status")
    val queueStatus: String,
    @get:JsonProperty("notification_status")
    val notificationStatus: String,
    val duplicate: Boolean,
    @get:JsonProperty("uploaded_count")
    val uploadedCount: Int,
    @get:JsonProperty("completed_at")
    val completedAt: String,
)
