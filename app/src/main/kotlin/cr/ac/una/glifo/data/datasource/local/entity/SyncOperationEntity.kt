package cr.ac.una.glifo.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Outbox de sincronización (issue #26, D-10). Es la cola autoritativa: vive en el teléfono y
 * el servidor solo recibe el resultado. Room: `SyncOperation`; backend: `sync_queue`
 * (Glifo_UML_Modeling.md §6.4). Las operaciones se procesan en orden de [id], así un
 * CREATE_NOTE siempre sale antes que los UPLOAD_PAGE de ese apunte (D-23).
 */
@Entity(
    tableName = "sync_operations",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["status"]),
        Index(value = ["note_id"])
    ]
)
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** CREATE_NOTE · UPLOAD_PAGE */
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    /** UUID generado antes del primer intento y reenviado en cada reintento. */
    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    /** Solo en UPLOAD_PAGE. */
    @ColumnInfo(name = "page_id")
    val pageId: Long? = null,
    /** PENDING · IN_PROGRESS · FAILED · DONE (SyncStatus, §5.7). */
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    companion object {
        const val TYPE_CREATE_NOTE = "CREATE_NOTE"
        const val TYPE_UPLOAD_PAGE = "UPLOAD_PAGE"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_DONE = "DONE"
    }
}

/** Una fila de la pantalla de cola: la operación con el título del apunte. */
data class SyncOperationRow(
    val id: Long,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    @ColumnInfo(name = "note_title")
    val noteTitle: String,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int?,
    val status: String,
    val attempts: Int,
    @ColumnInfo(name = "last_error")
    val lastError: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
