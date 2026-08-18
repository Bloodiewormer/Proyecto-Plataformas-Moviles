package cr.ac.una.glifo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "note_pages")
data class NotePageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "note_id")
    val noteId: Long,
    @ColumnInfo(name = "page_index")
    val pageIndex: Int,
    @ColumnInfo(name = "perceptual_hash")
    val perceptualHash: String,
    @ColumnInfo(name = "storage_uri")
    val storageUri: String,
    @ColumnInfo(name = "level_reached")
    val levelReached: String,
    @ColumnInfo(name = "overall_confidence")
    val overallConfidence: Float,
    @ColumnInfo(name = "quality_metrics")
    val qualityMetrics: String?,
    val regions: String?,
    @ColumnInfo(name = "processed_at")
    val processedAt: String?
)
