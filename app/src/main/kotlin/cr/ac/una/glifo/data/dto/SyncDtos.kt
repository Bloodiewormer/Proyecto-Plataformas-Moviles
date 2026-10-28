package cr.ac.una.glifo.data.dto

import com.google.gson.annotations.SerializedName

data class SyncQueueDto(
    @SerializedName("id") val id: Long,
    @SerializedName("entity_type") val entityType: String,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("payload") val payload: String,
    @SerializedName("attempts") val attempts: Int,
    @SerializedName("last_error") val lastError: String?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: Long
)

data class SyncStatusResponse(
    @SerializedName("pending_count") val pendingCount: Int,
    @SerializedName("is_syncing") val isSyncing: Boolean,
    @SerializedName("items") val items: List<SyncQueueDto> = emptyList()
)

data class RegisterDeviceRequest(
    @SerializedName("installation_id") val installationId: String,
    @SerializedName("platform") val platform: String = "ANDROID",
    @SerializedName("fcm_token") val fcmToken: String? = null
)

data class UpdateDeviceTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

data class DeviceResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("installation_id") val installationId: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("active") val active: Boolean,
    @SerializedName("has_fcm_token") val hasFcmToken: Boolean,
    @SerializedName("last_seen_at") val lastSeenAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class SyncCompleteRequest(
    @SerializedName("device_id") val deviceId: Long,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("uploaded_count") val uploadedCount: Int,
    @SerializedName("pending_count") val pendingCount: Int = 0,
    @SerializedName("failed_count") val failedCount: Int = 0,
    @SerializedName("note_ids") val noteIds: List<Long> = emptyList()
)

data class SyncCompleteResponse(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("notification_id") val notificationId: Long,
    @SerializedName("device_id") val deviceId: Long,
    @SerializedName("uploaded_count") val uploadedCount: Int,
    @SerializedName("notification_status") val notificationStatus: String,
    @SerializedName("fcm_message_id") val fcmMessageId: String?,
    @SerializedName("duplicate") val duplicate: Boolean
)
