package cr.ac.una.glifo.core.sync

import com.google.gson.Gson
import java.util.UUID

sealed class SyncOperation {
    abstract val idempotencyKey: String

    data class CreateNote(
        val courseId: Long,
        val syllabusTopicId: Long?,
        val classDate: String,
        val title: String,
        override val idempotencyKey: String = UUID.randomUUID().toString()
    ) : SyncOperation()

    data class UploadPage(
        val noteIdempotencyKey: String,
        val pageIndex: Int,
        val perceptualHash: String,
        val storageUri: String,
        override val idempotencyKey: String = UUID.randomUUID().toString()
    ) : SyncOperation()

    companion object {
        fun toJson(operation: SyncOperation, gson: Gson): String {
            return when (operation) {
                is CreateNote -> gson.toJson(operation)
                is UploadPage -> gson.toJson(operation)
            }
        }
        
        fun entityType(operation: SyncOperation): String {
            return when (operation) {
                is CreateNote -> "CREATE_NOTE"
                is UploadPage -> "UPLOAD_PAGE"
            }
        }
    }
}
