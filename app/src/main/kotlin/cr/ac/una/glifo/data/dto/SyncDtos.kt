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
